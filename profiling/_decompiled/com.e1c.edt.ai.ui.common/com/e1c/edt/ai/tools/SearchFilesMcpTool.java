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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class SearchFilesMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "SearchFiles";
   private static final int DEFAULT_MAX_FILES = 32;
   private static String QuestionExample = "{\n  \"search_pattern\": \"*.bsl\",\n  \"include_subfolders\": true,\n  \"max_results\": 20\n}\n\n// Search in all projects:\n{\n  \"search_pattern\": \"*.xml\",\n  \"max_results\": 50\n}\n\n// Search with path (determines project automatically):\n{\n  \"path\": \"C:/Projects/MyProject/src\",\n  \"search_pattern\": \"*.bsl\",\n  \"max_results\": 20\n}";
   private static String AnswerExample = "[\n  {\n    \"project_name\": \"MyProject\",\n    \"path\": \"C:/Projects/MyProject/src/CommonModules/MainModule/Module.bsl\"\n  }\n]";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IMarkdownUtils markdownUtils;
   private final IProjectTools projectTools;

   @Inject
   public SearchFilesMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils, IProjectTools projectTools) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(projectTools);
      this.json = json;
      this.messageFactory = messageFactory;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.markdownUtils = markdownUtils;
      this.projectTools = projectTools;
      this.spec = createSpecification();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = true;
      details.hideAfter = true;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         String path = request.path;
         String searchPattern = request.searchPattern != null ? request.searchPattern : "*";
         boolean includeSubfolders = request.includeSubfolders != null ? request.includeSubfolders : true;
         int maxResults = request.maxResults != null && request.maxResults > 0 ? request.maxResults : 32;
         if (call.callKind == ToolCallKind.RENDER) {
            String pattern = request.searchPattern != null ? request.searchPattern : "*";
            StringBuilder requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.FindFilesTitleTemplate, pattern)).append("\n\n").append(Messages.SearchQuery).append(": ").append("`").append(this.markdownUtils.escapeForMarkdown(pattern)).append("`");
            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
         } else {
            return CompletableFuture.supplyAsync(() -> {
               if (cancellationToken.isCanceled()) {
                  throw new ToolException("Operation was cancelled before execution.");
               } else {
                  IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                  ArrayList<FileInfo> foundFiles = new ArrayList();

                  try {
                     ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
                     monitor.setCancellationToken(cancellationToken);
                     if (path != null && !path.isBlank()) {
                        String determinedProject = this.projectTools.determineProjectName(path);
                        if (determinedProject != null) {
                           IProject project = root.getProject(determinedProject);
                           if (project == null || !project.exists()) {
                              throw new ToolException("The project \"" + determinedProject + "\" does not exist.");
                           }

                           if (!project.isOpen()) {
                              try {
                                 project.open(monitor);
                              } catch (CoreException error) {
                                 throw new ToolException("Cannot open the project \"" + determinedProject + "\"", error, ToolErrorType.RETRYABLE);
                              }
                           }

                           IContainer container = root.getContainerForLocation(new org.eclipse.core.runtime.Path(path));
                           if (container == null) {
                              throw new ToolException("The directory \"" + path + "\" does not exist.");
                           }

                           this.searchFiles(container, searchPattern, includeSubfolders, foundFiles, maxResults, monitor, cancellationToken);
                        } else {
                           this.searchFilesViaIO(path, searchPattern, includeSubfolders, foundFiles, maxResults, cancellationToken);
                        }
                     } else {
                        IProject[] projects = root.getProjects();

                        for(IProject project : projects) {
                           if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults) {
                              break;
                           }

                           if (!project.isOpen()) {
                              try {
                                 project.open(monitor);
                              } catch (CoreException var18) {
                                 continue;
                              }
                           }

                           this.searchFiles(project, searchPattern, includeSubfolders, foundFiles, maxResults, monitor, cancellationToken);
                        }
                     }
                  } catch (CoreException e) {
                     throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
                  } catch (IOException e) {
                     throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
                  }

                  String content = this.json.serialize(foundFiles);
                  StringBuilder responseMarkdown = new StringBuilder();
                  responseMarkdown.append(MessageFormat.format(Messages.FilesFoundTemplate, this.markdownUtils.createStyledText(String.valueOf(foundFiles.size()), TextColor.GREEN, FontWeight.BOLD, false))).append("\n\n").append(Messages.SearchQuery).append(": ").append("`").append(searchPattern).append("`");
                  responseMarkdown.append("\n\n<details><summary>").append(Messages.SearchResults).append("</summary>\n\n");

                  for(FileInfo fileInfo : foundFiles) {
                     responseMarkdown.append("- **").append(this.markdownUtils.escapeForMarkdown(fileInfo.path)).append("**\n");
                     responseMarkdown.append("\n");
                  }

                  responseMarkdown.append("</details>");
                  details.responseMarkdown = responseMarkdown.toString();
                  details.hideAfter = foundFiles.size() == 0;
                  return this.messageFactory.createMessage(this, call, content, details);
               }
            });
         }
      }
   }

   private void searchFiles(IResource container, String pattern, boolean includeSubfolders, List<FileInfo> foundFiles, int maxResults, ICancellationProgressMonitor monitor, ICancellationToken cancellationToken) throws CoreException {
      if (!cancellationToken.isCanceled() && foundFiles.size() < maxResults) {
         try {
            if (cancellationToken.isCanceled()) {
               return;
            }

            if (container.getType() != 4 && container.getType() != 2) {
               return;
            }

            IResource[] members = ((IContainer)container).members();

            for(IResource member : members) {
               if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults) {
                  return;
               }

               if (member.getType() == 1) {
                  if (this.matchesPattern(member.getName(), pattern)) {
                     FileInfo fileInfo = new FileInfo();
                     fileInfo.projectName = member.getProject().getName();
                     fileInfo.path = member.getLocation().toOSString();
                     foundFiles.add(fileInfo);
                  }
               } else if (includeSubfolders && member.getType() == 2) {
                  this.searchFiles(member, pattern, includeSubfolders, foundFiles, maxResults, monitor, cancellationToken);
               }
            }
         } catch (CoreException var14) {
         }

      }
   }

   private boolean matchesPattern(String fileName, String pattern) {
      String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
      return fileName.matches(regex);
   }

   private void searchFilesViaIO(String basePath, String pattern, boolean includeSubfolders, List<FileInfo> foundFiles, int maxResults, ICancellationToken cancellationToken) throws IOException {
      if (!cancellationToken.isCanceled() && foundFiles.size() < maxResults) {
         File baseDir = new File(basePath);
         if (baseDir.exists() && baseDir.isDirectory()) {
            Throwable var8 = null;
            Object var9 = null;

            try {
               Stream<Path> stream = includeSubfolders ? Files.walk(baseDir.toPath()) : Files.list(baseDir.toPath());

               try {
                  stream.filter((path) -> !Files.isDirectory(path, new LinkOption[0])).filter((path) -> {
                     if (!cancellationToken.isCanceled() && foundFiles.size() < maxResults) {
                        String fileName = path.getFileName().toString();
                        return this.matchesPattern(fileName, pattern);
                     } else {
                        return false;
                     }
                  }).limit((long)(maxResults - foundFiles.size())).forEach((path) -> {
                     FileInfo fileInfo = new FileInfo();
                     fileInfo.projectName = null;
                     fileInfo.path = path.toAbsolutePath().toString();
                     foundFiles.add(fileInfo);
                  });
               } finally {
                  if (stream != null) {
                     stream.close();
                  }

               }

            } catch (Throwable var16) {
               if (var8 == null) {
                  var8 = var16;
               } else if (var8 != var16) {
                  var8.addSuppressed(var16);
               }

               throw var8;
            }
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "SearchFiles";
      StringBuilder description = new StringBuilder();
      description.append("Finds files by name pattern in a project or all projects.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Supports wildcards: `*` for any characters, `?` for a single character.");
      description.append("\n- If path is specified, it attempts to determine the project via IFileSystem.");
      description.append("\n  If the path is not within any project, searches using IO API.");
      description.append("\n  If path is not specified, searches in all projects.");
      description.append("\n- Can search recursively or only in the root folder.");
      description.append("\n- Limits results to avoid overload on large projects.");
      description.append("\n\nRelated tools:");
      description.append("\n- Search by content: `Find`.");
      description.append("\n- Open/edit files: `Read`, `Edit`.");
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
      pathProp.description = "Optional absolute path to search in. If specified, attempts to determine the project via IFileSystem. If the path is not within any project, searches using IO API.";
      properties.put("path", pathProp);
      McpToolCallProperty searchPatternProp = new McpToolCallProperty();
      searchPatternProp.type = "string";
      searchPatternProp.description = "File name search pattern. Supports wildcards (*, ?). Default: \"*\" (all files).";
      properties.put("search_pattern", searchPatternProp);
      McpToolCallProperty includeSubfoldersProp = new McpToolCallProperty();
      includeSubfoldersProp.type = "boolean";
      includeSubfoldersProp.description = "Include subfolders in search. Default: true";
      properties.put("include_subfolders", includeSubfoldersProp);
      McpToolCallProperty maxResultsProp = new McpToolCallProperty();
      maxResultsProp.type = "integer";
      maxResultsProp.description = "Maximum number of results to return. Default: 32";
      properties.put("max_results", maxResultsProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList();
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("path")
      public String path;
      @SerializedName("search_pattern")
      public String searchPattern;
      @SerializedName("include_subfolders")
      public Boolean includeSubfolders;
      @SerializedName("max_results")
      public Integer maxResults;
   }

   private static class FileInfo {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("path")
      public String path;
   }
}
