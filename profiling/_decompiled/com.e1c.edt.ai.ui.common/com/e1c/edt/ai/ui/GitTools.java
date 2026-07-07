package com.e1c.edt.ai.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

public class GitTools implements IGitTools {
   public void getDiff(Repository repository, int contextLines, OutputStream gitDiffStream) throws GitAPIException, IOException {
      Throwable var4 = null;
      Object var5 = null;

      try {
         Git git = new Git(repository);

         try {
            DiffFormatter diffFormatter = new DiffFormatter(gitDiffStream);

            try {
               diffFormatter.setRepository(repository);
               diffFormatter.setContext(contextLines);
               ObjectId headCommitId = repository.resolve("HEAD");
               List<DiffEntry> diffs;
               if (headCommitId == null) {
                  Throwable var10 = null;
                  Object var11 = null;

                  try {
                     ObjectReader reader = repository.newObjectReader();

                     try {
                        EmptyTreeIterator headTreeIter = new EmptyTreeIterator();
                        DirCacheIterator indexTreeIter = new DirCacheIterator(repository.readDirCache());
                        diffs = diffFormatter.scan(headTreeIter, indexTreeIter);
                     } finally {
                        if (reader != null) {
                           reader.close();
                        }

                     }
                  } catch (Throwable var41) {
                     if (var10 == null) {
                        var10 = var41;
                     } else if (var10 != var41) {
                        var10.addSuppressed(var41);
                     }

                     throw var10;
                  }
               } else {
                  diffs = git.diff().setCached(true).setShowNameAndStatusOnly(false).call();
               }

               for(DiffEntry diff : diffs) {
                  diffFormatter.format(diff);
               }
            } finally {
               if (diffFormatter != null) {
                  diffFormatter.close();
               }

            }
         } catch (Throwable var43) {
            if (var4 == null) {
               var4 = var43;
            } else if (var4 != var43) {
               var4.addSuppressed(var43);
            }

            if (git != null) {
               git.close();
            }

            throw var4;
         }

         if (git != null) {
            git.close();
         }

      } catch (Throwable var44) {
         if (var4 == null) {
            var4 = var44;
         } else if (var4 != var44) {
            var4.addSuppressed(var44);
         }

         throw var4;
      }
   }

   public List<GitCommitInfo> getCommitHistory(Repository repository, int maxCommits) throws GitAPIException, IOException {
      ArrayList<GitCommitInfo> commits = new ArrayList();
      Throwable var4 = null;
      Object var5 = null;

      try {
         Git git = new Git(repository);

         label372: {
            RevWalk var10000;
            try {
               RevWalk walk = new RevWalk(repository);

               try {
                  Ref headRef = repository.findRef("HEAD");
                  if (headRef != null && headRef.getObjectId() != null) {
                     ObjectId headId = headRef.getObjectId();
                     RevCommit headCommit = walk.parseCommit(headId);
                     walk.markStart(headCommit);
                     int count = 0;
                     Iterator var13 = walk.iterator();

                     while(true) {
                        if (!var13.hasNext()) {
                           break label372;
                        }

                        RevCommit commit = (RevCommit)var13.next();
                        if (count >= maxCommits) {
                           break label372;
                        }

                        List<String> changedFiles = this.getChangedFiles(repository, commit);
                        GitCommitInfo commitInfo = new GitCommitInfo(commit.getName(), commit.abbreviate(8).name(), commit.getAuthorIdent().getName(), commit.getAuthorIdent().getEmailAddress(), (long)commit.getCommitTime() * 1000L, commit.getFullMessage().trim(), changedFiles);
                        commits.add(commitInfo);
                        ++count;
                     }
                  }
               } finally {
                  var10000 = walk;
                  if (walk != null) {
                     var10000 = walk;
                     walk.close();
                  }

               }
            } catch (Throwable var26) {
               if (var4 == null) {
                  var4 = var26;
               } else if (var4 != var26) {
                  var4.addSuppressed(var26);
               }

               if (git != null) {
                  git.close();
               }

               throw var4;
            }

            if (git != null) {
               git.close();
            }

            return var10000;
         }

         if (git != null) {
            git.close();
         }

         return commits;
      } catch (Throwable var27) {
         if (var4 == null) {
            var4 = var27;
         } else if (var4 != var27) {
            var4.addSuppressed(var27);
         }

         throw var4;
      }
   }

   private List<String> getChangedFiles(Repository repository, RevCommit commit) throws IOException {
      ArrayList<String> changedFiles = new ArrayList();
      Throwable var4 = null;
      Object var5 = null;

      try {
         Git git = new Git(repository);

         try {
            RevCommit[] parents = commit.getParents();
            if (parents.length == 0) {
               Throwable var8 = null;
               Object var9 = null;

               try {
                  TreeWalk walk = new TreeWalk(repository);

                  try {
                     walk.addTree(commit.getTree());
                     walk.setRecursive(true);

                     while(walk.next()) {
                        changedFiles.add(walk.getPathString());
                     }
                  } finally {
                     if (walk != null) {
                        walk.close();
                     }

                  }
               } catch (Throwable var54) {
                  if (var8 == null) {
                     var8 = var54;
                  } else if (var8 != var54) {
                     var8.addSuppressed(var54);
                  }

                  throw var8;
               }
            } else {
               RevCommit parent = parents[0];
               Throwable var58 = null;
               Object var59 = null;

               try {
                  DiffFormatter diffFormatter = new DiffFormatter((OutputStream)null);

                  try {
                     diffFormatter.setRepository(repository);

                     for(DiffEntry diff : diffFormatter.scan(parent.getTree(), commit.getTree())) {
                        changedFiles.add(diff.getNewPath());
                     }
                  } finally {
                     if (diffFormatter != null) {
                        diffFormatter.close();
                     }

                  }
               } catch (Throwable var52) {
                  if (var58 == null) {
                     var58 = var52;
                  } else if (var58 != var52) {
                     var58.addSuppressed(var52);
                  }

                  throw var58;
               }
            }
         } finally {
            if (git != null) {
               git.close();
            }

         }

         return changedFiles;
      } catch (Throwable var56) {
         if (var4 == null) {
            var4 = var56;
         } else if (var4 != var56) {
            var4.addSuppressed(var56);
         }

         throw var4;
      }
   }

   public String getDiffText(Repository repository, int contextLines) throws GitAPIException, IOException {
      Throwable var3 = null;
      Object var4 = null;

      try {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

         String var10000;
         try {
            this.getDiff(repository, contextLines, outputStream);
            var10000 = outputStream.toString(StandardCharsets.UTF_8.name());
         } finally {
            if (outputStream != null) {
               outputStream.close();
            }

         }

         return var10000;
      } catch (Throwable var11) {
         if (var3 == null) {
            var3 = var11;
         } else if (var3 != var11) {
            var3.addSuppressed(var11);
         }

         throw var3;
      }
   }

   public String getUncommittedDiffText(Repository repository, int contextLines) throws GitAPIException, IOException {
      Throwable var3 = null;
      Object var4 = null;

      try {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

         String var29;
         label387: {
            try {
               DiffFormatter diffFormatter = new DiffFormatter(outputStream);

               try {
                  diffFormatter.setRepository(repository);
                  diffFormatter.setContext(contextLines);
                  ObjectId headCommitId = repository.resolve("HEAD");
                  FileTreeIterator workingTreeIter = new FileTreeIterator(repository);
                  if (headCommitId == null) {
                     for(DiffEntry diff : diffFormatter.scan(new EmptyTreeIterator(), workingTreeIter)) {
                        diffFormatter.format(diff);
                     }

                     var29 = outputStream.toString(StandardCharsets.UTF_8.name());
                     break label387;
                  }

                  RevCommit headCommit = repository.parseCommit(headCommitId);
                  AbstractTreeIterator headTreeIter = this.prepareTreeParser(repository, headCommit);

                  for(DiffEntry diff : diffFormatter.scan(headTreeIter, workingTreeIter)) {
                     diffFormatter.format(diff);
                  }

                  var29 = outputStream.toString(StandardCharsets.UTF_8.name());
               } finally {
                  if (diffFormatter != null) {
                     diffFormatter.close();
                  }

               }
            } catch (Throwable var24) {
               if (var3 == null) {
                  var3 = var24;
               } else if (var3 != var24) {
                  var3.addSuppressed(var24);
               }

               if (outputStream != null) {
                  outputStream.close();
               }

               throw var3;
            }

            if (outputStream != null) {
               outputStream.close();
            }

            return var29;
         }

         if (outputStream != null) {
            outputStream.close();
         }

         return var29;
      } catch (Throwable var25) {
         if (var3 == null) {
            var3 = var25;
         } else if (var3 != var25) {
            var3.addSuppressed(var25);
         }

         throw var3;
      }
   }

   public String getDiffText(Repository repository, String oldCommit, String newCommit, int contextLines) throws GitAPIException, IOException {
      Throwable var5 = null;
      Object var6 = null;

      try {
         Git git = new Git(repository);

         String var10000;
         try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
               DiffFormatter diffFormatter = new DiffFormatter(outputStream);

               try {
                  diffFormatter.setRepository(repository);
                  diffFormatter.setContext(contextLines);
                  RevCommit oldRev = repository.parseCommit(repository.resolve(oldCommit));
                  RevCommit newRev = repository.parseCommit(repository.resolve(newCommit));

                  for(DiffEntry diff : git.diff().setOldTree(this.prepareTreeParser(repository, oldRev)).setNewTree(this.prepareTreeParser(repository, newRev)).call()) {
                     diffFormatter.format(diff);
                  }

                  var10000 = outputStream.toString(StandardCharsets.UTF_8.name());
               } finally {
                  if (diffFormatter != null) {
                     diffFormatter.close();
                  }

               }
            } catch (Throwable var32) {
               if (var5 == null) {
                  var5 = var32;
               } else if (var5 != var32) {
                  var5.addSuppressed(var32);
               }

               if (outputStream != null) {
                  outputStream.close();
               }

               throw var5;
            }

            if (outputStream != null) {
               outputStream.close();
            }
         } catch (Throwable var33) {
            if (var5 == null) {
               var5 = var33;
            } else if (var5 != var33) {
               var5.addSuppressed(var33);
            }

            if (git != null) {
               git.close();
            }

            throw var5;
         }

         if (git != null) {
            git.close();
         }

         return var10000;
      } catch (Throwable var34) {
         if (var5 == null) {
            var5 = var34;
         } else if (var5 != var34) {
            var5.addSuppressed(var34);
         }

         throw var5;
      }
   }

   private AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
      Throwable var3 = null;
      Object var4 = null;

      try {
         ObjectReader reader = repository.newObjectReader();

         CanonicalTreeParser var10000;
         try {
            RevTree tree = commit.getTree();
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(reader, tree);
            var10000 = treeParser;
         } finally {
            if (reader != null) {
               reader.close();
            }

         }

         return var10000;
      } catch (Throwable var13) {
         if (var3 == null) {
            var3 = var13;
         } else if (var3 != var13) {
            var3.addSuppressed(var13);
         }

         throw var3;
      }
   }
}
