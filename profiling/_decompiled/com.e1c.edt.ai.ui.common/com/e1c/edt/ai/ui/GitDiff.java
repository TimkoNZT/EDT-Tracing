package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import java.util.List;
import org.eclipse.jgit.lib.Repository;

public class GitDiff {
   private final Repository repository;
   private final List<String> paths;

   public GitDiff(Repository repository, List<String> paths) {
      Preconditions.checkNotNull(repository);
      Preconditions.checkNotNull(paths);
      this.repository = repository;
      this.paths = paths;
   }

   public Repository getRepository() {
      return this.repository;
   }

   public List<String> getPaths() {
      return this.paths;
   }
}
