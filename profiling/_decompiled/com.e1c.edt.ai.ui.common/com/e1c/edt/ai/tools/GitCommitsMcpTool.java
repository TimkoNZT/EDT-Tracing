package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.ui.GitCommitInfo;
import com.e1c.edt.ai.ui.GitUtils;
import com.e1c.edt.ai.ui.IGitTools;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.lib.Repository;

public class GitCommitsMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "GitCommits";
   private static final int DEFAULT_MAX_COMMITS = 32;
   private static String QuestionExample = "{\n  \"project_name\": \"MyProject\",\n  \"max_commits\": 10\n}";
   private static String AnswerExample = "{\n  \"commits\": [\n    {\n      \"hash\": \"a1b2c3d4e5f6g7h8i9j0\",\n      \"short_hash\": \"a1b2c3d4\",\n      \"author_name\": \"John Doe\",\n      \"author_email\": \"john@example.com\",\n      \"commit_time\": 1642678800000,\n      \"formatted_time\": \"2022-01-20T10:30:00+03:00\",\n      \"message\": \"Fix bug in user authentication\",\n      \"changed_files\": [\"src/auth/UserService.java\", \"src/auth/UserController.java\"]\n    }\n  ],\n  \"has_more\": true\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IMarkdownUtils markdownUtils;
   private final IGitTools gitTools;

   @Inject
   public GitCommitsMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils, IGitTools gitTools) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(gitTools);
      this.json = json;
      this.messageFactory = messageFactory;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.markdownUtils = markdownUtils;
      this.gitTools = gitTools;
      this.spec = createSpecification();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = true;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample + "\n\nRequired field: 'project_name' (string)" + "\nOptional fields: 'max_commits' (integer)", ToolErrorType.RETRYABLE);
      } else {
         Request request = (Request)optionalRequest.get();
         String projectName = request.projectName;
         if (projectName != null && !projectName.isBlank()) {
            int maxCommits = request.maxCommits != null && request.maxCommits > 0 ? request.maxCommits : 32;
            if (call.callKind == ToolCallKind.RENDER) {
               StringBuilder requestMarkdown = new StringBuilder();
               requestMarkdown.append(MessageFormat.format(Messages.GitCommitsTitleTemplate, projectName)).append("\n\n").append(Messages.ProjectName).append(": ").append("`").append(this.markdownUtils.escapeForMarkdown(projectName)).append("`");
               requestMarkdown.append("\n\n").append(Messages.MaxCommits).append(": ").append("`").append(maxCommits).append("`");
               details.requestMarkdown = requestMarkdown.toString();
               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            } else {
               return CompletableFuture.supplyAsync(() -> {
                  if (cancellationToken.isCanceled()) {
                     throw new ToolException("Operation was cancelled before execution.");
                  } else {
                     IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                     IProject project = root.getProject(projectName);
                     if (project != null && project.exists()) {
                        if (!project.isOpen()) {
                           try {
                              ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
                              monitor.setCancellationToken(cancellationToken);
                              project.open(monitor);
                           } catch (CoreException error) {
                              throw new ToolException("Cannot open the project \"" + projectName + "\"", error, ToolErrorType.RETRYABLE);
                           }
                        }

                        Repository repository = GitUtils.getRepository(project);
                        if (repository == null) {
                           throw new ToolException("The project \"" + projectName + "\" is not a Git repository.");
                        } else {
                           try {
                              List<GitCommitInfo> gitCommits = this.gitTools.getCommitHistory(repository, maxCommits);
                              ArrayList<CommitInfo> commitInfos = new ArrayList();

                              for(GitCommitInfo gitCommit : gitCommits) {
                                 CommitInfo commitInfo = new CommitInfo();
                                 commitInfo.hash = gitCommit.getHash();
                                 commitInfo.shortHash = gitCommit.getShortHash();
                                 commitInfo.authorName = gitCommit.getAuthorName();
                                 commitInfo.authorEmail = gitCommit.getAuthorEmail();
                                 commitInfo.commitTime = gitCommit.getCommitTime();
                                 commitInfo.formattedTime = gitCommit.getFormattedTime();
                                 commitInfo.message = gitCommit.getMessage();
                                 commitInfo.changedFiles = gitCommit.getChangedFiles();
                                 commitInfo.changedFilesCount = gitCommit.getChangedFilesCount();
                                 commitInfos.add(commitInfo);
                              }

                              boolean hasMore = commitInfos.size() == maxCommits;
                              GitCommitsResponse response = new GitCommitsResponse();
                              response.commits = commitInfos;
                              response.hasMore = hasMore;
                              String content = this.json.serialize(response);
                              StringBuilder responseMarkdown = new StringBuilder();
                              responseMarkdown.append(MessageFormat.format(Messages.GitCommitsFoundTemplate, this.markdownUtils.createStyledText(String.valueOf(commitInfos.size()), TextColor.GREEN, FontWeight.BOLD, false), this.markdownUtils.escapeForMarkdown(projectName))).append("\n\n").append(Messages.ProjectName).append(": ").append("`").append(this.markdownUtils.escapeForMarkdown(projectName)).append("`").append("\n\n").append(Messages.MaxCommits + ": ").append("`").append(maxCommits).append("`");
                              responseMarkdown.append("\n\n<details><summary>").append(Messages.CommitsList).append("</summary>\n\n");

                              for(CommitInfo commit : commitInfos) {
                                 responseMarkdown.append("### **").append(this.markdownUtils.createStyledText(commit.shortHash, TextColor.BLUE, FontWeight.NORMAL, true)).append("** - ").append(this.markdownUtils.escapeForMarkdown(commit.message)).append("\n\n");
                                 responseMarkdown.append("**").append(Messages.Author).append(":** ").append(this.markdownUtils.escapeForMarkdown(commit.authorName)).append(" <").append(this.markdownUtils.escapeForMarkdown(commit.authorEmail)).append(">\n");
                                 responseMarkdown.append("**").append(Messages.Date).append(":** ").append(commit.formattedTime).append("\n");
                                 responseMarkdown.append("**").append(Messages.ChangedFiles).append(":** ").append(commit.changedFilesCount).append(" ").append(Messages.Files.toLowerCase()).append("\n");
                                 if (!commit.changedFiles.isEmpty()) {
                                    responseMarkdown.append("\n```\n");

                                    for(String file : commit.changedFiles) {
                                       responseMarkdown.append(this.markdownUtils.escapeForMarkdown(file)).append("\n");
                                    }

                                    responseMarkdown.append("```\n");
                                 }

                                 responseMarkdown.append("\n---\n\n");
                              }

                              responseMarkdown.append("</details>");
                              details.responseMarkdown = responseMarkdown.toString();
                              return this.messageFactory.createMessage(this, call, content, details);
                           } catch (Exception e) {
                              throw new ToolException("Failed to get commit history", e, ToolErrorType.RETRYABLE);
                           }
                        }
                     } else {
                        throw new ToolException("The project \"" + projectName + "\" does not exist.");
                     }
                  }
               }).exceptionally((throwable) -> {
                  if (throwable.getCause() instanceof ToolException) {
                     throw (ToolException)throwable.getCause();
                  } else {
                     throw new ToolException(throwable.getMessage(), throwable, ToolErrorType.RETRYABLE);
                  }
               });
            }
         } else {
            throw new ToolException("`project_name` is required.");
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "GitCommits";
      StringBuilder description = new StringBuilder();
      description.append("Lists recent Git commits for a project.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Requires the project to be a Git repository.");
      description.append("\n- Supports limiting the number of commits.");
      description.append("\n- Returns hash, author, date, message, and changed files.");
      description.append("\n- Response includes has_more flag indicating if more commits are available.");
      description.append("\n\nRelated tools:");
      description.append("\n- Inspect diffs: `GitDiff`.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty projectNameProp = new McpToolCallProperty();
      projectNameProp.type = "string";
      projectNameProp.description = "Project name in IDE. For example, \"MyProject\".";
      properties.put("project_name", projectNameProp);
      McpToolCallProperty maxCommitsProp = new McpToolCallProperty();
      maxCommitsProp.type = "integer";
      maxCommitsProp.description = "Maximum number of commits to return. Default: 32";
      properties.put("max_commits", maxCommitsProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("project_name");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("max_commits")
      public Integer maxCommits;
   }

   private static class CommitInfo {
      @SerializedName("hash")
      public String hash;
      @SerializedName("short_hash")
      public String shortHash;
      @SerializedName("author_name")
      public String authorName;
      @SerializedName("author_email")
      public String authorEmail;
      @SerializedName("commit_time")
      public long commitTime;
      @SerializedName("formatted_time")
      public String formattedTime;
      @SerializedName("message")
      public String message;
      @SerializedName("changed_files")
      public List<String> changedFiles;
      @SerializedName("changed_files_count")
      public int changedFilesCount;
   }

   private static class GitCommitsResponse {
      @SerializedName("commits")
      public List<CommitInfo> commits;
      @SerializedName("has_more")
      public boolean hasMore;
   }
}
