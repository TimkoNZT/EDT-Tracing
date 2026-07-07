package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.text.quicksearch.internal.core.LineItem;
import org.eclipse.text.quicksearch.internal.core.QuickTextQuery;
import org.eclipse.text.quicksearch.internal.core.QuickTextSearchRequestor;
import org.eclipse.text.quicksearch.internal.core.QuickTextSearcher;
import org.eclipse.text.quicksearch.internal.core.pathmatch.ResourceMatchers;
import org.eclipse.text.quicksearch.internal.core.priority.PriorityFunction;

public class SearchTextMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "SearchText";
   private static final int DEFAULT_MAX_ELEMENTS = 32;
   private static final int MAX_LINE_LEN = 3000;
   private static final int MAX_RESULTS = 3000;
   private static String QuestionExample = "{\n  \"search_query\": \"Test.*Service\",\n  \"file_path_patterns\": [\"src/**/*.bsl\", \"*.mdo\", \"config/Configuration.xml\"],\n  \"first_index\": 0,\n  \"max_count\": 64\n}";
   private static String AnswerExample = "{\n  \"results\": [\n    {\n      \"project_name\": \"core-api\",\n      \"path\": \"/home/user/workspace/projects/core-api/src/services/TestUserService.bsl\",\n      \"offset\": 243,\n      \"length\": 16,\n      \"line_offset\": 15,\n      \"line_length\": 16,\n      \"line_number\": 12,\n      \"line_content\": \"function TestUserService()\"\n    }\n  ],\n  \"total_results\": 245\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IMarkdownUtils markdownUtils;

   @Inject
   public SearchTextMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(markdownUtils);
      this.json = json;
      this.messageFactory = messageFactory;
      this.markdownUtils = markdownUtils;
      this.spec = this.createSpecification();
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
         throw new ToolException("Cannot deserialize arguments. JSON format is invalid or missing required fields. Use this example: " + QuestionExample + "\n\nRequired field: 'search_query' (string)" + "\nOptional fields: 'file_path_patterns' (array), 'first_index' (integer), 'max_count' (integer)");
      } else {
         Request request = (Request)optionalRequest.get();
         if (request.searchQuery != null && !request.searchQuery.isBlank()) {
            String searchQuery = request.searchQuery;
            List<String> filePathPatterns = request.filePathPatterns;
            int firstIndex = request.firstIndex != null ? Math.max(0, request.firstIndex) : 0;
            int maxCount = request.maxCount != null && request.maxCount > 0 ? request.maxCount : 32;
            if (call.callKind == ToolCallKind.RENDER) {
               StringBuilder requestMarkdown = new StringBuilder();
               requestMarkdown.append(MessageFormat.format(Messages.SearchTitleTemplate, searchQuery));
               if (filePathPatterns != null && !filePathPatterns.isEmpty()) {
                  requestMarkdown.append("\n\n").append(Messages.FileNamePatterns).append(": ").append(this.formatFilePathPatterns(filePathPatterns));
               }

               details.requestMarkdown = requestMarkdown.toString();
               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            } else {
               return CompletableFuture.supplyAsync(() -> {
                  if (cancellationToken.isCanceled()) {
                     throw new ToolException("Operation was cancelled before execution.");
                  } else {
                     final List<Element> allElements = new ArrayList();
                     final ReadWriteLock lock = new ReentrantReadWriteLock();
                     final int maxTotalElements = Math.min(firstIndex + maxCount, 3000);
                     QuickTextQuery query = new QuickTextQuery(searchQuery, false);
                     QuickTextSearcher searcher = new QuickTextSearcher(query, new PriorityFunction() {
                        public double priority(IResource resource) {
                           return (double)0.0F;
                        }
                     }, 3000, new QuickTextSearchRequestor() {
                        public void add(LineItem match) {
                           lock.writeLock().lock();

                           try {
                              if (!cancellationToken.isCanceled() && allElements.size() < maxTotalElements) {
                                 Element element = SearchTextMcpTool.this.createElement(match);
                                 if (element != null) {
                                    allElements.add(element);
                                 }
                              }
                           } finally {
                              lock.writeLock().unlock();
                           }

                        }

                        public void clear() {
                           lock.writeLock().lock();

                           try {
                              allElements.clear();
                           } finally {
                              lock.writeLock().unlock();
                           }

                        }

                        public void revoke(LineItem match) {
                           String path = match.getFile().getFullPath().toString();
                           int lineNumber = match.getLineNumber();
                           int offset = match.getOffset();
                           lock.writeLock().lock();

                           try {
                              Iterator<Element> iterator = allElements.iterator();

                              while(iterator.hasNext()) {
                                 Element element = (Element)iterator.next();
                                 if (SearchTextMcpTool.this.isSameElement(element, path, lineNumber, offset)) {
                                    iterator.remove();
                                 }
                              }
                           } finally {
                              lock.writeLock().unlock();
                           }

                        }

                        public void update(LineItem match) {
                           String path = match.getFile().getFullPath().toString();
                           int lineNumber = match.getLineNumber();
                           int offset = match.getOffset();
                           lock.writeLock().lock();

                           try {
                              int i = 0;

                              while(true) {
                                 if (i >= allElements.size()) {
                                    return;
                                 }

                                 Element element = (Element)allElements.get(i);
                                 if (SearchTextMcpTool.this.isSameElement(element, path, lineNumber, offset)) {
                                    Element updatedElement = SearchTextMcpTool.this.createElement(match);
                                    if (updatedElement != null) {
                                       allElements.set(i, updatedElement);
                                    }
                                    break;
                                 }

                                 ++i;
                              }
                           } finally {
                              lock.writeLock().unlock();
                           }

                        }
                     });
                     if (filePathPatterns != null && !filePathPatterns.isEmpty()) {
                        String patterns = String.join(",", filePathPatterns);
                        searcher.setPathMatcher(ResourceMatchers.commaSeparatedPaths(patterns));
                     }

                     try {
                        while(searcher.isActive() && !cancellationToken.isCanceled()) {
                           Thread.sleep(50L);
                        }
                     } catch (InterruptedException var34) {
                        Thread.currentThread().interrupt();
                     } finally {
                        searcher.cancel();
                     }

                     if (cancellationToken.isCanceled()) {
                        throw new ToolException("Search was cancelled");
                     } else if (firstIndex >= 3000) {
                        throw new ToolException("Parameter 'first_index' cannot be greater than or equal to 3000. Maximum pagination depth is 3000 results.");
                     } else {
                        lock.readLock().lock();

                        int totalResults;
                        List<Element> elements;
                        try {
                           if (firstIndex >= allElements.size()) {
                              elements = new ArrayList();
                           } else {
                              int endIndex = Math.min(firstIndex + maxCount, allElements.size());
                              elements = new ArrayList(allElements.subList(firstIndex, endIndex));
                           }

                           totalResults = allElements.size();
                        } finally {
                           lock.readLock().unlock();
                        }

                        SearchTextResponse response = new SearchTextResponse();
                        response.results = elements;
                        response.totalResults = totalResults;
                        String content = this.json.serialize(response);
                        StringBuilder responseMarkdown = new StringBuilder();
                        String resultCountText;
                        if (response.totalResults <= maxCount && firstIndex <= 0) {
                           resultCountText = String.valueOf(elements.size());
                        } else {
                           resultCountText = response.totalResults + "/" + elements.size();
                        }

                        responseMarkdown.append(MessageFormat.format(Messages.FindTemplate, this.markdownUtils.createStyledText(resultCountText, TextColor.GREEN, FontWeight.BOLD, false))).append("\n\n").append(Messages.SearchQuery).append(": `").append(searchQuery).append("`");
                        if (filePathPatterns != null && !filePathPatterns.isEmpty()) {
                           responseMarkdown.append("\n\n").append(Messages.FileNamePatterns).append(": ").append(this.formatFilePathPatterns(filePathPatterns));
                        }

                        if (!elements.isEmpty()) {
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
                                 String formattedPath = this.markdownUtils.formatFilePath(element.path, element.lineNumber, 0);
                                 responseMarkdown.append("- **").append(formattedPath).append("**");
                                 responseMarkdown.append(" - ").append(Messages.Line).append(" ").append(element.lineNumber);
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
                  }
               });
            }
         } else {
            throw new ToolException("Field 'search_query' is required and cannot be empty.");
         }
      }
   }

   private Element createElement(LineItem match) {
      IFile file = match.getFile();
      if (file == null) {
         return null;
      } else {
         Element element = new Element();
         element.projectName = file.getProject().getName();
         element.path = file.getFullPath().toString();
         element.offset = match.getOffset();
         element.length = match.getText() != null ? match.getText().length() : 0;
         element.lineOffset = match.getOffset();
         element.lineLength = match.getText() != null ? match.getText().length() : 0;
         element.lineNumber = match.getLineNumber();
         element.lineContent = match.getText();
         return element;
      }
   }

   private boolean isSameElement(Element element, String path, int lineNumber, int offset) {
      return path.equals(element.path) && lineNumber == element.lineNumber && offset == element.offset;
   }

   private McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "SearchText";
      StringBuilder description = new StringBuilder();
      description.append("Searches for text in files using the quick search API.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Provide a search pattern in `search_query`.");
      description.append("\n- Optionally use `file_path_patterns` to filter by file types (e.g., [\"*.bsl\", \"*.mdo\"] or directory patterns like \"src/**/*.bsl\").");
      description.append("\n- Use `first_index` and `max_count` for pagination. Response includes `total_results` for all matches.");
      description.append("\n- Searches all projects by default.");
      description.append("\n\nRelated tools:");
      description.append("\n- Open/edit results: `").append("Read").append("`, `").append("Edit").append("`.");
      description.append("\n\nExample:");
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
      searchQueryProp.description = "Text pattern to search for. Supports wildcard patterns (*, ?).";
      properties.put("search_query", searchQueryProp);
      McpToolCallProperty filePathPatternsProp = new McpToolCallProperty();
      filePathPatternsProp.type = "array";
      filePathPatternsProp.description = "File path patterns (e.g., [\"*.bsl\", \"*.mdo\", \"src/**/*.bsl\"]). If not specified, searches all files.";
      properties.put("file_path_patterns", filePathPatternsProp);
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

   private String formatFilePathPatterns(List<String> patterns) {
      if (patterns != null && !patterns.isEmpty()) {
         if (patterns.size() == 1) {
            return "`" + (String)patterns.get(0) + "`";
         } else {
            StringBuilder result = new StringBuilder();

            for(int i = 0; i < patterns.size(); ++i) {
               if (i > 0) {
                  result.append(", ");
               }

               result.append("`").append((String)patterns.get(i)).append("`");
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
      @SerializedName("file_path_patterns")
      public List<String> filePathPatterns;
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
      @SerializedName("line_content")
      public String lineContent;
   }

   private static class SearchTextResponse {
      @SerializedName("results")
      public List<Element> results;
      @SerializedName("total_results")
      public int totalResults;
   }
}
