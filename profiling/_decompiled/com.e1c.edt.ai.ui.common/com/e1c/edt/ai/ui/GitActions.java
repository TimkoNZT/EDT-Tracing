package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.assistent.ITools;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ToolFeedbackFinalTextRequest;
import com.e1c.edt.ai.assistent.model.ToolFeedbackResponse;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequestContent;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponseContent;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.lib.Repository;

public class GitActions implements IGitActions {
   private final ILog log;
   private final IDispatcher dispatcher;
   private final IProjectIdProvider projectIdProvider;
   private final ISettings settings;
   private final IGitTools gitTools;
   private final ITools tools;
   private final IResourceProvider resourceProvider;
   private final IChat chat;
   private Job currentJob;

   @Inject
   public GitActions(ILog log, IDispatcher dispatcher, IProjectIdProvider projectIdProvider, ISettings settings, IGitTools gitTools, ITools tools, IResourceProvider resourceProvider, IChat chat) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(projectIdProvider);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(gitTools);
      Preconditions.checkNotNull(tools);
      Preconditions.checkNotNull(resourceProvider);
      Preconditions.checkNotNull(chat);
      this.log = log;
      this.dispatcher = dispatcher;
      this.projectIdProvider = projectIdProvider;
      this.settings = settings;
      this.gitTools = gitTools;
      this.tools = tools;
      this.resourceProvider = resourceProvider;
      this.chat = chat;
   }

   public IObservable<CommitMessage> ceateGitCommitMessageSource(String baseCommitMessage, List<GitDiff> diffs, ICancellationToken cancellationToken) {
      return Observables.create((observer) -> {
         Job job = this.dispatcher.createJob(Messages.BackgroundJobName, (jobCtx) -> {
            try {
               this.getGitCommitMessage(baseCommitMessage, diffs, observer, cancellationToken);
            } catch (Exception error) {
               this.log.logError(error);
               observer.onError(error);
            }

         }, false, cancellationToken);
         this.runJob(job);
         return Closeables.Empty;
      });
   }

   public void reviewGitChanges(List<GitDiff> diffs, ICancellationToken cancellationToken) {
      Job job = this.dispatcher.createJob(Messages.BackgroundJobName, (jobCtx) -> {
         try {
            Map<ProjectId, String> diff = this.getDiff(diffs, cancellationToken);
            ProjectId firstProjectId = null;
            StringBuilder diffText = new StringBuilder();

            for(Map.Entry<ProjectId, String> diffItem : diff.entrySet()) {
               if (cancellationToken.isCanceled()) {
                  break;
               }

               if (firstProjectId == null) {
                  firstProjectId = (ProjectId)diffItem.getKey();
               } else {
                  diffText.append(System.lineSeparator());
               }

               diffText.append((String)diffItem.getValue());
            }

            if (firstProjectId != null) {
               AIContext ctx = new AIContext(firstProjectId, "", (IDocument)null);
               String diffStr = diffText.toString();
               this.dispatcher.dispatchAsync(() -> this.chat.reviewCode(ctx, diffStr));
            }
         } catch (Exception error) {
            this.log.logError(error);
         }

      }, false, cancellationToken);
      this.runJob(job);
   }

   public CompletableFuture<Optional<String>> feedbackAsync(CommitMessage commitMessage, String finalText, ICancellationToken cancellationToken) {
      ToolFeedbackFinalTextRequest request = new ToolFeedbackFinalTextRequest();
      request.uuid = commitMessage.getUuid();
      request.finalText = finalText;
      return this.tools.feedbackAsync(commitMessage.getProjectId(), request, cancellationToken).thenApplyAsync((response) -> response.map((i) -> i.uuid));
   }

   private void getGitCommitMessage(String baseCommitMessage, List<GitDiff> diffs, final IObserver<CommitMessage> observer, ICancellationToken cancellationToken) {
      Map<ProjectId, String> diff = this.getDiff(diffs, cancellationToken);
      final ProjectId firstProjectId = null;
      StringBuilder diffText = new StringBuilder();

      for(Map.Entry<ProjectId, String> diffItem : diff.entrySet()) {
         if (cancellationToken.isCanceled()) {
            break;
         }

         if (firstProjectId == null) {
            firstProjectId = (ProjectId)diffItem.getKey();
         } else {
            diffText.append(System.lineSeparator());
         }

         diffText.append((String)diffItem.getValue());
      }

      if (firstProjectId == null) {
         observer.onCompleted();
      } else {
         ToolInvokeRequest toolInvokeRequest = new ToolInvokeRequest();
         toolInvokeRequest.toolName = "raw";
         toolInvokeRequest.uiLanguage = this.settings.getLanguage();
         toolInvokeRequest.programmingLanguage = "git diff";
         ToolInvokeRequestContent content = new ToolInvokeRequestContent();
         toolInvokeRequest.content = content;
         content.instruction = ((String)this.resourceProvider.getTextResource("prompts/git_commit.txt").orElse("")).replace("${language}", this.settings.getLanguage()).replace("${base_commit_message}", (CharSequence)Optional.ofNullable(baseCommitMessage != null && !baseCommitMessage.isBlank() ? baseCommitMessage : null).orElse("no additional lines")).replace("${git_dif}", diffText.toString());
         this.log.trace("api_calls", "Prompt", () -> content.instruction);
         final StringBuilder message = new StringBuilder();
         final StringBuilder uudi = new StringBuilder();
         IObservable<ToolInvokeResponse> invokeSource = this.tools.createInvokeSource(firstProjectId, toolInvokeRequest, cancellationToken);
         invokeSource.subscribe(new IObserver<ToolInvokeResponse>() {
            public void onNext(ToolInvokeResponse value) {
               ToolInvokeResponseContent content = value.content;
               if (value.uuid != null) {
                  uudi.setLength(0);
                  uudi.append(value.uuid);
               }

               if (content != null) {
                  String text = content.text;
                  if (text != null) {
                     if (value.finished) {
                        text = text.trim();
                     } else {
                        synchronized(message) {
                           message.append(text);
                           text = message.toString().trim();
                        }
                     }

                     if (!text.isBlank()) {
                        observer.onNext(new CommitMessage(firstProjectId, uudi.toString(), text));
                     }
                  }
               }

            }

            public void onError(Throwable error) {
               uudi.setLength(0);
               observer.onError(error);
            }

            public void onCompleted() {
               uudi.setLength(0);
               observer.onCompleted();
            }
         });
      }
   }

   private Map<ProjectId, String> getDiff(List<GitDiff> diffs, ICancellationToken cancellationToken) {
      HashMap<ProjectId, String> result = new HashMap();
      Map<Repository, List<GitDiff>> groupsByRepo = this.groupChangesByRepo(diffs);

      for(Map.Entry<Repository, List<GitDiff>> groupByRepo : groupsByRepo.entrySet()) {
         if (cancellationToken.isCanceled()) {
            break;
         }

         Optional<ProjectId> optionalProjectId = this.getProjectId(cancellationToken, groupByRepo);
         if (optionalProjectId.isEmpty()) {
            this.log.warning("Git", () -> "No project id found for diffs");
         } else {
            Repository repository = (Repository)groupByRepo.getKey();

            try {
               Throwable var9 = null;
               Object var10 = null;

               try {
                  ByteArrayOutputStream gitDiffStream = new ByteArrayOutputStream();

                  try {
                     ProjectId projectId = (ProjectId)optionalProjectId.get();
                     this.gitTools.getDiff(repository, this.settings.getGitDiffContextLines(projectId), gitDiffStream);
                     String gitDiff = gitDiffStream.toString("UTF-8");
                     result.put(projectId, gitDiff);
                  } finally {
                     if (gitDiffStream != null) {
                        gitDiffStream.close();
                     }

                  }
               } catch (Throwable var21) {
                  if (var9 == null) {
                     var9 = var21;
                  } else if (var9 != var21) {
                     var9.addSuppressed(var21);
                  }

                  throw var9;
               }
            } catch (Exception error) {
               this.log.logError(error);
            }
         }
      }

      return result;
   }

   private Optional<ProjectId> getProjectId(ICancellationToken cancellationToken, Map.Entry<Repository, List<GitDiff>> groupByRepo) {
      return ((List)groupByRepo.getValue()).stream().flatMap((diff) -> diff.getPaths().stream()).map((diff) -> this.projectIdProvider.getProjectId(diff, cancellationToken)).filter((project) -> project.isPresent()).map((project) -> (ProjectId)project.get()).findFirst();
   }

   private Map<Repository, List<GitDiff>> groupChangesByRepo(List<GitDiff> diffs) {
      return (Map)diffs.stream().collect(Collectors.groupingBy((diff) -> diff.getRepository()));
   }

   private synchronized void runJob(Job job) {
      if (this.currentJob != null) {
         this.currentJob.cancel();
         this.currentJob = null;
      }

      this.currentJob = job;
      this.currentJob.setPriority(50);
      job.schedule();
   }
}
