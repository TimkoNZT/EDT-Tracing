package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IFileDocument;
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
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

public class EditMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "Edit";
   private static String QuestionExample = "{\n  \"path\": \"C:/Projects/AccountingSystem/src/MainModule.bsl\",\n  \"old_content\": \"Procedure Test()\\n    SetId(1);\\nEndProcedure\",\n  \"new_content\": \"Procedure Test()\\n    SetId(9);\\nEndProcedure\",\n  \"replace_all\": false\n}";
   private static String AnswerExample = "File updated: \"C:/Projects/AccountingSystem/src/MainModule.bsl\"";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IContentSourceProvider contentSourceProvider;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IFileSystem fileSystem;
   private final IProjectTools projectTools;
   private final IDispatcher dispatcher;
   private final IContentReplacer contentReplacer;
   private final IMarkdownUtils markdownUtils;
   private final IEditingSupport editingSupport;

   @Inject
   public EditMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IContentSourceProvider contentSourceProvider, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem, IProjectTools projectTools, IDispatcher dispatcher, IContentReplacer contentReplacer, IMarkdownUtils markdownUtils, IEditingSupport editingSupport) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(contentSourceProvider);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(fileSystem);
      Preconditions.checkNotNull(projectTools);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(contentReplacer);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(editingSupport);
      this.json = json;
      this.messageFactory = messageFactory;
      this.contentSourceProvider = contentSourceProvider;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.fileSystem = fileSystem;
      this.projectTools = projectTools;
      this.dispatcher = dispatcher;
      this.contentReplacer = contentReplacer;
      this.markdownUtils = markdownUtils;
      this.editingSupport = editingSupport;
      this.spec = createSpecification();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = false;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. JSON must be a single object with double-quoted keys and strings. Use this example: " + QuestionExample + "\n\nRequired fields: 'path' (string), " + "'old_content' (string), 'new_content' (string)" + "\nOptional field: 'replace_all' (boolean)");
      } else {
         Request request = (Request)optionalRequest.get();
         String path = request.path;
         if (path != null && !path.isBlank()) {
            String oldContent = request.oldContent;
            if (oldContent == null) {
               throw new ToolException("`old_content` is required and cannot be null.");
            } else {
               String newContent = request.newContent;
               if (newContent == null) {
                  throw new ToolException("`new_content` is required.");
               } else if (call.callKind == ToolCallKind.RENDER) {
                  StringBuilder requestMarkdown = new StringBuilder();
                  requestMarkdown.append(MessageFormat.format(Messages.EditTitleTemplate, this.markdownUtils.formatFilePath(path)));
                  if (request.oldContent != null && request.newContent != null) {
                     requestMarkdown.append("\n\n");
                     requestMarkdown.append("<details><summary>").append(Messages.EditDetailsSummary).append("</summary>\n\n");
                     requestMarkdown.append(this.markdownUtils.buildGitDiff(path, request.oldContent, request.newContent));
                     requestMarkdown.append("\n</details>");
                  }

                  details.requestMarkdown = requestMarkdown.toString();
                  return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
               } else {
                  boolean replaceAll = request.replaceAll != null ? request.replaceAll : false;
                  return CompletableFuture.supplyAsync(() -> {
                     if (cancellationToken.isCanceled()) {
                        throw new ToolException("Operation was cancelled before execution.");
                     } else {
                        String detectedProjectName = this.projectTools.determineProjectName(path);
                        boolean isProjectFile = detectedProjectName != null && !detectedProjectName.isBlank();
                        if (!isProjectFile) {
                           try {
                              if (!this.fileSystem.fileExists(path)) {
                                 throw new ToolException("The file \"" + path + "\" does not exist.");
                              } else {
                                 byte[] fileData = this.fileSystem.readAllBytes(path);
                                 String content = new String(fileData, StandardCharsets.UTF_8);
                                 ReplaceResult replaceResult = this.contentReplacer.replace(content, oldContent, newContent, System.lineSeparator(), replaceAll);
                                 if (!replaceResult.isSuccess()) {
                                    String errorMessage = this.getReplacementErrorMessage(replaceAll, replaceResult);
                                    throw new ToolException(errorMessage);
                                 } else {
                                    ReplacementResult replacementResult = new ReplacementResult();
                                    replacementResult.updatedContent = replaceResult.getUpdatedContent();
                                    replacementResult.addedLines = replaceResult.getAddedLines();
                                    replacementResult.removedLines = replaceResult.getRemovedLines();
                                    byte[] updatedData = replacementResult.updatedContent.getBytes(StandardCharsets.UTF_8);
                                    this.fileSystem.writeAllBytes(path, updatedData);
                                    StringBuilder response = new StringBuilder();
                                    response.append("File updated: \"").append(path).append("\".\n");
                                    response.append("⚠️ WARNING: File not part of project. Changes to non-project files may have irreversible consequences.\n");
                                    StringBuilder responseMarkdown = new StringBuilder();
                                    responseMarkdown.append(MessageFormat.format(Messages.EditedTemplate, this.markdownUtils.formatFilePath(path), this.createChangesString(replacementResult.addedLines, replacementResult.removedLines)));
                                    responseMarkdown.append("\n\n");
                                    responseMarkdown.append("<details><summary>").append(Messages.EditDetailsSummary).append("</summary>\n\n");
                                    responseMarkdown.append(this.markdownUtils.buildGitDiff(path, oldContent, newContent));
                                    responseMarkdown.append("\n</details>");
                                    details.responseMarkdown = responseMarkdown.toString();
                                    return this.messageFactory.createMessage(this, call, response.toString(), details);
                                 }
                              }
                           } catch (IOException error) {
                              throw new ToolException("Failed to edit file", error, ToolErrorType.RETRYABLE);
                           }
                        } else {
                           IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                           IProject project = root.getProject(detectedProjectName);
                           if (project != null && project.exists()) {
                              if (!project.isOpen()) {
                                 try {
                                    ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
                                    monitor.setCancellationToken(cancellationToken);
                                    project.open(monitor);
                                 } catch (CoreException error) {
                                    throw new ToolException("Cannot open the project \"" + detectedProjectName + "\"", error, ToolErrorType.RETRYABLE);
                                 }
                              }

                              Optional<IFile> projectFile = this.projectTools.getProjectFile(project, path);
                              if (!projectFile.isPresent()) {
                                 throw new ToolException("The file \"" + path + "\" does not exist within the IDE project context. " + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope. " + "Use the `" + "Write" + "` tool to create a new file.");
                              } else if (!this.editingSupport.canEdit((IFile)projectFile.orElse((Object)null))) {
                                 throw new ToolException("The file \"" + path + "\" cannot be edited. Editing is not supported for this file type or the file is locked.");
                              } else {
                                 IFile actualFile = (IFile)projectFile.get();
                                 Optional<IFileDocument> optionalDocument = this.contentSourceProvider.getFileDocument(actualFile);
                                 if (optionalDocument.isEmpty()) {
                                    String filePathForError = actualFile.getProjectRelativePath().toOSString();
                                    throw new ToolException("The file \"" + filePathForError + "\" does not exist within the IDE project context. " + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope. " + "Use the `" + "Write" + "` tool to create a new file.");
                                 } else {
                                    IFileDocument fileDocument = (IFileDocument)optionalDocument.get();
                                    IDocument document = fileDocument.getDocument();
                                    Optional<String> optionalCurrentContent = this.dispatcher.dispatch((Supplier)(() -> document.get()));
                                    if (optionalCurrentContent.isEmpty()) {
                                       throw new ToolException("Cannot read the file \"" + path + "\".");
                                    } else {
                                       String currentContent = (String)optionalCurrentContent.get();
                                       ReplaceResult replaceResult = this.contentReplacer.replace(currentContent, oldContent, newContent, System.lineSeparator(), replaceAll);
                                       if (!replaceResult.isSuccess()) {
                                          String errorMessage = this.getReplacementErrorMessage(replaceAll, replaceResult);
                                          throw new ToolException(errorMessage);
                                       } else {
                                          ReplacementResult replacementResult = new ReplacementResult();
                                          replacementResult.updatedContent = replaceResult.getUpdatedContent();
                                          replacementResult.addedLines = replaceResult.getAddedLines();
                                          replacementResult.removedLines = replaceResult.getRemovedLines();
                                          Optional<Exception> optionalError = this.dispatcher.dispatch((Supplier)(() -> {
                                             try {
                                                fileDocument.setContent(replacementResult.updatedContent);
                                                fileDocument.save();
                                                return null;
                                             } catch (Exception error) {
                                                return error;
                                             }
                                          }));
                                          if (optionalError.isPresent()) {
                                             throw new ToolException("Failed to save file", (Throwable)optionalError.get(), ToolErrorType.RETRYABLE);
                                          } else {
                                             StringBuilder response = new StringBuilder();
                                             String displayPath = actualFile.getProjectRelativePath().toPortableString();
                                             response.append("File updated: \"").append(displayPath).append("\".");
                                             StringBuilder responseMarkdown = new StringBuilder();
                                             responseMarkdown.append(MessageFormat.format(Messages.EditedTemplate, this.markdownUtils.formatFilePath(path), this.createChangesString(replacementResult.addedLines, replacementResult.removedLines)));
                                             responseMarkdown.append("\n\n");
                                             responseMarkdown.append("<details><summary>").append(Messages.EditDetailsSummary).append("</summary>\n\n");
                                             responseMarkdown.append(this.markdownUtils.buildGitDiff(path, oldContent, newContent));
                                             responseMarkdown.append("\n</details>");
                                             details.responseMarkdown = responseMarkdown.toString();
                                             return this.messageFactory.createMessage(this, call, response.toString(), details);
                                          }
                                       }
                                    }
                                 }
                              }
                           } else {
                              throw new ToolException("The project \"" + detectedProjectName + "\" does not exist.");
                           }
                        }
                     }
                  });
               }
            }
         } else {
            throw new ToolException("`path` is required.");
         }
      }
   }

   private String getReplacementErrorMessage(boolean replaceAll, ReplaceResult replaceResult) {
      if (replaceAll) {
         return "Original content not found in file. Verify the `old_content`.";
      } else {
         return replaceResult.hasMultipleOccurrences() ? "Multiple matches found for original content. Change the `old_content` to avoid multiple matches. Provide a larger `old_content` with more surrounding lines (minimum 3)." : "Original content not found in file. Verify the `old_content`.";
      }
   }

   private String createChangesString(int addedLines, int removedLines) {
      StringBuilder changes = new StringBuilder();
      if (addedLines > 0) {
         changes.append(this.markdownUtils.createStyledText("+" + addedLines, TextColor.GREEN, FontWeight.BOLD, false));
      }

      if (removedLines > 0) {
         if (changes.length() > 0) {
            changes.append(' ');
         }

         changes.append(this.markdownUtils.createStyledText("-" + removedLines, TextColor.RED, FontWeight.BOLD, false));
      }

      return changes.toString();
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "Edit";
      StringBuilder description = new StringBuilder();
      description.append("Edits an existing file by exact string replacement.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- MUST read the file first with `Read`!!!");
      description.append("\n- MUST remove line-number prefixes from `Read` output before using `old_content` or `new_content`.");
      description.append("\n- MUST specify `old_content` exactly, including spaces, tabs, and line separators, but without line-number prefixes.");
      description.append("\n- Provide a unique `old_content` with enough surrounding lines (min 3) to avoid ambiguity.");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Use `replace_all` only when you want to replace every match.");
      description.append("\n- To delete content, set `new_content` to an empty string.");
      description.append("\n- Avoid emojis unless explicitly requested.");
      description.append("\n\nRelated tools:");
      description.append("\n- Create new files: `Write`.");
      description.append("\n- Delete files: `Delete`.");
      description.append("\n- MUST use `DeleteMarkers` and `SetMarkers` to update issues, plans, schedules, proposals, tasks, TODO, bookmarks, etc.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty pathProp = new McpToolCallProperty();
      pathProp.type = "string";
      pathProp.description = "Absolute path to the file. The system will auto-detect the project from the absolute path.";
      properties.put("path", pathProp);
      McpToolCallProperty oldContentProp = new McpToolCallProperty();
      oldContentProp.type = "string";
      oldContentProp.description = "The fragment of the file content that will be replaced. Provide a larger `old_content` with more surrounding lines (minimum 3).";
      properties.put("old_content", oldContentProp);
      McpToolCallProperty newContentProp = new McpToolCallProperty();
      newContentProp.type = "string";
      newContentProp.description = "The content fragment that will replace the original (`old_content`). Can be empty to delete content.";
      properties.put("new_content", newContentProp);
      McpToolCallProperty replaceAllProp = new McpToolCallProperty();
      replaceAllProp.type = "boolean";
      replaceAllProp.description = "If true, all occurrences of the `old_content` fragment will be replaced. If false, only the single occurrence will be replaced. If no fragments are found, or more than one is found, the request will fail. False by default.";
      properties.put("replace_all", replaceAllProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("path", "old_content", "new_content");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class ReplacementResult {
      String updatedContent;
      int addedLines;
      int removedLines;
   }

   private static class Request {
      @SerializedName("path")
      public String path;
      @SerializedName("old_content")
      public String oldContent;
      @SerializedName("new_content")
      public String newContent;
      @SerializedName("replace_all")
      public Boolean replaceAll;
   }
}
