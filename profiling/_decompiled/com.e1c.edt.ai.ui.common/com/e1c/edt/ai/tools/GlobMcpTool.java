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
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class GlobMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "Glob";
   private static final int LIMIT = 100;
   private static String QuestionExample = "{\n  \"path\": \"/home/user/workspace\",\n  \"pattern\": \"src/**/*.bsl\",\n  \"depth\": 3\n}\n\n// List current directory contents (default):\n{\n  \"pattern\": \"*\"\n}\n\n// Find all Java files in any subdirectory:\n{\n  \"pattern\": \"**/*.java\"\n}";
   private static String AnswerExample = "{\n  \"tree\": \"Documents/\\n ├── Document1/\\n │   ├── ManagerModule.bsl\\n │   ├── ObjectModule.bsl\\n │   └── Document1.mdo\\n └── ...\",\n  \"items\": [\n    {\"path\": \"/home/user/workspace/Documents/Document1/ManagerModule.bsl\", \"type\": \"file\", \"modified\": 1734567890000},\n    {\"path\": \"/home/user/workspace/Documents\", \"type\": \"directory\", \"modified\": 1734567890000}\n  ],\n  \"stats\": {\n    \"total_items\": 35,\n    \"max_depth_reached\": 2\n  }\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IMarkdownUtils markdownUtils;
   private final IPatternMatcher patternMatcher;
   private final Provider<ITreeBuilder> treeBuilderProvider;

   @Inject
   public GlobMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils, IPatternMatcher patternMatcher, Provider<ITreeBuilder> treeBuilderProvider) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(patternMatcher);
      Preconditions.checkNotNull(treeBuilderProvider);
      this.json = json;
      this.messageFactory = messageFactory;
      this.markdownUtils = markdownUtils;
      this.patternMatcher = patternMatcher;
      this.treeBuilderProvider = treeBuilderProvider;
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
         if (path != null && !path.isBlank()) {
            String pattern = request.pattern != null ? request.pattern : "*";
            int userDepth = request.depth != null ? request.depth : 3;
            int depth = pattern.contains("**") && userDepth < 10 ? 10 : userDepth;
            if (call.callKind == ToolCallKind.RENDER) {
               String patternDisplay = request.pattern != null ? this.markdownUtils.escapeForMarkdown(request.pattern) : this.markdownUtils.escapeForMarkdown("*");
               details.requestMarkdown = MessageFormat.format(Messages.GlobTitleTemplate, patternDisplay);
               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            } else {
               return CompletableFuture.supplyAsync(() -> {
                  if (cancellationToken.isCanceled()) {
                     throw new ToolException("Operation was cancelled before execution.");
                  } else {
                     File baseDir = new File(path);
                     if (baseDir.exists() && baseDir.isDirectory()) {
                        Result result = new Result();
                        result.items = new ArrayList();
                        result.stats = new Stats();

                        try {
                           HashSet<String> relevantPaths = new HashSet();
                           ITreeBuilder treeBuilder = (ITreeBuilder)this.treeBuilderProvider.get();
                           this.scanDirectory(baseDir.toPath(), baseDir.toPath(), pattern, depth, 0, result, relevantPaths, cancellationToken, 100);
                           if (!relevantPaths.isEmpty()) {
                              relevantPaths.add(baseDir.getAbsolutePath());
                           }

                           this.scanDirectoryForTree(baseDir.toPath(), baseDir.toPath(), pattern, depth, 0, result, relevantPaths, treeBuilder, cancellationToken);
                           result.tree = treeBuilder.build();
                           result.items.sort((a, b) -> Long.compare(b.modified, a.modified));
                           result.stats.truncated = result.items.size() >= 100;
                        } catch (IOException e) {
                           throw new ToolException("Directory listing failed", e, ToolErrorType.RETRYABLE);
                        }

                        String content = this.json.serialize(result);
                        String styledItemsCount = this.markdownUtils.createStyledText(String.valueOf(result.stats.totalItems), TextColor.GREEN, FontWeight.BOLD, false);
                        details.responseMarkdown = MessageFormat.format(Messages.GlobTemplate, this.markdownUtils.escapeForMarkdown(pattern), styledItemsCount);
                        details.hideAfter = result.stats.totalItems == 0;
                        return this.messageFactory.createMessage(this, call, content, details);
                     } else {
                        throw new ToolException("The directory \"" + path + "\" does not exist or is not a directory.");
                     }
                  }
               });
            }
         } else {
            throw new ToolException("The \"path\" parameter is required and must be a valid directory path.");
         }
      }
   }

   private void scanDirectory(Path baseDir, Path dir, String pattern, int maxDepth, int currentDepth, Result result, Set<String> relevantPaths, ICancellationToken cancellationToken, int limit) throws IOException {
      if (!cancellationToken.isCanceled() && currentDepth <= maxDepth && result.items.size() < limit) {
         Throwable var10 = null;
         Object var11 = null;

         try {
            Stream<Path> stream = Files.list(dir);

            try {
               stream.sorted(Comparator.comparing(Path::getFileName)).forEach((path) -> {
                  if (!cancellationToken.isCanceled() && result.items.size() < limit) {
                     try {
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        String relativePath = baseDir.relativize(path).toString();
                        String normalizedPath = relativePath.replace("\\", "/");
                        boolean matchesPattern = this.patternMatcher.matches(normalizedPath, pattern);
                        if (Files.isDirectory(path, new LinkOption[0])) {
                           if (matchesPattern) {
                              ItemInfo dirInfo = new ItemInfo();
                              dirInfo.path = path.toAbsolutePath().toString();
                              dirInfo.type = "directory";
                              dirInfo.modified = attrs.lastModifiedTime().toMillis();
                              result.items.add(dirInfo);
                              ++result.stats.totalItems;
                              relevantPaths.add(path.toAbsolutePath().toString());
                           }

                           int beforeCount = relevantPaths.size();
                           this.scanDirectory(baseDir, path, pattern, maxDepth, currentDepth + 1, result, relevantPaths, cancellationToken, limit);
                           int afterCount = relevantPaths.size();
                           if (afterCount > beforeCount) {
                              relevantPaths.add(dir.toAbsolutePath().toString());
                           }

                           if (afterCount > beforeCount || matchesPattern) {
                              result.stats.maxDepthReached = Math.max(result.stats.maxDepthReached, currentDepth + 1);
                           }
                        } else if (Files.isRegularFile(path, new LinkOption[0]) && matchesPattern) {
                           ItemInfo fileInfo = new ItemInfo();
                           fileInfo.path = path.toAbsolutePath().toString();
                           fileInfo.type = "file";
                           fileInfo.modified = attrs.lastModifiedTime().toMillis();
                           result.items.add(fileInfo);
                           ++result.stats.totalItems;
                           relevantPaths.add(path.toAbsolutePath().toString());
                           relevantPaths.add(dir.toAbsolutePath().toString());
                           result.stats.maxDepthReached = Math.max(result.stats.maxDepthReached, currentDepth);
                        }
                     } catch (IOException var17) {
                     }

                  }
               });
            } finally {
               if (stream != null) {
                  stream.close();
               }

            }

         } catch (Throwable var18) {
            if (var10 == null) {
               var10 = var18;
            } else if (var10 != var18) {
               var10.addSuppressed(var18);
            }

            throw var10;
         }
      }
   }

   private void scanDirectoryForTree(Path baseDir, Path dir, String pattern, int maxDepth, int currentDepth, Result result, Set<String> relevantPaths, ITreeBuilder treeBuilder, ICancellationToken cancellationToken) throws IOException {
      if (!cancellationToken.isCanceled() && currentDepth <= maxDepth) {
         Throwable var10 = null;
         Object var11 = null;

         try {
            Stream<Path> stream = Files.list(dir);

            try {
               stream.sorted(Comparator.comparing(Path::getFileName)).forEach((path) -> {
                  if (!cancellationToken.isCanceled()) {
                     try {
                        String relativePath = baseDir.relativize(path).toString();
                        String normalizedPath = relativePath.replace("\\", "/");
                        String absolutePath = path.toAbsolutePath().toString();
                        boolean isRelevant = relevantPaths.contains(absolutePath);
                        if (Files.isDirectory(path, new LinkOption[0])) {
                           if (isRelevant) {
                              treeBuilder.addDirectory(normalizedPath, currentDepth);
                              this.scanDirectoryForTree(baseDir, path, pattern, maxDepth, currentDepth + 1, result, relevantPaths, treeBuilder, cancellationToken);
                              treeBuilder.endDirectory();
                           }
                        } else if (Files.isRegularFile(path, new LinkOption[0]) && isRelevant) {
                           treeBuilder.addFile(normalizedPath, currentDepth);
                        }
                     } catch (IOException var14) {
                     }

                  }
               });
            } finally {
               if (stream != null) {
                  stream.close();
               }

            }

         } catch (Throwable var18) {
            if (var10 == null) {
               var10 = var18;
            } else if (var10 != var18) {
               var10.addSuppressed(var18);
            }

            throw var10;
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "Glob";
      StringBuilder description = new StringBuilder();
      description.append("Fast directory listing tool that works with any configuration size.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Lists directory contents with configurable depth and optional pattern matching.");
      description.append("\n- By default, behaves like ls command - shows current directory contents with depth=1 and pattern=\"*\".");
      description.append("\n- Supports glob patterns with path separators (\"/\" or \"\\\\\"):");
      description.append("\n  - \"*.bsl\" - matches files in root directory only");
      description.append("\n  - \"src/**/*.bsl\" - matches .bsl files anywhere under src/");
      description.append("\n  - \"**/*.java\" - matches .java files in any subdirectory");
      description.append("\n  - \"**/test_*.py\" - matches test_*.py files in any subdirectory");
      description.append("\n  - \"*\" - matches any name");
      description.append("\n  - \"?\" - matches any single character");
      description.append("\n  - \"**\" - matches any number of directory segments (including zero)");
      description.append("\n- Returns matching file and directory paths sorted by modification time.");
      description.append("\n- Use this tool when you need to explore directory structure or list files.");
      description.append("\n- Depth parameter controls how deep to traverse subdirectories (0 = only root, 1 = root + one level, etc.).");
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
      pathProp.description = "The directory to search in. Must be a valid directory path.";
      properties.put("path", pathProp);
      McpToolCallProperty patternProp = new McpToolCallProperty();
      patternProp.type = "string";
      patternProp.description = "The search pattern to match files or directories. Supports glob patterns with path separators: \"*.bsl\", \"src/**/*.bsl\", \"**/*.java\", \"**/test_*.py\". Wildcards: '*' (any characters), '?' (single character), '**' (any number of directory segments). If omitted, all files and directories are matched. Default: \"*\"";
      properties.put("pattern", patternProp);
      McpToolCallProperty depthProp = new McpToolCallProperty();
      depthProp.type = "integer";
      depthProp.description = "The maximum depth of subdirectories to search. A value of 0 means only the root directory, 1 includes one level of subdirectories, etc. Defaults to 3 if not specified.";
      properties.put("depth", depthProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("path");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("path")
      public String path;
      @SerializedName("pattern")
      public String pattern;
      @SerializedName("depth")
      public Integer depth;
   }

   private static class ItemInfo {
      @SerializedName("path")
      public String path;
      @SerializedName("type")
      public String type;
      @SerializedName("modified")
      public long modified;
   }

   private static class Stats {
      @SerializedName("total_items")
      public int totalItems;
      @SerializedName("max_depth_reached")
      public int maxDepthReached;
      @SerializedName("truncated")
      public boolean truncated;
   }

   private static class Result {
      @SerializedName("tree")
      public String tree;
      @SerializedName("items")
      public List<ItemInfo> items;
      @SerializedName("stats")
      public Stats stats;
   }
}
