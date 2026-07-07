package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.ui.util.LabelUtil;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.IProjectTools;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import java.io.File;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.IDocument;

public class Files implements IFiles {
   private final Cache<String, String> displayedFileNameCache = CacheBuilder.newBuilder().maximumSize(256L).build();
   private final IResourceLookup resourceLookup;
   private final IProjectTools projectTools;
   private final IBmPovider bmPovider;
   private final IQualifiedNameFilePathConverter fqn2PathConverter;

   @Inject
   public Files(IResourceLookup resourceLookup, IProjectTools projectTools, IBmPovider bmPovider, IQualifiedNameFilePathConverter fqn2PathConverter) {
      Preconditions.checkNotNull(resourceLookup);
      Preconditions.checkNotNull(projectTools);
      Preconditions.checkNotNull(bmPovider);
      Preconditions.checkNotNull(fqn2PathConverter);
      this.resourceLookup = resourceLookup;
      this.projectTools = projectTools;
      this.bmPovider = bmPovider;
      this.fqn2PathConverter = fqn2PathConverter;
   }

   public Optional<IFile> getCodeFile(EObject eObject) {
      if (!(eObject instanceof CommonModule)) {
         return Optional.empty();
      } else {
         Module module = ((CommonModule)eObject).getModule();
         if (module != null) {
            IFile file = this.resourceLookup.getPlatformResource(module);
            if (file != null && !file.isHidden() && !file.isVirtual() && file.exists()) {
               return Optional.of(file);
            }
         }

         return Optional.empty();
      }
   }

   public String getDisplayedFileName(File file) {
      if (file == null) {
         return "";
      } else {
         String path = file.getAbsolutePath();
         String fromCache = (String)this.displayedFileNameCache.getIfPresent(path);
         if (fromCache != null) {
            return fromCache;
         } else {
            String projectName = this.projectTools.determineProjectName(path);
            if (projectName != null && !projectName.isBlank()) {
               if (path != projectName) {
                  IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                  IProject project = root.getProject(projectName);
                  if (project != null) {
                     Optional<IFile> optionalProjectFile = this.projectTools.getProjectFile(project, path);
                     if (optionalProjectFile.isPresent()) {
                        IFile projectFile = (IFile)optionalProjectFile.get();

                        try {
                           Optional<BmRoot> otionalRoot = this.bmPovider.getRoot((IDocument)null, project.getName() + "/" + projectFile.getProjectRelativePath().toPortableString(), CancellationTokens.NONE);
                           if (otionalRoot.isPresent()) {
                              BmRoot objRoot = (BmRoot)otionalRoot.get();
                              String label = LabelUtil.getPath(objRoot.getBmObject(), "→", IProject.class, 1, (obj) -> !(obj instanceof IProject));
                              if (label != null && !label.isBlank()) {
                                 return label;
                              }
                           }
                        } catch (Throwable var12) {
                        }

                        Optional<String> fqnName = this.getFileName(projectFile).map((name) -> name.replace('.', '→'));
                        if (fqnName.isPresent()) {
                           return (String)fqnName.get();
                        }
                     }
                  }
               }

               return this.getFileName(file).replace('/', '→');
            } else {
               return this.getFileName(file).replace('/', '→');
            }
         }
      }
   }

   private Optional<String> getFileName(IFile file) {
      return Optional.ofNullable(this.fqn2PathConverter.getFqn(file)).map((fqn) -> fqn.toString());
   }

   private String getFileName(File file) {
      String path = file.getAbsolutePath();
      String fileName = file.getName();
      int srcIndex = path.indexOf("src");
      if (srcIndex > 0) {
         String afterSrc = path.substring(srcIndex + 3);

         String relativePath;
         for(relativePath = afterSrc.replace('\\', '/'); relativePath.startsWith("/"); relativePath = relativePath.substring(1)) {
         }

         if (!relativePath.isEmpty() && relativePath.endsWith(fileName)) {
            return relativePath;
         }

         if (!relativePath.isEmpty()) {
            return relativePath + "/" + fileName;
         }
      }

      return fileName;
   }
}
