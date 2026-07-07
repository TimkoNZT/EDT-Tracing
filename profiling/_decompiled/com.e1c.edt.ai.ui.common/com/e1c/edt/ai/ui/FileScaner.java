package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

class FileScaner implements IFileScaner {
   private static final HashSet<String> extensions = new HashSet();
   private final ILog log;

   static {
      extensions.add("bsl");
      extensions.add("mdo");
      extensions.add("form");
   }

   @Inject
   public FileScaner(ILog log) {
      Preconditions.checkNotNull(log);
      this.log = log;
   }

   public List<IFile> scan(IProject project) {
      ArrayList<IFile> files = new ArrayList();

      try {
         project.accept((resource) -> {
            if (resource instanceof IFile) {
               IFile file = (IFile)resource;
               String[] pathSegments = file.getProjectRelativePath().segments();
               if (pathSegments.length <= 2) {
                  return false;
               } else {
                  String ext = file.getFileExtension();
                  if (ext == null) {
                     return false;
                  } else {
                     if (extensions.contains(ext.toLowerCase())) {
                        files.add(file);
                     }

                     return false;
                  }
               }
            } else if (resource instanceof IFolder) {
               IFolder folder = (IFolder)resource;
               String[] pathSegments = folder.getProjectRelativePath().segments();
               if (pathSegments.length == 1) {
                  return "src".equalsIgnoreCase(pathSegments[0]);
               } else if (pathSegments.length == 2) {
                  return !"Configuration".equalsIgnoreCase(pathSegments[1]);
               } else {
                  return pathSegments.length > 2;
               }
            } else {
               return resource instanceof IProject;
            }
         });
      } catch (CoreException error) {
         this.log.logError(error);
      }

      return files;
   }
}
