package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class WriteMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "Write";
   private static String QuestionExample = "{\n  \"path\": \"C:/Projects/AccountingSystem/src/MainModule.bsl\",\n  \"content\": \"Procedure Test()\\n    Message(\\\"Hello\\\");\\nEndProcedure\"\n}";
   private static String AnswerExample = "File written: \"C:/Projects/AccountingSystem/src/MainModule.bsl\"";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IFileSystem fileSystem;
   private final IProjectTools projectTools;
   private final IMarkdownUtils markdownUtils;
   private final IEditingSupport editingSupport;
   private final ILog log;

   @Inject
   public WriteMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem, IProjectTools projectTools, IMarkdownUtils markdownUtils, IEditingSupport editingSupport, ILog log) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(fileSystem);
      Preconditions.checkNotNull(projectTools);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(editingSupport);
      Preconditions.checkNotNull(log);
      this.json = json;
      this.messageFactory = messageFactory;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.fileSystem = fileSystem;
      this.projectTools = projectTools;
      this.markdownUtils = markdownUtils;
      this.editingSupport = editingSupport;
      this.log = log;
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
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         String path = request.path;
         if (path != null && !path.isBlank()) {
            String content = request.content;
            if (content == null) {
               throw new ToolException("`content` is required.");
            } else {
               String charsetName = request.charsetName != null && !request.charsetName.isBlank() ? request.charsetName : "UTF-8";

               byte[] data;
               try {
                  data = content.getBytes(charsetName);
               } catch (UnsupportedEncodingException error) {
                  throw new ToolException("Unsupported charset: \"" + charsetName + "\"", error, ToolErrorType.RETRYABLE);
               }

               if (call.callKind == ToolCallKind.RENDER) {
                  StringBuilder requestMarkdown = new StringBuilder();
                  requestMarkdown.append(MessageFormat.format(Messages.WriteTitleTemplate, this.markdownUtils.formatFilePath(path)));
                  if (request.content != null) {
                     requestMarkdown.append("\n\n");
                     requestMarkdown.append("<details><summary>").append(Messages.WriteDetailsSummary).append("</summary>\n\n");
                     requestMarkdown.append(this.markdownUtils.buildGitDiff(path, (String)null, request.content));
                     requestMarkdown.append("\n</details>");
                  }

                  details.requestMarkdown = requestMarkdown.toString();
                  return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
               } else {
                  return CompletableFuture.supplyAsync(() -> {
                     if (cancellationToken.isCanceled()) {
                        throw new ToolException("Operation was cancelled before execution.");
                     } else {
                        String detectedProjectName = this.projectTools.determineProjectName(path);
                        boolean isProjectFile = detectedProjectName != null && !detectedProjectName.isBlank();
                        if (isProjectFile) {
                           IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                           IProject project = root.getProject(detectedProjectName);
                           if (project == null || !project.exists()) {
                              throw new ToolException("The project \"" + detectedProjectName + "\" does not exist.");
                           }

                           ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
                           monitor.setCancellationToken(cancellationToken);
                           if (!project.isOpen()) {
                              try {
                                 project.open(monitor);
                              } catch (CoreException error) {
                                 throw new ToolException("Cannot open the project \"" + detectedProjectName + "\"", error, ToolErrorType.RETRYABLE);
                              }
                           }

                           Optional<IFile> optionalProjectFile = this.projectTools.getProjectFile(project, path);
                           if (optionalProjectFile.isPresent()) {
                              IFile projectFile = (IFile)optionalProjectFile.get();
                              if (projectFile.exists()) {
                                 try {
                                    if (projectFile.getLocation() != null && projectFile.getLocation().toFile().length() > 0L) {
                                       throw new ToolException("The file \"" + path + "\" already exists and is not empty. Use the `" + "Edit" + "` tool to modify this file.");
                                    }
                                 } catch (Exception var33) {
                                    throw new ToolException("The file \"" + path + "\" already exists. Use the `" + "Edit" + "` tool to modify this file.");
                                 }
                              }

                              if (!this.editingSupport.canEdit(projectFile)) {
                                 throw new ToolException("The file \"" + path + "\" cannot be created. Writing is not supported for this file type or the location is restricted.");
                              }

                              try {
                                 this.createParentFolders(projectFile, monitor);
                                 Throwable errorx = null;
                                 Object var16 = null;

                                 try {
                                    ByteArrayInputStream source = new ByteArrayInputStream(data);

                                    try {
                                       if (projectFile.exists()) {
                                          projectFile.setContents(source, 1, monitor);
                                       } else {
                                          projectFile.create(source, true, monitor);
                                       }

                                       this.refreshResources(projectFile, monitor);
                                    } finally {
                                       if (source != null) {
                                          source.close();
                                       }

                                    }
                                 } catch (Throwable var37) {
                                    if (errorx == null) {
                                       errorx = var37;
                                    } else if (errorx != var37) {
                                       errorx.addSuppressed(var37);
                                    }

                                    throw errorx;
                                 }
                              } catch (IOException | CoreException error) {
                                 this.refreshResourcesSafe(projectFile, monitor);
                                 throw new ToolException("Failed to write file", error, ToolErrorType.RETRYABLE);
                              }

                              StringBuilder response = new StringBuilder();
                              response.append("File written: \"").append(path).append("\".\n");
                              String fileExt = projectFile.getFileExtension();
                              if (fileExt != null) {
                                 label335: {
                                    fileExt = fileExt.toLowerCase();
                                    switch (fileExt.hashCode()) {
                                       case 97851:
                                          if (fileExt.equals("bsl")) {
                                             response.append("ACTION REQUIRED: check that corresponding \"").append(projectFile.getProjectRelativePath().removeFileExtension().addFileExtension("mdo").toPortableString()).append("\" file exists or create it.\n");
                                          }
                                          break label335;
                                       case 107960:
                                          if (!fileExt.equals("mdo")) {
                                             break label335;
                                          }
                                          break;
                                       case 3148996:
                                          if (!fileExt.equals("form")) {
                                             break label335;
                                          }
                                          break;
                                       default:
                                          break label335;
                                    }

                                    response.append("ACTION REQUIRED: verify that the file \"src/Configuration/Configuration.mdo\" has been updated with the new configuration item. Use `Edit` tool.");
                                 }
                              }

                              int newLines = content.split("\\r?\\n", -1).length;
                              StringBuilder changes = new StringBuilder();
                              changes.append(this.markdownUtils.createStyledText("+" + newLines, TextColor.GREEN, FontWeight.BOLD, false));
                              StringBuilder responseMarkdown = new StringBuilder();
                              responseMarkdown.append(MessageFormat.format(Messages.WrittenTemplate, this.markdownUtils.formatFilePath(path), changes));
                              responseMarkdown.append("\n\n");
                              responseMarkdown.append("<details><summary>").append(Messages.WriteDetailsSummary).append("</summary>\n\n");
                              responseMarkdown.append(this.markdownUtils.buildGitDiff(path, (String)null, content));
                              responseMarkdown.append("\n</details>");
                              details.responseMarkdown = responseMarkdown.toString();
                              return this.messageFactory.createMessage(this, call, response.toString(), details);
                           }
                        }

                        try {
                           if (this.fileSystem.fileExists(path) && !this.fileSystem.isFileEmpty(path)) {
                              throw new ToolException("The file \"" + path + "\" already exists and is not empty. Use the `" + "Edit" + "` tool to modify this file.");
                           } else {
                              this.fileSystem.writeAllBytes(path, data);
                              StringBuilder response = new StringBuilder();
                              response.append("File written: \"").append(path).append("\".\n");
                              response.append("⚠️ WARNING: File not part of project. Changes to non-project files may have irreversible consequences.\n");
                              int newLines = content.split("\\r?\\n", -1).length;
                              StringBuilder changes = new StringBuilder();
                              changes.append(this.markdownUtils.createStyledText("+" + newLines, TextColor.GREEN, FontWeight.BOLD, false));
                              StringBuilder responseMarkdown = new StringBuilder();
                              responseMarkdown.append(MessageFormat.format(Messages.WrittenTemplate, this.markdownUtils.formatFilePath(path), changes));
                              responseMarkdown.append("\n\n");
                              responseMarkdown.append("<details><summary>").append(Messages.WriteDetailsSummary).append("</summary>\n\n");
                              responseMarkdown.append(this.markdownUtils.buildGitDiff(path, (String)null, content));
                              responseMarkdown.append("\n</details>");
                              details.responseMarkdown = responseMarkdown.toString();
                              return this.messageFactory.createMessage(this, call, response.toString(), details);
                           }
                        } catch (IOException error) {
                           throw new ToolException("Failed to write file", error, ToolErrorType.RETRYABLE);
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

   private void createParentFolders(IFile file, IProgressMonitor monitor) throws CoreException {
      IContainer container = file.getParent();
      if (container instanceof IFolder && !container.exists()) {
         this.createFolderRecursive((IFolder)container, monitor);
      }

   }

   private void createFolderRecursive(IFolder folder, IProgressMonitor monitor) throws CoreException {
      if (folder != null && !folder.exists()) {
         IContainer parent = folder.getParent();
         if (parent instanceof IFolder) {
            this.createFolderRecursive((IFolder)parent, monitor);
         }

         if (!folder.exists()) {
            folder.create(true, true, monitor);
         }

      }
   }

   private void refreshResources(IFile file, IProgressMonitor monitor) throws CoreException {
      file.refreshLocal(0, monitor);
      if (file.getParent() != null) {
         file.getParent().refreshLocal(1, monitor);
      }

   }

   private void refreshResourcesSafe(IFile file, IProgressMonitor monitor) {
      try {
         this.refreshResources(file, monitor);
      } catch (CoreException error) {
         this.log.logError(error);
      }

   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "Write";
      StringBuilder description = new StringBuilder();
      description.append("Creates a new project file.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Fails if the file already exists and is not empty; empty files can be overwritten. Use `Edit` to modify existing non-empty files.");
      description.append("\n- Verify the target folder and naming patterns before creating files.");
      description.append("\n- Some file types require companions (e.g., .bsl needs a matching .mdo).");
      description.append("\n- Avoid creating docs (*.md/README) unless the user explicitly asks.");
      description.append("\n- Avoid emojis unless explicitly requested.");
      description.append("\n- For temporary files, create them in the system temporary folder: `" + getTempDirectory() + "`.");
      description.append("\n\nRelated tools:");
      description.append("\n- Check existence and context: `Read`.");
      description.append("\n- Update existing files: `Edit`.");
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
      McpToolCallProperty contentProp = new McpToolCallProperty();
      contentProp.type = "string";
      contentProp.description = "Content to write to file.";
      properties.put("content", contentProp);
      McpToolCallProperty charsetNameProp = new McpToolCallProperty();
      charsetNameProp.type = "string";
      charsetNameProp.description = "File encoding, for example, \"UTF-8\", \"windows-1251\", \"KOI8-R\", \"UTF-16\", \"UTF-32\", etc. By default, \"UTF-8\".";
      properties.put("charset_name", charsetNameProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("path", "content");
      spec.function.parameters = parameters;
      return spec;
   }

   private static String getTempDirectory() {
      return System.getProperty("java.io.tmpdir");
   }

   private static class Request {
      @SerializedName("path")
      public String path;
      @SerializedName("content")
      public String content;
      @SerializedName("charset_name")
      public String charsetName;
   }
}
