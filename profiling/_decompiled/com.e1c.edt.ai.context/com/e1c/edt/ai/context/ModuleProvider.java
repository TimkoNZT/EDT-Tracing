package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmExternalUriResolver;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.bm.xtext.XtextBmLinkProvider;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

public class ModuleProvider implements IModuleProvider, IProjectIdProvider, IProjectProvider {
   private final IBmModelManager modelManager;
   private final ISettings settings;
   private final IResourceLookup resourceLookup;

   @Inject
   public ModuleProvider(IBmModelManager modelManager, ISettings settings, IResourceLookup resourceLookup) {
      Preconditions.checkNotNull(modelManager);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(resourceLookup);
      this.modelManager = modelManager;
      this.settings = settings;
      this.resourceLookup = resourceLookup;
   }

   public ProjectId getProjectId(IProject project) {
      return new ProjectId(project);
   }

   public Optional<ProjectId> getProjectId(String filePath, ICancellationToken cancellationToken) {
      return filePath.isBlank() ? Optional.empty() : this.getProject(filePath).map((project) -> this.getProjectId(project));
   }

   public synchronized Optional<ModuleInfo> getModule(IDocument document, String filePath, ICancellationToken cancellationToken) {
      Preconditions.checkNotNull(filePath);
      Preconditions.checkNotNull(cancellationToken);
      return filePath.isBlank() ? Optional.empty() : this.getProject(filePath).flatMap((project) -> this.getModuleInfo(project, filePath, cancellationToken));
   }

   public Optional<ModuleInfo> getModuleInfo(IDocument document, ICancellationToken cancellationToken) {
      if (document instanceof BslXtextDocument) {
         IUnitOfWork<XtextResource, XtextResource> work = (res) -> res;
         XtextResource bslXtextDocument = (XtextResource)((BslXtextDocument)document).readOnlyDataModel(work);

         for(EObject content : bslXtextDocument.getContents()) {
            if (cancellationToken.isCanceled()) {
               break;
            }

            if (content instanceof Module) {
               String path = null;
               IFile moduleFile = this.resourceLookup.getPlatformResource(content);
               if (moduleFile != null) {
                  path = moduleFile.getFullPath().makeRelative().toPortableString();
               }

               return Optional.of(new ModuleInfo(this.analyzeModule((Module)content, cancellationToken), path));
            }
         }
      }

      return Optional.empty();
   }

   private Optional<ModuleInfo> getModuleInfo(IProject project, String filePath, ICancellationToken cancellationToken) {
      IBmModel bmModel = this.modelManager.getModel(project);
      if (bmModel == null) {
         return Optional.empty();
      } else {
         for(IBmExternalUriResolver provider : bmModel.getEngine().getExternalUriResolvers()) {
            if (cancellationToken.isCanceled()) {
               break;
            }

            if (provider instanceof XtextBmLinkProvider) {
               URI moduleUri = this.getModuleURI(filePath);
               EObject currentModule = ((XtextBmLinkProvider)provider).getObject(moduleUri);
               if (currentModule != null && currentModule instanceof Module) {
                  return Optional.of(new ModuleInfo(this.analyzeModule((Module)currentModule, cancellationToken), filePath));
               }
            }
         }

         return Optional.empty();
      }
   }

   private Module analyzeModule(Module module, final ICancellationToken cancellationToken) {
      if (!this.settings.isExperimental()) {
         return module;
      } else {
         Resource moduleResource = module.eResource();
         if (moduleResource instanceof BslResource) {
            ((BslResource)moduleResource).setDeepAnalysis(true);
            EcoreUtil2.resolveLazyCrossReferences(moduleResource, new CancelIndicator() {
               public boolean isCanceled() {
                  return cancellationToken.isCanceled();
               }
            });
         }

         return module;
      }
   }

   public Optional<IProject> getProject(String filePath) {
      Preconditions.checkNotNull(filePath);
      if (filePath.isBlank()) {
         return Optional.empty();
      } else {
         URI moduleUri = this.getModuleURI(filePath);
         return Optional.ofNullable(this.resourceLookup.getProject(moduleUri));
      }
   }

   private URI getModuleURI(String filePath) {
      Preconditions.checkNotNull(filePath);
      return URI.createPlatformResourceURI(filePath, true).appendFragment("/0");
   }
}
