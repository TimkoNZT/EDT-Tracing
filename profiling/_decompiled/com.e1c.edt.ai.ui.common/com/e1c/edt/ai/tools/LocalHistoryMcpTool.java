package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
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
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class LocalHistoryMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "LocalHistory";
   private static final int DEFAULT_MAX_ENTRIES = 20;
   private static String QuestionExample = "{\n  \"project_name\": \"MyProject\",\n  \"file_path\": \"src/com/example/MyClass.java\",\n  \"max_entries\": 10\n}";
   private static String AnswerExample = "{\n  \"entries\": [\n    {\n      \"index\": 0,\n      \"revision_id\": \"current\",\n      \"timestamp\": 1642678800000,\n      \"formatted_time\": \"2022-01-20T10:30:45+03:00\",\n      \"file_size\": 1024,\n      \"location\": \"/path/to/file\",\n      \"is_current\": true,\n      \"is_oldest\": false\n    }\n  ],\n  \"total_entries\": 15\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IMarkdownUtils markdownUtils;
   private final ILocalHistoryUtils localHistoryUtils;
   private final IProjectTools projectTools;

   @Inject
   public LocalHistoryMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils, ILocalHistoryUtils localHistoryUtils, IProjectTools projectTools) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(localHistoryUtils);
      Preconditions.checkNotNull(projectTools);
      this.json = json;
      this.messageFactory = messageFactory;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.markdownUtils = markdownUtils;
      this.localHistoryUtils = localHistoryUtils;
      this.projectTools = projectTools;
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
            String filePath = request.filePath;
            if (filePath != null && !filePath.isBlank()) {
               int maxEntries;
               if (request.maxEntries == null) {
                  maxEntries = 20;
               } else if (request.maxEntries <= 0) {
                  maxEntries = Integer.MAX_VALUE;
               } else {
                  maxEntries = request.maxEntries;
               }

               if (call.callKind == ToolCallKind.RENDER) {
                  details.requestMarkdown = MessageFormat.format(Messages.LocalHistoryTitleTemplate, projectName != null ? projectName : Messages.CurrentProject, filePath != null ? filePath : Messages.SelectedFile);
                  return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
               } else {
                  return CompletableFuture.supplyAsync(() -> {
                     try {
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
                                    throw new ToolException("Cannot open the project \"" + projectName + "\". " + error.getMessage(), error, ToolErrorType.RETRYABLE);
                                 }
                              }

                              Optional<IFile> file = this.projectTools.getProjectFile(project, filePath);
                              if (!file.isPresent()) {
                                 throw new ToolException("The file \"" + filePath + "\" does not exist within the IDE project context. " + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope.");
                              } else {
                                 IFile actualFile = (IFile)file.get();
                                 if (cancellationToken.isCanceled()) {
                                    throw new ToolException("Operation was cancelled before retrieving history.");
                                 } else {
                                    List<LocalHistoryEntry> historyEntries;
                                    try {
                                       historyEntries = this.localHistoryUtils.getLocalHistory(actualFile, maxEntries);
                                    } catch (Exception e) {
                                       throw new ToolException("Failed to get local history: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
                                    }

                                    if (cancellationToken.isCanceled()) {
                                       throw new ToolException("Operation was cancelled while processing history.");
                                    } else {
                                       int lastIndex = historyEntries.size() - 1;

                                       for(int i = 0; i < historyEntries.size(); ++i) {
                                          LocalHistoryEntry entry = (LocalHistoryEntry)historyEntries.get(i);
                                          entry.index = i;
                                          entry.isOldest = i == lastIndex;
                                       }

                                       boolean hasMore = historyEntries.size() == maxEntries && maxEntries != Integer.MAX_VALUE;
                                       LocalHistoryResponse response = new LocalHistoryResponse();
                                       response.entries = historyEntries;
                                       response.hasMore = hasMore;
                                       String content = this.json.serialize(response);
                                       StringBuilder responseMarkdown = new StringBuilder();
                                       responseMarkdown.append(MessageFormat.format(Messages.LocalHistoryFoundTemplate, this.markdownUtils.createStyledText(String.valueOf(historyEntries.size()), TextColor.GREEN, FontWeight.BOLD, false), this.markdownUtils.escapeForMarkdown(filePath), this.markdownUtils.escapeForMarkdown(projectName)));
                                       if (!historyEntries.isEmpty()) {
                                          responseMarkdown.append("\n\n<details><summary>").append(Messages.ViewHistory).append("</summary>\n\n");

                                          for(LocalHistoryEntry entry : historyEntries) {
                                             responseMarkdown.append("### **").append(this.markdownUtils.createStyledText(entry.revisionId, TextColor.BLUE, FontWeight.NORMAL, false)).append("**").append(entry.isCurrent ? " " + Messages.Current : "").append(" - ").append(entry.formattedTime).append("\n\n");
                                             responseMarkdown.append("**").append(Messages.FileSize).append(":** ").append(entry.fileSize).append(" bytes\n");
                                             responseMarkdown.append("**").append(Messages.Location).append(":** ").append(this.markdownUtils.escapeForMarkdown(entry.location)).append("\n\n");
                                             responseMarkdown.append("---\n\n");
                                          }

                                          responseMarkdown.append("</details>");
                                       } else {
                                          responseMarkdown.append("\n\n").append(Messages.NoLocalHistoryFound);
                                       }

                                       details.responseMarkdown = responseMarkdown.toString();
                                       return this.messageFactory.createMessage(this, call, content, details);
                                    }
                                 }
                              }
                           } else {
                              throw new ToolException("The project \"" + projectName + "\" does not exist.");
                           }
                        }
                     } catch (Exception e) {
                        throw new ToolException("Failed to get local history: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
                     }
                  });
               }
            } else {
               throw new ToolException("`file_path` is required.");
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
      spec.function.name = "LocalHistory";
      StringBuilder description = new StringBuilder();
      description.append("Lists local history revisions for a file.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Returns recent entries first (index 0 is current).");
      description.append("\n- Includes timestamp, size, and history location.");
      description.append("\n- Each entry includes `index` and `is_oldest` to help select versions.");
      description.append("\n- `location` is a virtual id (`local_history:<revision_id>`) for history entries.");
      description.append("\n- Set `max_entries` to 0 to return all available history.");
      description.append("\n- Works with Eclipse local history when available.");
      description.append("\n- Response includes has_more flag indicating if more history entries are available.");
      description.append("\n\nRelated tools:");
      description.append("\n- Diff revisions: `LocalChanges`.");
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
      McpToolCallProperty filePathProp = new McpToolCallProperty();
      filePathProp.type = "string";
      filePathProp.description = "Relative file path within the project. For example, \"src/com/example/MyClass.java\". Absolute paths are also supported.";
      properties.put("file_path", filePathProp);
      McpToolCallProperty maxEntriesProp = new McpToolCallProperty();
      maxEntriesProp.type = "integer";
      maxEntriesProp.description = "Maximum number of history entries to return. Default: 20. Use 0 to return all entries.";
      properties.put("max_entries", maxEntriesProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("project_name", "file_path");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("file_path")
      public String filePath;
      @SerializedName("max_entries")
      public Integer maxEntries;
   }

   private static class LocalHistoryResponse {
      @SerializedName("entries")
      public List<LocalHistoryEntry> entries;
      @SerializedName("has_more")
      public boolean hasMore;
   }
}
