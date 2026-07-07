package com.e1c.edt.ai.ui;

import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitUtils {
   public static Repository getRepository(IProject project) {
      if (project != null && project.exists()) {
         try {
            IPath projectLocation = project.getLocation();
            if (projectLocation == null) {
               return null;
            } else {
               File projectDir = projectLocation.toFile();
               File gitDir = findGitDirectory(projectDir);
               if (gitDir == null) {
                  return null;
               } else {
                  FileRepositoryBuilder builder = new FileRepositoryBuilder();
                  builder.setGitDir(gitDir);
                  builder.readEnvironment();
                  builder.findGitDir();
                  return builder.build();
               }
            }
         } catch (Exception var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static File findGitDirectory(File directory) {
      if (directory == null) {
         return null;
      } else {
         File gitDir = new File(directory, ".git");
         if (gitDir.exists() && gitDir.isDirectory()) {
            return gitDir;
         } else {
            File parentDir = directory.getParentFile();
            if (parentDir != null) {
               gitDir = new File(parentDir, ".git");
               if (gitDir.exists() && gitDir.isDirectory()) {
                  return gitDir;
               }
            }

            return null;
         }
      }
   }
}
