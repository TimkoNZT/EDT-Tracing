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
import com.e1c.edt.ai.ui.GitUtils;
import com.e1c.edt.ai.ui.IGitTools;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.lib.Repository;

public class GitDiffMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "GitDiff";
   private static final int DEFAULT_CONTEXT_LINES = 3;
   private static String QuestionExample = "{\n  \"project_name\": \"MyProject\",\n  \"context_lines\": 5\n}";
   private static String QuestionExampleUncommitted = "{\n  \"project_name\": \"MyProject\",\n  \"uncommitted_changes\": true,\n  \"context_lines\": 5\n}";
   private static String QuestionExampleWithCommits = "{\n  \"project_name\": \"MyProject\",\n  \"old_commit\": \"a1b2c3d4\",\n  \"new_commit\": \"e5f6g7h8\",\n  \"context_lines\": 5\n}";
   private static String AnswerExample = "{\n  \"project_name\": \"MyProject\",\n  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\nindex 1234567..abcdefg 100644\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,5 +1,5 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n  \"context_lines\": 5,\n  \"has_changes\": true\n}";
   private static String AnswerExampleUncommitted = "{\n  \"project_name\": \"MyProject\",\n  \"uncommitted_changes\": true,\n  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\nindex 1234567..abcdefg 100644\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,5 +1,5 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n  \"context_lines\": 5,\n  \"has_changes\": true\n}";
   private static String AnswerExampleWithCommits = "{\n  \"project_name\": \"MyProject\",\n  \"old_commit\": \"a1b2c3d4\",\n  \"new_commit\": \"e5f6g7h8\",\n  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\nindex 1234567..abcdefg 100644\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,5 +1,5 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n  \"context_lines\": 5,\n  \"has_changes\": true\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IMarkdownUtils markdownUtils;
   private final IGitTools gitTools;

   @Inject
   public GitDiffMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils, IGitTools gitTools) {
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
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         String projectName = request.projectName;
         if (projectName != null && !projectName.isBlank()) {
            int contextLines = request.contextLines != null && request.contextLines > 0 ? request.contextLines : 3;
            String oldCommit = request.oldCommit;
            String newCommit = request.newCommit;
            boolean uncommittedChanges = Boolean.TRUE.equals(request.uncommittedChanges);
            if (call.callKind == ToolCallKind.RENDER) {
               details.requestMarkdown = MessageFormat.format(Messages.GitDiffTitleTemplate, projectName != null ? projectName : "current project");
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
                              String diffText;
                              if (oldCommit != null && newCommit != null) {
                                 diffText = this.gitTools.getDiffText(repository, oldCommit, newCommit, contextLines);
                              } else if (uncommittedChanges) {
                                 diffText = this.gitTools.getUncommittedDiffText(repository, contextLines);
                              } else {
                                 diffText = this.gitTools.getDiffText(repository, contextLines);
                              }

                              boolean hasChanges = !diffText.trim().isEmpty();
                              GitDiffResponse response = new GitDiffResponse();
                              response.projectName = projectName;
                              response.oldCommit = oldCommit;
                              response.newCommit = newCommit;
                              response.uncommittedChanges = oldCommit == null && newCommit == null ? uncommittedChanges : null;
                              response.diffText = diffText;
                              response.contextLines = contextLines;
                              response.hasChanges = hasChanges;
                              String content = this.json.serialize(response);
                              StringBuilder responseMarkdown = new StringBuilder();
                              if (hasChanges) {
                                 String diffType;
                                 if (oldCommit != null && newCommit != null) {
                                    diffType = MessageFormat.format(Messages.GitCommitDiffTemplate, this.markdownUtils.createStyledText(oldCommit.substring(0, Math.min(8, oldCommit.length())), TextColor.BLUE, FontWeight.NORMAL, true), this.markdownUtils.createStyledText(newCommit.substring(0, Math.min(8, newCommit.length())), TextColor.BLUE, FontWeight.NORMAL, true), this.markdownUtils.escapeForMarkdown(projectName));
                                 } else if (uncommittedChanges) {
                                    diffType = MessageFormat.format(Messages.GitUncommittedDiffTemplate, this.markdownUtils.createStyledText(String.valueOf(contextLines), TextColor.GREEN, FontWeight.BOLD, false), this.markdownUtils.escapeForMarkdown(projectName));
                                 } else {
                                    diffType = MessageFormat.format(Messages.GitDiffFoundTemplate, this.markdownUtils.createStyledText(String.valueOf(contextLines), TextColor.GREEN, FontWeight.BOLD, false), this.markdownUtils.escapeForMarkdown(projectName));
                                 }

                                 responseMarkdown.append(diffType);
                                 responseMarkdown.append("\n\n");
                                 responseMarkdown.append(this.markdownUtils.buildUnifiedDiffByFile(diffText));
                              } else if (oldCommit != null && newCommit != null) {
                                 responseMarkdown.append(MessageFormat.format(Messages.NoGitCommitChangesTemplate, this.markdownUtils.createStyledText(oldCommit.substring(0, Math.min(8, oldCommit.length())), TextColor.BLUE, FontWeight.BOLD, true), this.markdownUtils.createStyledText(newCommit.substring(0, Math.min(8, newCommit.length())), TextColor.BLUE, FontWeight.BOLD, true)));
                              } else if (uncommittedChanges) {
                                 responseMarkdown.append(MessageFormat.format(Messages.NoGitUncommittedChangesTemplate, this.markdownUtils.escapeForMarkdown(projectName)));
                              } else {
                                 responseMarkdown.append(MessageFormat.format(Messages.NoGitChangesTemplate, this.markdownUtils.escapeForMarkdown(projectName)));
                              }

                              details.responseMarkdown = responseMarkdown.toString();
                              return this.messageFactory.createMessage(this, call, content, details);
                           } catch (Exception e) {
                              throw new ToolException("Failed to get Git diff", e, ToolErrorType.RETRYABLE);
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
      spec.function.name = "GitDiff";
      StringBuilder description = new StringBuilder();
      description.append("Retrieves Git diffs for working directory changes or commit ranges.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Default: staged vs last commit.");
      description.append("\n- Set `uncommitted_changes` to include staged + unstaged.");
      description.append("\n- Provide `old_commit` and `new_commit` to diff specific commits.");
      description.append("\n- `context_lines` controls surrounding lines in the diff.");
      description.append("\n- Requires the project to be a Git repository.");
      description.append("\n\nRelated tools:");
      description.append("\n- Get commit hashes: `GitCommits`.");
      description.append("\n- Non-Git history: `LocalHistory`, `LocalChanges`.");
      description.append("\n\nStaged changes diff example:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      description.append("\n\nUncommitted changes diff example:");
      description.append("\n  Q: ");
      description.append(QuestionExampleUncommitted);
      description.append("\n  A: ");
      description.append(AnswerExampleUncommitted);
      description.append("\n\nCommit comparison example:");
      description.append("\n  Q: ");
      description.append(QuestionExampleWithCommits);
      description.append("\n  A: ");
      description.append(AnswerExampleWithCommits);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty projectNameProp = new McpToolCallProperty();
      projectNameProp.type = "string";
      projectNameProp.description = "Project name in IDE. For example, \"MyProject\".";
      properties.put("project_name", projectNameProp);
      McpToolCallProperty contextLinesProp = new McpToolCallProperty();
      contextLinesProp.type = "integer";
      contextLinesProp.description = "Number of context lines to show around changes. Default: 3";
      properties.put("context_lines", contextLinesProp);
      McpToolCallProperty oldCommitProp = new McpToolCallProperty();
      oldCommitProp.type = "string";
      oldCommitProp.description = "Old commit hash (optional). If provided with new_commit, shows diff between these two commits instead of working directory changes.";
      properties.put("old_commit", oldCommitProp);
      McpToolCallProperty newCommitProp = new McpToolCallProperty();
      newCommitProp.type = "string";
      newCommitProp.description = "New commit hash (optional). If provided with old_commit, shows diff between these two commits instead of working directory changes.";
      properties.put("new_commit", newCommitProp);
      McpToolCallProperty uncommittedChangesProp = new McpToolCallProperty();
      uncommittedChangesProp.type = "boolean";
      uncommittedChangesProp.description = "If true, returns diff for uncommitted changes (staged + unstaged) instead of only staged changes.";
      properties.put("uncommitted_changes", uncommittedChangesProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("project_name");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("context_lines")
      public Integer contextLines;
      @SerializedName("old_commit")
      public String oldCommit;
      @SerializedName("new_commit")
      public String newCommit;
      @SerializedName("uncommitted_changes")
      public Boolean uncommittedChanges;
   }

   private static class GitDiffResponse {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("old_commit")
      public String oldCommit;
      @SerializedName("new_commit")
      public String newCommit;
      @SerializedName("uncommitted_changes")
      public Boolean uncommittedChanges;
      @SerializedName("diff_text")
      public String diffText;
      @SerializedName("context_lines")
      public int contextLines;
      @SerializedName("has_changes")
      public boolean hasChanges;
   }
}
