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
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.MatchEvent;
import org.eclipse.search.ui.text.TextSearchQueryProvider;

public class FindMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "Find";
   private static final int DEFAULT_MAX_ELEMENTS = 32;
   private static String QuestionExample = "{\n  \"search_query\": \"Test.*Service\",\n  \"is_case_sensitive_search\": true,\n  \"is_regular_expression_search\": true,\n  \"search_project_names\": [\"core-api\", \"backend\"],\n  \"file_name_patterns\": [\"*.bsl\", \"*.mdo\"],\n  \"include_derived\": false,\n  \"first_index\": 0,\n  \"max_count\": 64\n}";
   private static String AnswerExample = "{\n  \"results\": [\n    {\n      \"project_name\": \"core-api\",\n      \"path\": \"/home/user/workspace/projects/core-api/src/services/TestUserService.bsl\",\n      \"offset\": 243,\n      \"length\": 16,\n      \"line_offset\": 15,\n      \"line_length\": 16,\n      \"line_number\": 12,\n      \"line_content\": \"function TestUserService()\"\n    }\n  ],\n  \"total_results\": 245\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IMarkdownUtils markdownUtils;

   @Inject
   public FindMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(markdownUtils);
      this.json = json;
      this.messageFactory = messageFactory;
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.markdownUtils = markdownUtils;
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
         throw new ToolException("Cannot deserialize arguments. JSON format is invalid or missing required fields. Use this example: " + QuestionExample + "\n\nRequired field: 'search_query' (string)" + "\nOptional fields: 'is_case_sensitive_search' (boolean), 'is_regular_expression_search' (boolean), " + "'search_project_names' (array), 'file_name_patterns' (array), 'include_derived' (boolean), " + "'first_index' (integer), 'max_count' (integer)");
      } else {
         Request request = (Request)optionalRequest.get();
         if (request.searchQuery == null || request.searchQuery.isBlank()) {
            request.searchQuery = "*";
         }

         String searchQuery = request.searchQuery;
         boolean isFileNameSearch = this.isWildcardPattern(searchQuery) && (request.isRegularExpressionSearch == null || !request.isRegularExpressionSearch);
         boolean isCaseSensitiveSearch = request.isCaseSensitiveSearch != null ? request.isCaseSensitiveSearch : false;
         boolean isRegularExpressionSearch = request.isRegularExpressionSearch != null ? request.isRegularExpressionSearch : false;
         String[] fileNamePatterns = request.fileNamePatterns != null && !request.fileNamePatterns.isEmpty() ? (String[])request.fileNamePatterns.toArray(new String[0]) : null;
         boolean includeDerived = request.includeDerived != null ? request.includeDerived : true;
         boolean includeSubfolders = request.includeSubfolders != null ? request.includeSubfolders : true;
         List<String> projectNames = request.projectNames != null ? request.projectNames : List.of();
         int firstIndex = request.firstIndex != null ? Math.max(0, request.firstIndex) : 0;
         int maxCount = request.maxCount != null && request.maxCount > 0 ? request.maxCount : 32;
         if (call.callKind == ToolCallKind.RENDER) {
            StringBuilder requestMarkdown = new StringBuilder();
            requestMarkdown.append(MessageFormat.format(Messages.SearchTitleTemplate, searchQuery));
            if (!isFileNameSearch) {
               requestMarkdown.append("\n\n").append(Messages.FileNamePatterns).append(": ").append(this.formatFileNamePatterns(fileNamePatterns));
            }

            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
         } else if (isFileNameSearch && projectNames.isEmpty()) {
            throw new ToolException("For file name search (when search_query contains wildcards), `search_project_names` is required.");
         } else {
            return isFileNameSearch ? this.performFileSearch(call, cancellationToken, details, projectNames, searchQuery, includeSubfolders, maxCount) : CompletableFuture.supplyAsync(() -> {
               if (cancellationToken.isCanceled()) {
                  throw new ToolException("Operation was cancelled before execution.");
               } else {
                  List<IResource> roots = new ArrayList();
                  IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                  ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
                  monitor.setCancellationToken(cancellationToken);
                  if (!projectNames.isEmpty()) {
                     for(String projectName : projectNames) {
                        IProject project = root.getProject(projectName);
                        if (project == null || !project.exists()) {
                           throw new ToolException("The project \"" + projectName + "\" does not exist.");
                        }

                        if (!project.isOpen()) {
                           try {
                              project.open(monitor);
                           } catch (CoreException error) {
                              throw new ToolException("Cannot open the project \"" + projectName + "\"", error, ToolErrorType.RETRYABLE);
                           }
                        }

                        roots.add(project);
                     }
                  } else {
                     roots.add(root);
                  }

                  final FileTextSearchScope scope = FileTextSearchScope.newSearchScope((IResource[])roots.toArray(new IResource[0]), fileNamePatterns, includeDerived);

                  ISearchQuery query;
                  try {
                     query = TextSearchQueryProvider.getPreferred().createQuery(new TextSearchQueryProvider.TextSearchInput() {
                        public String getSearchText() {
                           return searchQuery;
                        }

                        public boolean isRegExSearch() {
                           return isRegularExpressionSearch;
                        }

                        public boolean isCaseSensitiveSearch() {
                           return isCaseSensitiveSearch;
                        }

                        public FileTextSearchScope getScope() {
                           return scope;
                        }
                     });
                  } catch (CoreException error) {
                     throw new ToolException("Cannot create search query", error, ToolErrorType.RETRYABLE);
                  }

                  final List<Element> allElements = new ArrayList();
                  final Object lock = new Object();
                  ISearchResultListener listener = new ISearchResultListener() {
                     public void searchResultChanged(SearchResultEvent e) {
                        if (e instanceof MatchEvent) {
                           MatchEvent matchEvent = (MatchEvent)e;
                           synchronized(lock) {
                              Match[] var7;
                              for(Match match : var7 = matchEvent.getMatches()) {
                                 if (match instanceof FileMatch) {
                                    FileMatch fileMatch = (FileMatch)match;
                                    IFile file = fileMatch.getFile();
                                    Element element = new Element();
                                    element.offset = fileMatch.getOffset();
                                    element.length = fileMatch.getLength();
                                    if (file != null) {
                                       element.projectName = file.getProject().getName();
                                       IPath location = file.getRawLocation();
                                       if (location != null) {
                                          element.path = location.toOSString();
                                       }

                                       LineElement line = fileMatch.getLineElement();
                                       if (line != null) {
                                          element.lineOffset = line.getOffset();
                                          element.lineLength = line.getLength();
                                          element.lineNumber = line.getLine();
                                       }

                                       allElements.add(element);
                                    }
                                 }
                              }
                           }
                        }

                     }
                  };
                  query.getSearchResult().addListener(listener);

                  try {
                     query.run(monitor);
                  } catch (OperationCanceledException e) {
                     throw new ToolException("Search was cancelled", e, ToolErrorType.RETRYABLE);
                  } catch (Exception e) {
                     throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
                  } finally {
                     query.getSearchResult().removeListener(listener);
                  }

                  List<Element> elements;
                  if (firstIndex >= allElements.size()) {
                     elements = new ArrayList();
                  } else {
                     int endIndex = Math.min(firstIndex + maxCount, allElements.size());
                     elements = allElements.subList(firstIndex, endIndex);
                  }

                  FindResponse response = new FindResponse();
                  response.results = elements;
                  response.totalResults = allElements.size();
                  String content = this.json.serialize(response);
                  StringBuilder responseMarkdown = new StringBuilder();
                  responseMarkdown.append(MessageFormat.format(Messages.FindTemplate, this.markdownUtils.createStyledText(elements.size() + "/" + response.totalResults, TextColor.GREEN, FontWeight.BOLD, false))).append("\n\n").append(Messages.SearchQuery).append(": ").append("`").append(searchQuery).append("`").append("\n\n").append(Messages.FileNamePatterns).append(": ").append(this.formatFileNamePatterns(fileNamePatterns));
                  if (elements.size() > 0) {
                     responseMarkdown.append("\n\n<details><summary>").append(Messages.SearchResults).append("</summary>\n\n");
                     HashMap<String, List<Element>> projectGroups = new HashMap();

                     for(Element element : elements) {
                        ((List)projectGroups.computeIfAbsent(element.projectName, (k) -> new ArrayList())).add(element);
                     }

                     for(Map.Entry<String, List<Element>> entry : projectGroups.entrySet()) {
                        String projectName = (String)entry.getKey();
                        List<Element> projectElements = (List)entry.getValue();
                        responseMarkdown.append("**").append(this.markdownUtils.escapeForMarkdown(projectName)).append("**");
                        responseMarkdown.append(" (").append(projectElements.size()).append(" ").append(Messages.Matches).append(")\n\n");

                        for(Element element : projectElements) {
                           String formattedPath;
                           if (element.lineNumber > 0) {
                              formattedPath = this.markdownUtils.formatFilePath(element.path, element.lineNumber, 0);
                           } else {
                              formattedPath = this.markdownUtils.formatFilePath(element.path);
                           }

                           responseMarkdown.append("- **").append(formattedPath).append("**");
                           if (element.lineNumber > 0) {
                              responseMarkdown.append(" - ").append(Messages.Line).append(" ").append(element.lineNumber);
                           }

                           responseMarkdown.append("\n");
                        }

                        responseMarkdown.append("\n");
                     }

                     responseMarkdown.append("</details>");
                  }

                  details.responseMarkdown = responseMarkdown.toString();
                  details.hideAfter = elements.size() == 0;
                  return this.messageFactory.createMessage(this, call, content, details);
               }
            });
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "Find";
      StringBuilder description = new StringBuilder();
      description.append("Finds files by content pattern or file name pattern in the IDE.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Provide a search pattern in `search_query`.");
      description.append("\n- If `search_query` contains wildcard patterns (*, ?), performs file name search.");
      description.append("\n- For file name search, `search_project_names` is required.");
      description.append("\n- For content search, searches all projects if `search_project_names` is not specified.");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Narrow scope with project/file parameters to reduce noise.");
      description.append("\n- Use `first_index` and `max_count` for pagination. Response includes `total_results` for all matches.");
      description.append("\n\nRelated tools:");
      description.append("\n- Open/edit results: `Read`, `Edit`.");
      description.append("\n\nExample (content search):");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty searchQueryProp = new McpToolCallProperty();
      searchQueryProp.type = "string";
      searchQueryProp.description = "Text or regular expression to search. Supports wildcards (*, ?) when not using regex. If contains wildcards, performs file name search (requires search_project_names).";
      properties.put("search_query", searchQueryProp);
      McpToolCallProperty isCaseSensitiveSearchProp = new McpToolCallProperty();
      isCaseSensitiveSearchProp.type = "boolean";
      isCaseSensitiveSearchProp.description = "Case-sensitive search. Default: false";
      properties.put("is_case_sensitive_search", isCaseSensitiveSearchProp);
      McpToolCallProperty isRegularExpressionSearchProp = new McpToolCallProperty();
      isRegularExpressionSearchProp.type = "boolean";
      isRegularExpressionSearchProp.description = "Treat search query as regular expression. Default: false";
      properties.put("is_regular_expression_search", isRegularExpressionSearchProp);
      McpToolCallProperty projectNamesProp = new McpToolCallProperty();
      projectNamesProp.type = "array";
      projectNamesProp.description = "Project names to search in. Searches all projects if empty (for content search). Required for file name search (when search_query contains wildcards).";
      properties.put("search_project_names", projectNamesProp);
      McpToolCallProperty fileNamePatternsProp = new McpToolCallProperty();
      fileNamePatternsProp.type = "array";
      fileNamePatternsProp.description = "File name patterns (e.g., [\"*.bsl\", \"*.mdo\"]). Used for content search only.";
      properties.put("file_name_patterns", fileNamePatternsProp);
      McpToolCallProperty includeDerivedProp = new McpToolCallProperty();
      includeDerivedProp.type = "boolean";
      includeDerivedProp.description = "Include derived resources. Default: true. Used for content search only.";
      properties.put("include_derived", includeDerivedProp);
      McpToolCallProperty includeSubfoldersProp = new McpToolCallProperty();
      includeSubfoldersProp.type = "boolean";
      includeSubfoldersProp.description = "Include subfolders in search. Default: true. Used for file name search only.";
      properties.put("include_subfolders", includeSubfoldersProp);
      McpToolCallProperty firstIndexProp = new McpToolCallProperty();
      firstIndexProp.type = "integer";
      firstIndexProp.description = "Index of first element to return (0-based). Use for pagination with max_count. Response includes total_results for all matches. Default: 0";
      properties.put("first_index", firstIndexProp);
      McpToolCallProperty maxCountProp = new McpToolCallProperty();
      maxCountProp.type = "integer";
      maxCountProp.description = "Maximum number of elements to return. Use for pagination with first_index. Response includes total_results for all matches. Default: 64";
      properties.put("max_count", maxCountProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("search_query");
      spec.function.parameters = parameters;
      return spec;
   }

   private boolean isWildcardPattern(String pattern) {
      if (pattern != null && !pattern.isEmpty()) {
         return pattern.contains("*") || pattern.contains("?");
      } else {
         return false;
      }
   }

   private CompletableFuture<ToolCallMessage> performFileSearch(McpToolCall call, ICancellationToken cancellationToken, ToolCallMessageDetails details, List<String> projectNames, String searchPattern, boolean includeSubfolders, int maxCount) {
      return CompletableFuture.supplyAsync(() -> {
         if (cancellationToken.isCanceled()) {
            throw new ToolException("Operation was cancelled before execution.");
         } else {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            List<Element> allElements = new ArrayList();
            boolean isDefaultSearch = "*".equals(searchPattern);
            Iterator var12 = projectNames.iterator();

            while(true) {
               if (var12.hasNext()) {
                  String projectName = (String)var12.next();
                  if (!cancellationToken.isCanceled() && allElements.size() < maxCount) {
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

                        try {
                           ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
                           monitor.setCancellationToken(cancellationToken);
                           this.searchFilesByName(project, searchPattern, includeSubfolders, allElements, maxCount, monitor, cancellationToken);
                           continue;
                        } catch (CoreException e) {
                           throw new ToolException("Search failed", e, ToolErrorType.RETRYABLE);
                        }
                     }

                     throw new ToolException("The project \"" + projectName + "\" does not exist.");
                  }
               }

               String content = this.json.serialize(allElements);
               StringBuilder responseMarkdown = new StringBuilder();
               responseMarkdown.append(MessageFormat.format(Messages.FilesFoundTemplate, this.markdownUtils.createStyledText(String.valueOf(allElements.size()), TextColor.GREEN, FontWeight.BOLD, false))).append("\n\n").append(Messages.SearchQuery).append(": ").append("`").append(isDefaultSearch ? "*" : searchPattern).append("`").append("\n\n").append(Messages.FileNamePatterns).append(": ").append("`").append(isDefaultSearch ? "*" : searchPattern).append("`");
               responseMarkdown.append("\n\n<details><summary>").append(Messages.SearchResults).append("</summary>\n\n");

               for(Element element : allElements) {
                  String formattedPath;
                  if (element.lineNumber > 0) {
                     formattedPath = this.markdownUtils.formatFilePath(element.path, element.lineNumber, 0);
                  } else {
                     formattedPath = this.markdownUtils.formatFilePath(element.path);
                  }

                  responseMarkdown.append("- **").append(formattedPath).append("**\n");
               }

               responseMarkdown.append("</details>");
               details.responseMarkdown = responseMarkdown.toString();
               return this.messageFactory.createMessage(this, call, content, details);
            }
         }
      });
   }

   private void searchFilesByName(IResource container, String pattern, boolean includeSubfolders, List<Element> foundElements, int maxCount, ICancellationProgressMonitor monitor, ICancellationToken cancellationToken) throws CoreException {
      if (!cancellationToken.isCanceled() && foundElements.size() < maxCount) {
         try {
            if (cancellationToken.isCanceled()) {
               return;
            }

            if (container.getType() != 4 && container.getType() != 2) {
               return;
            }

            IResource[] members = ((IContainer)container).members();

            for(IResource member : members) {
               if (cancellationToken.isCanceled() || foundElements.size() >= maxCount) {
                  return;
               }

               if (member.getType() == 1) {
                  if (this.matchesPattern(member.getName(), pattern)) {
                     Element element = new Element();
                     element.projectName = member.getProject().getName();
                     element.path = member.getLocation().toOSString();
                     element.offset = 0;
                     element.length = 0;
                     element.lineOffset = 0;
                     element.lineLength = 0;
                     element.lineNumber = 0;
                     foundElements.add(element);
                  }
               } else if (includeSubfolders && member.getType() == 2) {
                  this.searchFilesByName(member, pattern, includeSubfolders, foundElements, maxCount, monitor, cancellationToken);
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

   private String formatFileNamePatterns(String[] patterns) {
      if (patterns != null && patterns.length != 0) {
         if (patterns.length == 1) {
            return "`" + patterns[0] + "`";
         } else {
            StringBuilder result = new StringBuilder();

            for(int i = 0; i < patterns.length; ++i) {
               if (i > 0) {
                  result.append(", ");
               }

               result.append("`").append(patterns[i]).append("`");
            }

            return result.toString();
         }
      } else {
         return Messages.AllFiles;
      }
   }

   private static class Request {
      @SerializedName("search_query")
      public String searchQuery;
      @SerializedName("is_case_sensitive_search")
      public Boolean isCaseSensitiveSearch;
      @SerializedName("is_regular_expression_search")
      public Boolean isRegularExpressionSearch;
      @SerializedName("search_project_names")
      public List<String> projectNames;
      @SerializedName("file_name_patterns")
      public List<String> fileNamePatterns;
      @SerializedName("include_derived")
      public Boolean includeDerived;
      @SerializedName("include_subfolders")
      public Boolean includeSubfolders;
      @SerializedName("first_index")
      public Integer firstIndex = 0;
      @SerializedName("max_count")
      public Integer maxCount = 32;
   }

   private static class Element {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("path")
      public String path;
      public int offset;
      public int length;
      @SerializedName("line_offset")
      public int lineOffset;
      @SerializedName("line_length")
      public int lineLength;
      @SerializedName("line_number")
      public int lineNumber;
   }

   private static class FindResponse {
      @SerializedName("results")
      public List<Element> results;
      @SerializedName("total_results")
      public int totalResults;
   }
}
