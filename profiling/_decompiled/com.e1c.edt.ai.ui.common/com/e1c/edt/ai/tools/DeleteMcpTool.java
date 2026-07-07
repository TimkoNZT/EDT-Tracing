package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEditingSupport;
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
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
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

public class DeleteMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "Delete";
   private static String QuestionExample = "{\n  \"path\": \"C:/Projects/AccountingSystem/src/MainModule.bsl\"\n}";
   private static String AnswerExample = "File deleted: \"C:/Projects/AccountingSystem/src/MainModule.bsl\"";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IFileSystem fileSystem;
   private final IProjectTools projectTools;
   private final IMarkdownUtils markdownUtils;
   private final IEditingSupport editingSupport;

   @Inject
   public DeleteMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem, IProjectTools projectTools, IMarkdownUtils markdownUtils, IEditingSupport editingSupport) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(fileSystem);
      Preconditions.checkNotNull(projectTools);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(editingSupport);
      this.json = json;
      this.messageFactory = messageFactory;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.fileSystem = fileSystem;
      this.projectTools = projectTools;
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
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample + "\n\nRequired field: 'path' (string)");
      } else {
         Request request = (Request)optionalRequest.get();
         String path = request.path;
         if (path != null && !path.isBlank()) {
            if (call.callKind == ToolCallKind.RENDER) {
               StringBuilder requestMarkdown = new StringBuilder();
               requestMarkdown.append(MessageFormat.format(Messages.DeleteTitleTemplate, this.markdownUtils.formatFilePath(path)));
               details.requestMarkdown = requestMarkdown.toString();
               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            } else {
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
                              this.fileSystem.deleteFile(path);
                              StringBuilder response = new StringBuilder();
                              response.append("File deleted: \"").append(path).append("\".\n");
                              response.append("⚠️ WARNING: File not part of project. Changes to non-project files may have irreversible consequences.\n");
                              String changes = this.markdownUtils.createStyledText("-1", TextColor.RED, FontWeight.BOLD, true);
                              details.responseMarkdown = MessageFormat.format(Messages.DeletedTemplate, this.markdownUtils.formatFilePath(path), changes);
                              return this.messageFactory.createMessage(this, call, response.toString(), details);
                           }
                        } catch (IOException error) {
                           throw new ToolException("Failed to delete file", error, ToolErrorType.RETRYABLE);
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
                              throw new ToolException("The file \"" + path + "\" does not exist within the IDE project context. " + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope.");
                           } else if (!this.editingSupport.canDelete((IFile)projectFile.get())) {
                              String filePathForError = (String)projectFile.map((f) -> f.getProjectRelativePath().toOSString()).orElse(path);
                              throw new ToolException("The file \"" + filePathForError + "\" cannot be deleted. Deletion is not supported for this file type or the file is locked.");
                           } else {
                              IFile actualFile = (IFile)projectFile.get();

                              try {
                                 ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
                                 monitor.setCancellationToken(cancellationToken);
                                 String displayPath = actualFile.getProjectRelativePath().toPortableString();
                                 actualFile.delete(true, monitor);
                                 if (actualFile.getParent() != null) {
                                    actualFile.getParent().refreshLocal(1, monitor);
                                 }

                                 StringBuilder response = new StringBuilder();
                                 response.append("File deleted: \"").append(displayPath).append("\".");
                                 StringBuilder changes = new StringBuilder();
                                 changes.append(this.markdownUtils.createStyledText(Messages.Deleted, TextColor.RED, FontWeight.BOLD, true));
                                 details.responseMarkdown = MessageFormat.format(Messages.DeletedTemplate, this.markdownUtils.formatFilePath(path), changes);
                                 return this.messageFactory.createMessage(this, call, response.toString(), details);
                              } catch (CoreException error) {
                                 throw new ToolException("Failed to delete file", error, ToolErrorType.RETRYABLE);
                              }
                           }
                        } else {
                           throw new ToolException("The project \"" + detectedProjectName + "\" does not exist.");
                        }
                     }
                  }
               }).handle((result, error) -> {
                  if (error != null) {
                     return error instanceof ToolException ? this.messageFactory.createError(this, call, ((ToolException)error).getMessage()) : this.messageFactory.createError(this, call, "Unexpected error: " + error.getMessage());
                  } else {
                     return result;
                  }
               });
            }
         } else {
            throw new ToolException("`path` is required.");
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "Delete";
      StringBuilder description = new StringBuilder();
      description.append("Deletes a project file.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Verify the file exists before deletion.");
      description.append("\n- This operation cannot be undone.");
      description.append("\n- Review file content and impact before deleting.");
      description.append("\n\nRelated tools:");
      description.append("\n- Read file: `Read`.");
      description.append("\n- Review history: `GitDiff`, `LocalHistory`.");
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
      parameters.properties = properties;
      parameters.required = Arrays.asList("path");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("path")
      public String path;
   }
}
