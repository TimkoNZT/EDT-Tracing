package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.ISettings;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

public class ReadMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "Read";
   private static final int MAX_LINES = 3000;
   private static final int LINE_NUMBER_WIDTH = 7;
   private static String QuestionExample = "{\n  \"path\": \"C:/Projects/MyProject/src/Catalogs/Nomencaltura/Module.bsl\",\n  \"first_line\": 1,\n  \"lines_number\": 10\n}";
   private static String AnswerExample = "{\n  \"content\": \"     1: // Module\\n     2: Procedure Test()\\n     3:     Message(\\\"Hello\\\");\\n     4: EndProcedure\\n     5:\\n     6: Procedure AnotherTest()\\n     7:     Var x = 10;\\n     8: EndProcedure\",\n  \"charset_name\": \"UTF-8\",\n  \"total_lines\": 42,\n  \"note\": \"There are more lines in the file that were not read. Increase lines_number parameter to read more content.\"\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IContentSourceProvider contentSourceProvider;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IFileSystem fileSystem;
   private final IProjectTools projectTools;
   private final IMarkdownUtils markdownUtils;
   private final IFiles files;
   private final ISettings settings;

   @Inject
   public ReadMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IContentSourceProvider contentSourceProvider, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem, IProjectTools projectTools, IMarkdownUtils markdownUtils, IFiles files, ISettings settings) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(contentSourceProvider);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(fileSystem);
      Preconditions.checkNotNull(projectTools);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(files);
      Preconditions.checkNotNull(settings);
      this.json = json;
      this.messageFactory = messageFactory;
      this.contentSourceProvider = contentSourceProvider;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.fileSystem = fileSystem;
      this.projectTools = projectTools;
      this.markdownUtils = markdownUtils;
      this.files = files;
      this.settings = settings;
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
         throw new ToolException("Cannot deserialize arguments. JSON must be a single object with double-quoted keys and strings. Use this example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         String path = request.path;
         if (path != null && !path.isBlank()) {
            int firstLineNumber = request.firstLine != null && request.firstLine > 0 ? request.firstLine : 1;
            int linesNumber = request.linesNumber != null && request.linesNumber > 0 ? request.linesNumber : 1000;
            if (linesNumber > 3000) {
               linesNumber = 3000;
            }

            if (call.callKind == ToolCallKind.RENDER) {
               String href = this.markdownUtils.formatFilePath(path, firstLineNumber - 1, 0, firstLineNumber + linesNumber - 1, 0);
               details.requestMarkdown = MessageFormat.format(Messages.ReadTitleTemplate, href);
               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            } else {
               return CompletableFuture.supplyAsync(() -> {
                  if (cancellationToken.isCanceled()) {
                     throw new ToolException("Operation was cancelled before execution.");
                  } else {
                     String detectedProjectName = this.projectTools.determineProjectName(path);
                     boolean isProjectFile = detectedProjectName != null && !detectedProjectName.isBlank();
                     if (!isProjectFile) {
                        if (!this.settings.isExperimental()) {
                           HashMap<String, Object> response = new HashMap();
                           response.put("content", "");
                           response.put("note", "The file \"" + path + "\" is not part of any project.");
                           details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate, this.files.getDisplayedFileName(new File(path)), this.markdownUtils.createStyledText("0/0", TextColor.RED, FontWeight.BOLD, false, 0.3));
                           return this.messageFactory.createMessage(this, call, this.json.serialize(response), details);
                        } else {
                           try {
                              if (!this.fileSystem.fileExists(path)) {
                                 HashMap<String, Object> response = new HashMap();
                                 response.put("content", "");
                                 response.put("note", "The file \"" + path + "\" does not exist.");
                                 details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate, this.files.getDisplayedFileName(new File(path)), this.markdownUtils.createStyledText("0/0", TextColor.RED, FontWeight.BOLD, false, 0.3));
                                 return this.messageFactory.createMessage(this, call, this.json.serialize(response), details);
                              } else {
                                 Throwable responsex = null;
                                 Object var57 = null;

                                 try {
                                    FileInputStream fileInputStream = new FileInputStream(path);

                                    ToolCallMessage var10000;
                                    try {
                                       InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);

                                       try {
                                          BufferedReader reader = new BufferedReader(inputStreamReader);

                                          try {
                                             int endLineNumber = firstLineNumber + linesNumber - 1;
                                             int totalLines = 0;
                                             int linesRead = 0;
                                             int lastLineSize = 0;
                                             StringBuilder resultContent = new StringBuilder();

                                             for(String line : this.fileSystem.getLines(reader)) {
                                                ++totalLines;
                                                if (totalLines >= firstLineNumber && totalLines <= endLineNumber) {
                                                   resultContent.append(formatLineWithNumberPrefix(totalLines, line));
                                                   lastLineSize = line.length();
                                                   ++linesRead;
                                                }
                                             }

                                             HashMap<String, Object> response = new HashMap();
                                             response.put("content", resultContent.toString());
                                             response.put("charset_name", "UTF-8");
                                             response.put("total_lines", totalLines);
                                             if (endLineNumber < totalLines) {
                                                response.put("note", "There are more lines in the file that were not read. Increase lines_number parameter to read more content.");
                                             }

                                             String styledLineNumber = this.markdownUtils.createStyledText(String.valueOf(String.format("%d/%d", linesRead, totalLines)), TextColor.GREEN, FontWeight.BOLD, false);
                                             String href = this.markdownUtils.formatFilePath(path, firstLineNumber - 1, 0, firstLineNumber + linesRead - 1, lastLineSize);
                                             details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate, href, styledLineNumber);
                                             var10000 = this.messageFactory.createMessage(this, call, this.json.serialize(response), details);
                                          } finally {
                                             if (reader != null) {
                                                reader.close();
                                             }

                                          }
                                       } catch (Throwable var50) {
                                          if (responsex == null) {
                                             responsex = var50;
                                          } else if (responsex != var50) {
                                             responsex.addSuppressed(var50);
                                          }

                                          if (inputStreamReader != null) {
                                             inputStreamReader.close();
                                          }

                                          throw responsex;
                                       }

                                       if (inputStreamReader != null) {
                                          inputStreamReader.close();
                                       }
                                    } catch (Throwable var51) {
                                       if (responsex == null) {
                                          responsex = var51;
                                       } else if (responsex != var51) {
                                          responsex.addSuppressed(var51);
                                       }

                                       if (fileInputStream != null) {
                                          fileInputStream.close();
                                       }

                                       throw responsex;
                                    }

                                    if (fileInputStream != null) {
                                       fileInputStream.close();
                                    }

                                    return var10000;
                                 } catch (Throwable var52) {
                                    if (responsex == null) {
                                       responsex = var52;
                                    } else if (responsex != var52) {
                                       responsex.addSuppressed(var52);
                                    }

                                    throw responsex;
                                 }
                              }
                           } catch (IOException error) {
                              throw new ToolException("Failed to read file", error, ToolErrorType.RETRYABLE);
                           }
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

                           Optional<IFileDocument> optionalDocument = this.projectTools.getProjectFile(project, path).flatMap((file) -> this.contentSourceProvider.getFileDocument(file));
                           if (optionalDocument.isEmpty()) {
                              HashMap<String, Object> response = new HashMap();
                              response.put("content", "");
                              response.put("note", "The file \"" + path + "\" does not exist within the IDE project context.");
                              details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate, this.files.getDisplayedFileName(new File(path)), this.markdownUtils.createStyledText("0/0", TextColor.RED, FontWeight.BOLD, false, 0.3));
                              return this.messageFactory.createMessage(this, call, this.json.serialize(response), details);
                           } else {
                              IFileDocument document = (IFileDocument)optionalDocument.get();
                              IDocument doc = document.getDocument();
                              StringBuilder resultContent = new StringBuilder();
                              int linesRead = 0;
                              int lastLineSize = 0;

                              for(String line : this.fileSystem.getLines(document, firstLineNumber - 1, linesNumber)) {
                                 resultContent.append(formatLineWithNumberPrefix(firstLineNumber + linesRead, line));
                                 lastLineSize = line.length();
                                 ++linesRead;
                              }

                              Response response = new Response();
                              response.content = resultContent.toString();
                              response.charsetName = document.getCharset().name();
                              int totalLines = doc.getNumberOfLines();
                              response.totalLines = totalLines;
                              int endLineNumber = firstLineNumber + linesNumber - 1;
                              if (totalLines > endLineNumber) {
                                 response.note = "There are more lines in the file that were not read. Increase lines_number parameter to read more content.";
                              }

                              String content = this.json.serialize(response);
                              String styledLineNumber = this.markdownUtils.createStyledText(String.format("%d/%d", linesRead, totalLines), TextColor.GREEN, FontWeight.BOLD, false);
                              String href = this.markdownUtils.formatFilePath(path, firstLineNumber - 1, 0, firstLineNumber + linesRead - 1, lastLineSize);
                              details.responseMarkdown = MessageFormat.format(Messages.ReadTemplate, href, styledLineNumber);
                              details.hideAfter = totalLines == 0;
                              return this.messageFactory.createMessage(this, call, content, details);
                           }
                        } else {
                           throw new ToolException("The project \"" + detectedProjectName + "\" does not exist.");
                        }
                     }
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
      spec.function.name = "Read";
      StringBuilder description = new StringBuilder();
      description.append("Reads file content from a project or from the file system.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- `path` is required and must be an absolute path. The system will auto-detect the project from the absolute path.");
      description.append("\n- IMPORTANT: Each line is prefixed with a 7-digit line number and colon (e.g., `    123:`); strip it before editing.");
      description.append("\n- Send only the raw JSON object (no code fences or extra text).");
      description.append("\n- Example (single line is OK): {\"path\":\"C:/Projects/MyProject/src/MainModule.bsl\",\"first_line\":1,\"lines_number\":200}");
      description.append("\n- Defaults: `first_line` = 1, `lines_number` = 2000; capped at 3000.");
      description.append("\n- Preserve exact whitespace and line endings (\\r, \\n, \\t).");
      description.append("\n- If you plan to edit, read a larger chunk to make `origin_content` unique.");
      description.append("\n\nRelated tools:");
      description.append("\n- Locate files: `Find`.");
      description.append("\n- Modify files: `Edit`, `Write`.");
      description.append("\n- MUST use `DeleteMarkers` and `SetMarkers` to update issues, plans, schedules, proposals, tasks, TODO, bookmarks, etc.");
      description.append("\n\nFile references in responses should use HTML `<a>` tags with `edt-file://` URLs and include a `title` attribute:");
      description.append("\n- Format: `<a href=\"edt-file://full_path:line:column:finish_line:finish_column\" title=\"description\">text</a>`");
      description.append("\n- Line and column numbers are 0-based integers.");
      description.append("\n- Example: `<a href=\"edt-file://C:/Projects/MyProject/src/Module.bsl:10:0:20:50\">Procedure in Module.bsl</a>`");
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
      McpToolCallProperty firstLineNumberProp = new McpToolCallProperty();
      firstLineNumberProp.type = "integer";
      firstLineNumberProp.description = "Number of the first line of the file to be read. It is 1-relative. The default is 1";
      properties.put("first_line", firstLineNumberProp);
      McpToolCallProperty linesNumberProp = new McpToolCallProperty();
      linesNumberProp.type = "integer";
      linesNumberProp.description = "Number of lines to read. Default is 2000, maximum is 3000.";
      properties.put("lines_number", linesNumberProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("path");
      spec.function.parameters = parameters;
      return spec;
   }

   private static String formatLineWithNumberPrefix(int lineNumber, String lineContent) {
      return String.format("%7d:", lineNumber) + lineContent;
   }

   private static class Request {
      @SerializedName("path")
      public String path;
      @SerializedName("first_line")
      public Integer firstLine;
      @SerializedName("lines_number")
      public Integer linesNumber;
   }

   private static class Response {
      @SerializedName("content")
      public String content;
      @SerializedName("charset_name")
      public String charsetName;
      @SerializedName("note")
      public String note;
      @SerializedName("total_lines")
      public Integer totalLines;
   }
}
