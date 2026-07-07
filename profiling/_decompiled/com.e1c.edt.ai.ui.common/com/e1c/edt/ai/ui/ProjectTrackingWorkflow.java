package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IHashTools;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;

class ProjectTrackingWorkflow implements IProjectTrackingWorkflow {
   private static final Duration ExtraLongDelay = Duration.ofSeconds(30L);
   private static final Duration LongDelay = Duration.ofSeconds(3L);
   private static final Duration ShortDelay = Duration.ofMillis(10L);
   private final ILog log;
   private final Provider<IStatistics> statisticsProvider;
   private final IHashTools hashTools;
   private final IClock clock;
   private final IProjectIdProvider projectIdProvider;
   private final IGlobalContextSync globalContextSync;
   private final ISettings settings;
   private final IFileScaner fileScaner;
   private final ISessionService sessionService;
   private final HashSet<ProjectFile> filesToSync = new HashSet();
   private final ConcurrentHashMap<String, ProjectFile> filesToHash = new ConcurrentHashMap();
   private IProject project;
   private ProjectId projectId;
   private String sessionId;
   private ProjectTrackingWorkflowState nextState;
   private int iterationCount;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$ui$ProjectTrackingWorkflowState;

   @Inject
   public ProjectTrackingWorkflow(ILog log, Provider<IStatistics> statisticsProvider, IHashTools hashTools, IClock clock, IProjectIdProvider projectIdProvider, IGlobalContextSync globalContextSync, ISettings settings, IFileScaner fileScaner, ISessionService sessionService) {
      this.nextState = ProjectTrackingWorkflowState.INIT;
      this.iterationCount = Integer.MAX_VALUE;
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(statisticsProvider);
      Preconditions.checkNotNull(hashTools);
      Preconditions.checkNotNull(clock);
      Preconditions.checkNotNull(projectIdProvider);
      Preconditions.checkNotNull(globalContextSync);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(fileScaner);
      Preconditions.checkNotNull(sessionService);
      this.log = log;
      this.statisticsProvider = statisticsProvider;
      this.hashTools = hashTools;
      this.clock = clock;
      this.projectIdProvider = projectIdProvider;
      this.globalContextSync = globalContextSync;
      this.settings = settings;
      this.fileScaner = fileScaner;
      this.sessionService = sessionService;
   }

   public ProjectTrackingWorkflow initialize(IProject project) {
      Preconditions.checkNotNull(project);
      this.project = project;
      this.projectId = this.projectIdProvider.getProjectId(project);
      this.iterationCount = Integer.MAX_VALUE;
      return this;
   }

   public IProject getProject() {
      return this.project;
   }

   public Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken) {
      if (this.settings.isEnabled() && this.project.exists()) {
         if (this.checkSessionChanged()) {
            this.reset();
         }

         Result result = null;

         try {
            switch (this.nextState) {
               case INIT:
                  result = this.init(progressMonitor, cancellationToken);
                  break;
               case SCAN:
                  result = this.scan(progressMonitor, cancellationToken);
                  break;
               case HASH:
                  result = this.hash(1000, progressMonitor, cancellationToken);
                  break;
               case SYNC:
                  result = this.sync(1000, progressMonitor, cancellationToken);
            }
         } catch (Exception error) {
            this.log.trace("api_calls", "Sync error", () -> error.toString());
            return LongDelay;
         }

         if (result != null) {
            this.nextState = result.nextState;
            return result.delay;
         } else {
            return LongDelay;
         }
      } else {
         return ExtraLongDelay;
      }
   }

   private boolean checkSessionChanged() {
      try {
         Optional<Session> session = (Optional)this.sessionService.getSessionAsync(this.projectId).get();
         String curSessionId = (String)session.map((i) -> i.sessionId).orElse("");
         if (!curSessionId.equals(this.sessionId)) {
            this.sessionId = curSessionId;
            return true;
         }
      } catch (ExecutionException | InterruptedException error) {
         this.log.logError(error);
      }

      return false;
   }

   private void reset() {
      this.filesToSync.clear();

      for(ProjectFile file : this.filesToHash.values()) {
         file.update(this.clock.now(), (String)null, 1L);
      }

   }

   public void track(AIContext aiCtx) {
      this.log.trace("sync", "Track", () -> aiCtx.toString());
      String path = aiCtx.getPath();
      IPath projectPath = this.project.getProjectRelativePath().append(path);
      if (projectPath.segmentCount() > 1) {
         IFile fileOnDisk = this.project.getFile(projectPath.removeFirstSegments(1));
         if (fileOnDisk != null) {
            ProjectFile fileToTrack = new ProjectFile(aiCtx, path, fileOnDisk, LocalDateTime.MIN);
            fileToTrack.update(this.clock.now(), (String)null, 1L);
            this.filesToHash.put(path, fileToTrack);
         }
      }
   }

   private Result init(IProgressMonitor progressMonitor, ICancellationToken cancellationToken) {
      if (this.iterationCount < 5) {
         ++this.iterationCount;
         return new Result(ProjectTrackingWorkflowState.HASH, LongDelay);
      } else {
         this.iterationCount = 0;
         return new Result(ProjectTrackingWorkflowState.SCAN, ShortDelay);
      }
   }

   private Result scan(IProgressMonitor progressMonitor, ICancellationToken cancellationToken) throws CoreException {
      progressMonitor.subTask(Messages.CodeCompletionBackgroundScanSubtaskName);
      if (!this.settings.sendGlobalContext(this.projectId)) {
         return new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
      } else {
         List<IFile> files = this.fileScaner.scan(this.project);
         LocalDateTime now = this.clock.now();

         for(IFile file : files) {
            if (file.getLocation().toFile().exists()) {
               String path = file.getFullPath().makeRelative().toPortableString();
               this.filesToHash.computeIfAbsent(path, (key) -> new ProjectFile(new AIContext(this.projectId, key, (IDocument)null), key, file, now));
               if (cancellationToken.isCanceled()) {
                  return new Result(ProjectTrackingWorkflowState.SCAN, ShortDelay);
               }
            }
         }

         int newFilesToHashCount = 0;

         for(ProjectFile file : this.filesToHash.values()) {
            if (file.getModificationStamp() <= 0L && file.getHash() == null) {
               ++newFilesToHashCount;
            }
         }

         this.log.trace("sync", "Scaned", () -> {
            StringBuilder message = new StringBuilder();
            message.append("Project: ");
            message.append(this.project.getName());
            message.append(System.lineSeparator());
            message.append("Files: ");
            message.append(files.size());
            message.append(System.lineSeparator());
            message.append("New files to hash: ");
            message.append(newFilesToHashCount);
            return message.toString();
         });
         if (newFilesToHashCount > 0) {
            return new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
         } else {
            return new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
         }
      }
   }

   private Result hash(int maxFiles, IProgressMonitor progressMonitor, ICancellationToken cancellationToken) {
      int filesToSyncSize = this.filesToSync.size();
      maxFiles -= filesToSyncSize;
      if (maxFiles <= 0) {
         return new Result(ProjectTrackingWorkflowState.INIT, Duration.ofMillis(100L));
      } else {
         int fileToSyncCount = 0;
         Duration delay = LongDelay;
         LocalDateTime now = this.clock.now();
         List<ProjectFile> hashingFiles = (List)this.filesToHash.values().stream().filter((i) -> !cancellationToken.isCanceled()).filter((filex) -> filex.getAge(now).compareTo(delay) >= 0).sorted(ProjectFile.COMPARATOR).limit((long)maxFiles).collect(Collectors.toList());
         int hashed = 0;
         progressMonitor.beginTask(Messages.CodeCompletionBackgroundHashSubtaskName, hashingFiles.size());

         for(ProjectFile file : hashingFiles) {
            if (cancellationToken.isCanceled()) {
               break;
            }

            try {
               IFile fileOnDisk = file.file;
               if (fileOnDisk == null || file.aiCtx.getDocument() != null || fileOnDisk.exists() && fileOnDisk.isAccessible() || this.filesToHash.remove(file.path) != null) {
                  long prevModificationStamp = file.getModificationStamp();
                  long newModificationStamp = -1L;
                  String prevHash = file.getHash();
                  IDocument document = file.aiCtx.getDocument();
                  boolean isAccessible = true;
                  if (!file.aiCtx.isDisposed() && document != null && document instanceof IDocumentExtension4) {
                     IDocumentExtension4 docExtension = (IDocumentExtension4)document;
                     newModificationStamp = docExtension.getModificationStamp();
                     if (file.getModificationStamp() == newModificationStamp) {
                        file.update(now, prevHash, newModificationStamp);
                        continue;
                     }
                  } else {
                     document = null;
                     isAccessible = file.file.isAccessible();
                     newModificationStamp = file.file.getModificationStamp();
                     if (isAccessible && file.getModificationStamp() == newModificationStamp) {
                        file.update(now, prevHash, newModificationStamp);
                        continue;
                     }
                  }

                  String newHash = isAccessible ? (String)this.hashTools.hashOf(document, file.file).map((hash) -> this.hashTools.format(hash, true)).orElse((Object)null) : null;
                  file.update(now, newHash, newModificationStamp);
                  ++hashed;
                  if (newHash == null || !newHash.equals(prevHash)) {
                     ++fileToSyncCount;
                     if (this.filesToSync.add(file)) {
                        this.log.trace("sync", "Sync required", () -> {
                           StringBuilder message = new StringBuilder();
                           message.append("Project: ");
                           message.append(this.project.getName());
                           message.append(System.lineSeparator());
                           message.append("File: ");
                           message.append(file.path);
                           message.append(System.lineSeparator());
                           message.append("Is accessible: ");
                           message.append(isAccessible);
                           message.append(System.lineSeparator());
                           message.append("Prev timestamp: ");
                           message.append(prevModificationStamp);
                           if (prevHash != null) {
                              message.append(System.lineSeparator());
                              message.append("Prev hash: ");
                              message.append(prevHash);
                           }

                           message.append(System.lineSeparator());
                           message.append("New timestamp: ");
                           message.append(newModificationStamp);
                           message.append(System.lineSeparator());
                           message.append("New hash: ");
                           message.append(newHash == null ? "empty" : newHash);
                           return message.toString();
                        });
                     }
                  }
               }
            } catch (Exception error) {
               this.log.logError(error);
            } finally {
               progressMonitor.worked(1);
            }
         }

         if (hashed > 0) {
            this.log.trace("sync", "Hashed", () -> {
               StringBuilder message = new StringBuilder();
               message.append("Project: ");
               message.append(this.project.getName());
               message.append(System.lineSeparator());
               message.append("Hashed: ");
               message.append(hashed);
               message.append(" files");
               return message.toString();
            });
         }

         return fileToSyncCount > 0 ? new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay) : new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
      }
   }

   private Result sync(int maxFiles, IProgressMonitor progressMonitor, ICancellationToken cancellationToken) {
      if (maxFiles <= 0) {
         return new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
      } else {
         List<ProjectFile> filesToProcess = (List)this.filesToSync.stream().filter((filex) -> !cancellationToken.isCanceled()).sorted(ProjectFile.COMPARATOR).limit((long)maxFiles).collect(Collectors.toList());
         ArrayList<CompletableFuture<Boolean>> features = new ArrayList();
         ArrayList<GlobalContextUpdate> filesUpdates = new ArrayList();

         for(ProjectFile file : filesToProcess) {
            if (cancellationToken.isCanceled()) {
               return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
            }

            this.filesToSync.remove(file);
            GlobalContextUpdate update = new GlobalContextUpdate();
            String ext = file.file.getFileExtension();
            if (ext != null) {
               String var12;
               switch ((var12 = file.file.getFileExtension()).hashCode()) {
                  case 97851:
                     if (!var12.equals("bsl")) {
                        continue;
                     }

                     update.field = "local_functions";
                     break;
                  case 107960:
                     if (!var12.equals("mdo")) {
                        continue;
                     }

                     update.field = "meta";
                     break;
                  case 3148996:
                     if (var12.equals("form")) {
                        update.field = "form";
                        break;
                     }
                  default:
                     continue;
               }

               update.path = file.path;
               update.hash = file.getHash();
               if (file.aiCtx.getDocument() != null) {
                  ArrayList<GlobalContextUpdate> documentUpdates = new ArrayList();
                  documentUpdates.add(update);
                  features.add(this.globalContextSync.syncUpdates(file.aiCtx, documentUpdates, 5, (IStatistics)this.statisticsProvider.get(), cancellationToken));
               } else {
                  filesUpdates.add(update);
               }
            }
         }

         if (!filesUpdates.isEmpty()) {
            features.add(this.globalContextSync.syncUpdates(new AIContext(this.projectId, "", (IDocument)null), filesUpdates, 5, (IStatistics)this.statisticsProvider.get(), cancellationToken));
         }

         CompletableFuture.allOf((CompletableFuture[])features.toArray(new CompletableFuture[features.size()])).join();
         return !this.filesToSync.isEmpty() ? new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay) : new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$ui$ProjectTrackingWorkflowState() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$ui$ProjectTrackingWorkflowState;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[ProjectTrackingWorkflowState.values().length];

         try {
            var0[ProjectTrackingWorkflowState.HASH.ordinal()] = 3;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[ProjectTrackingWorkflowState.INIT.ordinal()] = 1;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[ProjectTrackingWorkflowState.SCAN.ordinal()] = 2;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[ProjectTrackingWorkflowState.SYNC.ordinal()] = 4;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$ui$ProjectTrackingWorkflowState = var0;
         return var0;
      }
   }

   private static class Result {
      public final ProjectTrackingWorkflowState nextState;
      public final Duration delay;

      public Result(ProjectTrackingWorkflowState nextState, Duration delay) {
         Preconditions.checkNotNull(nextState);
         Preconditions.checkNotNull(delay);
         this.nextState = nextState;
         this.delay = delay;
      }
   }
}
