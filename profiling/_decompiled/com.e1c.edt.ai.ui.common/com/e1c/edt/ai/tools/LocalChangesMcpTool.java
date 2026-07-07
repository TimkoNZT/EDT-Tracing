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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;

public class LocalChangesMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "LocalChanges";
   private static final String CURRENT_REVISION = "current";
   private static final String LATEST_REVISION = "latest";
   private static final String OLDEST_REVISION = "oldest";
   private static final String PREVIOUS_REVISION = "previous";
   private static final String LOCAL_HISTORY_PREFIX = "local_history:";
   private static final int DEFAULT_CONTEXT_LINES = 3;
   private static final int DEFAULT_MAX_ENTRIES = 20;
   private static String QuestionExample = "{\n  \"project_name\": \"MyProject\",\n  \"file_path\": \"src/com/example/MyClass.java\",\n  \"revision_id\": \"file_20240101-120000\",\n  \"context_lines\": 3\n}";
   private static String QuestionExampleWithLocation = "{\n  \"project_name\": \"MyProject\",\n  \"file_path\": \"src/com/example/MyClass.java\",\n  \"history_location\": \"C:/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/123abc\",\n  \"context_lines\": 3\n}";
   private static String QuestionExampleBetweenRevisions = "{\n  \"project_name\": \"MyProject\",\n  \"file_path\": \"src/com/example/MyClass.java\",\n  \"from_revision_id\": \"oldest\",\n  \"to_revision_id\": \"latest\",\n  \"context_lines\": 3\n}";
   private static String QuestionExampleFirstChange = "{\n  \"project_name\": \"MyProject\",\n  \"file_path\": \"src/com/example/MyClass.java\",\n  \"from_index\": 12,\n  \"to_index\": 11,\n  \"context_lines\": 3,\n  \"max_entries\": 0\n}";
   private static String AnswerExample = "{\n  \"project_name\": \"MyProject\",\n  \"file_path\": \"src/com/example/MyClass.java\",\n  \"from_revision_id\": \"file_20240101-120000\",\n  \"from_history_location\": \"C:/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/123abc\",\n  \"to_revision_id\": \"current\",\n  \"to_history_location\": \"C:/workspace/MyProject/src/com/example/MyClass.java\",\n  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,3 +1,3 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n  \"context_lines\": 3,\n  \"has_changes\": true\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IMarkdownUtils markdownUtils;
   private final ILocalHistoryUtils localHistoryUtils;
   private final IProjectTools projectTools;

   @Inject
   public LocalChangesMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils, ILocalHistoryUtils localHistoryUtils, IProjectTools projectTools) {
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
               int contextLines = request.contextLines != null && request.contextLines > 0 ? request.contextLines : 3;
               int maxEntries;
               if (request.maxEntries == null) {
                  maxEntries = 20;
               } else if (request.maxEntries <= 0) {
                  maxEntries = Integer.MAX_VALUE;
               } else {
                  maxEntries = request.maxEntries;
               }

               boolean hasFromSelectors = hasAny(request.fromRevisionId, request.fromHistoryLocation, request.fromIndex);
               boolean hasToSelectors = hasAny(request.toRevisionId, request.toHistoryLocation, request.toIndex);
               boolean hasLegacySelectors = hasAny(request.revisionId, request.historyLocation);
               if (!hasFromSelectors && !hasToSelectors && !hasLegacySelectors) {
                  throw new ToolException("`revision_id` or `history_location` is required, or provide `from_*`/`to_*` selectors.");
               } else if (call.callKind == ToolCallKind.RENDER) {
                  details.requestMarkdown = MessageFormat.format(Messages.LocalChangesTitleTemplate, projectName != null ? projectName : Messages.CurrentProject, filePath != null ? filePath : Messages.SelectedFile);
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
                                 throw new ToolException("Cannot open the project \"" + projectName + "\". " + error.getMessage(), error, ToolErrorType.RETRYABLE);
                              }
                           }

                           Optional<IFile> file = this.projectTools.getProjectFile(project, filePath);
                           if (!file.isPresent()) {
                              throw new ToolException("The file \"" + filePath + "\" does not exist within the IDE project context. " + "The file may exist outside the project directory, but IDE tools can only access files within the current project scope.");
                           } else {
                              IFile actualFile = (IFile)file.get();

                              try {
                                 RevisionSelector fromSelector = new RevisionSelector();
                                 RevisionSelector toSelector = new RevisionSelector();
                                 if (!hasFromSelectors && !hasToSelectors) {
                                    fromSelector.revisionId = request.revisionId;
                                    fromSelector.historyLocation = request.historyLocation;
                                    toSelector.revisionId = "current";
                                 } else {
                                    fromSelector.revisionId = request.fromRevisionId;
                                    fromSelector.historyLocation = request.fromHistoryLocation;
                                    fromSelector.index = request.fromIndex;
                                    toSelector.revisionId = request.toRevisionId;
                                    toSelector.historyLocation = request.toHistoryLocation;
                                    toSelector.index = request.toIndex;
                                    if (!hasFromSelectors) {
                                       fromSelector.revisionId = "current";
                                    }

                                    if (!hasToSelectors) {
                                       toSelector.revisionId = "current";
                                    }
                                 }

                                 List<LocalHistoryEntry> historyEntries = !needsHistoryEntries(fromSelector) && !needsHistoryEntries(toSelector) ? null : this.localHistoryUtils.getLocalHistory(actualFile, maxEntries);
                                 if (historyEntries != null) {
                                    int lastIndex = historyEntries.size() - 1;

                                    for(int i = 0; i < historyEntries.size(); ++i) {
                                       LocalHistoryEntry entry = (LocalHistoryEntry)historyEntries.get(i);
                                       entry.index = i;
                                       entry.isOldest = i == lastIndex;
                                    }
                                 }

                                 List<HistoryState> historyStates = !needsHistoryStates(fromSelector) && !needsHistoryStates(toSelector) ? null : getHistoryStates(actualFile, maxEntries);
                                 ResolvedRevision fromRevision = resolveRevision(actualFile, fromSelector, historyEntries, historyStates);
                                 ResolvedRevision toRevision = resolveRevision(actualFile, toSelector, historyEntries, historyStates);
                                 byte[] oldContent = fromRevision.getContent();
                                 byte[] newContent = toRevision.getContent();
                                 String diffText = createDiffText(filePath, oldContent, newContent, contextLines);
                                 boolean hasChanges = !diffText.trim().isEmpty();
                                 LocalChangesResponse response = new LocalChangesResponse();
                                 response.projectName = projectName;
                                 response.filePath = filePath;
                                 response.fromRevisionId = fromRevision.revisionId;
                                 response.fromHistoryLocation = fromRevision.historyLocation;
                                 response.toRevisionId = toRevision.revisionId;
                                 response.toHistoryLocation = toRevision.historyLocation;
                                 response.diffText = diffText;
                                 response.contextLines = contextLines;
                                 response.hasChanges = hasChanges;
                                 String content = this.json.serialize(response);
                                 StringBuilder responseMarkdown = new StringBuilder();
                                 String revisionLabel = this.markdownUtils.createStyledText(response.fromRevisionId + " -> " + response.toRevisionId, TextColor.BLUE, FontWeight.NORMAL, false);
                                 if (hasChanges) {
                                    responseMarkdown.append(MessageFormat.format(Messages.LocalChangesFoundTemplate, this.markdownUtils.escapeForMarkdown(filePath), revisionLabel, this.markdownUtils.escapeForMarkdown(projectName)));
                                    responseMarkdown.append("\n\n");
                                    responseMarkdown.append(this.markdownUtils.buildUnifiedDiffByFile(diffText));
                                 } else {
                                    responseMarkdown.append(MessageFormat.format(Messages.NoLocalChangesFoundTemplate, this.markdownUtils.escapeForMarkdown(filePath), revisionLabel, this.markdownUtils.escapeForMarkdown(projectName)));
                                 }

                                 details.responseMarkdown = responseMarkdown.toString();
                                 return this.messageFactory.createMessage(this, call, content, details);
                              } catch (Exception e) {
                                 throw new ToolException("Failed to get local changes: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
                              }
                           }
                        } else {
                           throw new ToolException("The project \"" + projectName + "\" does not exist.");
                        }
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

   private static String createDiffText(String filePath, byte[] oldContent, byte[] newContent, int contextLines) throws Exception {
      RawText oldText = new RawText(oldContent);
      RawText newText = new RawText(newContent);
      DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(SupportedAlgorithm.HISTOGRAM);
      EditList edits = diffAlgorithm.diff(RawTextComparator.DEFAULT, oldText, newText);
      if (edits.isEmpty()) {
         return "";
      } else {
         String normalizedPath = filePath.replace('\\', '/');
         ByteArrayOutputStream output = new ByteArrayOutputStream();
         output.write(("diff --git a/" + normalizedPath + " b/" + normalizedPath + "\n").getBytes(StandardCharsets.UTF_8));
         output.write(("--- a/" + normalizedPath + "\n").getBytes(StandardCharsets.UTF_8));
         output.write(("+++ b/" + normalizedPath + "\n").getBytes(StandardCharsets.UTF_8));
         Throwable var10 = null;
         Object var11 = null;

         try {
            DiffFormatter formatter = new DiffFormatter(output);

            try {
               formatter.setContext(contextLines);
               formatter.format(edits, oldText, newText);
            } finally {
               if (formatter != null) {
                  formatter.close();
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

         return output.toString(StandardCharsets.UTF_8);
      }
   }

   private static boolean hasAny(String value, String otherValue) {
      return value != null && !value.isBlank() || otherValue != null && !otherValue.isBlank();
   }

   private static boolean hasAny(String value, String otherValue, Integer index) {
      return value != null && !value.isBlank() || otherValue != null && !otherValue.isBlank() || index != null;
   }

   private static boolean needsHistoryEntries(RevisionSelector selector) {
      if (selector == null) {
         return false;
      } else if (selector.index != null) {
         return true;
      } else if (selector.historyLocation != null && !selector.historyLocation.isBlank()) {
         return false;
      } else if (selector.revisionId != null && !selector.revisionId.isBlank()) {
         return !isCurrentRevision(selector.revisionId);
      } else {
         return false;
      }
   }

   private static boolean needsHistoryStates(RevisionSelector selector) {
      if (selector == null) {
         return false;
      } else if (selector.historyLocation != null && selector.historyLocation.startsWith("local_history:")) {
         return true;
      } else if (selector.index != null) {
         return true;
      } else if (selector.revisionId != null && !selector.revisionId.isBlank()) {
         return !isCurrentRevision(selector.revisionId);
      } else {
         return false;
      }
   }

   private static boolean isCurrentRevision(String revisionId) {
      return "current".equalsIgnoreCase(revisionId);
   }

   private static boolean isLatestRevision(String revisionId) {
      return "latest".equalsIgnoreCase(revisionId) || "previous".equalsIgnoreCase(revisionId);
   }

   private static boolean isOldestRevision(String revisionId) {
      return "oldest".equalsIgnoreCase(revisionId);
   }

   private static ResolvedRevision resolveRevision(IFile file, RevisionSelector selector, Iterable<LocalHistoryEntry> historyEntries, List<HistoryState> historyStates) {
      if (selector == null) {
         throw new RuntimeException("Revision selector is not specified.");
      } else if (selector.historyLocation != null && !selector.historyLocation.isBlank()) {
         if (selector.historyLocation.startsWith("local_history:")) {
            String revisionId = selector.historyLocation.substring("local_history:".length());
            if (revisionId.isBlank()) {
               revisionId = selector.revisionId;
            }

            return resolveHistoryState(revisionId, historyStates);
         } else {
            Path path = Paths.get(selector.historyLocation);
            String revisionId = selector.revisionId != null && !selector.revisionId.isBlank() ? selector.revisionId : path.getFileName().toString();
            return LocalChangesMcpTool.ResolvedRevision.forPath(path, revisionId, selector.historyLocation);
         }
      } else if (selector.index != null) {
         LocalHistoryEntry entry = getEntryByIndex(historyEntries, selector.index);
         return entry.isCurrent ? resolveCurrent(file) : resolveHistoryState(entry.revisionId, historyStates);
      } else if (selector.revisionId != null && !selector.revisionId.isBlank()) {
         if (isCurrentRevision(selector.revisionId)) {
            return resolveCurrent(file);
         } else if (isLatestRevision(selector.revisionId)) {
            LocalHistoryEntry entry = getLatestHistoryEntry(historyEntries, historyStates);
            return resolveHistoryState(entry.revisionId, historyStates);
         } else if (isOldestRevision(selector.revisionId)) {
            LocalHistoryEntry entry = getOldestHistoryEntry(historyEntries, historyStates);
            return resolveHistoryState(entry.revisionId, historyStates);
         } else {
            return resolveHistoryState(selector.revisionId, historyStates);
         }
      } else {
         return resolveCurrent(file);
      }
   }

   private static ResolvedRevision resolveCurrent(IFile file) {
      Path path = Paths.get(file.getLocation().toFile().getAbsolutePath());
      return LocalChangesMcpTool.ResolvedRevision.forPath(path, "current", path.toString());
   }

   private static LocalHistoryEntry getEntryByIndex(Iterable<LocalHistoryEntry> historyEntries, int index) {
      if (historyEntries == null) {
         throw new RuntimeException("Local history is required to resolve index.");
      } else {
         for(LocalHistoryEntry entry : historyEntries) {
            if (entry.index != null && entry.index == index) {
               return entry;
            }
         }

         throw new RuntimeException("Local history entry with index \"" + index + "\" was not found.");
      }
   }

   private static LocalHistoryEntry getLatestHistoryEntry(Iterable<LocalHistoryEntry> historyEntries, List<HistoryState> historyStates) {
      if (historyEntries == null) {
         throw new RuntimeException("Local history is required to resolve latest revision.");
      } else {
         for(LocalHistoryEntry entry : historyEntries) {
            if (!entry.isCurrent) {
               return entry;
            }
         }

         if (historyStates != null && !historyStates.isEmpty()) {
            HistoryState first = (HistoryState)historyStates.get(0);
            LocalHistoryEntry entry = new LocalHistoryEntry();
            entry.revisionId = first.revisionId;
            entry.isCurrent = false;
            return entry;
         } else {
            throw new RuntimeException("No local history entries available.");
         }
      }
   }

   private static LocalHistoryEntry getOldestHistoryEntry(Iterable<LocalHistoryEntry> historyEntries, List<HistoryState> historyStates) {
      if (historyEntries == null) {
         throw new RuntimeException("Local history is required to resolve oldest revision.");
      } else {
         LocalHistoryEntry oldest = null;

         for(LocalHistoryEntry entry : historyEntries) {
            if (!entry.isCurrent) {
               oldest = entry;
            }
         }

         if (oldest == null) {
            if (historyStates != null && !historyStates.isEmpty()) {
               LocalHistoryEntry entry = new LocalHistoryEntry();
               entry.revisionId = ((HistoryState)historyStates.get(historyStates.size() - 1)).revisionId;
               entry.isCurrent = false;
               return entry;
            } else {
               throw new RuntimeException("No local history entries available.");
            }
         } else {
            return oldest;
         }
      }
   }

   private static ResolvedRevision resolveHistoryState(String revisionId, List<HistoryState> historyStates) {
      if (revisionId != null && !revisionId.isBlank()) {
         if (historyStates == null) {
            throw new RuntimeException("Local history is required to resolve revision \"" + revisionId + "\".");
         } else {
            for(HistoryState state : historyStates) {
               if (revisionId.equals(state.revisionId)) {
                  return LocalChangesMcpTool.ResolvedRevision.forContent(state.content, revisionId, "local_history:" + revisionId);
               }
            }

            throw new RuntimeException("Local history entry \"" + revisionId + "\" was not found.");
         }
      } else {
         throw new RuntimeException("Revision id is required to resolve history state.");
      }
   }

   private static List<HistoryState> getHistoryStates(IFile file, int maxEntries) throws CoreException {
      IFileState[] states = file.getHistory((IProgressMonitor)null);
      ArrayList<HistoryState> result = new ArrayList();
      if (states != null && states.length != 0) {
         List<IFileState> historyStates = Arrays.asList(states);
         historyStates.sort((s1, s2) -> Long.compare(s2.getModificationTime(), s1.getModificationTime()));
         int limit = historyStates.size();
         if (maxEntries != Integer.MAX_VALUE) {
            limit = Math.min(maxEntries - 1, historyStates.size());
         }

         for(int i = 0; i < limit; ++i) {
            IFileState state = (IFileState)historyStates.get(i);
            if (state.exists()) {
               HistoryState historyState = new HistoryState();
               historyState.revisionId = buildRevisionId(state);
               historyState.content = readStateContent(state);
               result.add(historyState);
            }
         }

         return result;
      } else {
         return result;
      }
   }

   private static String buildRevisionId(IFileState state) {
      return state.getName() + "_" + generateRevisionId(state.getModificationTime());
   }

   private static byte[] readStateContent(IFileState state) {
      try {
         Throwable var1 = null;
         Object var2 = null;

         try {
            InputStream stream = state.getContents();

            byte[] var10000;
            try {
               var10000 = stream.readAllBytes();
            } finally {
               if (stream != null) {
                  stream.close();
               }

            }

            return var10000;
         } catch (Throwable var11) {
            if (var1 == null) {
               var1 = var11;
            } else if (var1 != var11) {
               var1.addSuppressed(var11);
            }

            throw var1;
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed to read local history content: " + e.getMessage(), e);
      }
   }

   private static String generateRevisionId(long timestamp) {
      return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "LocalChanges";
      StringBuilder description = new StringBuilder();
      description.append("Diffs local history revisions and returns a Git-style diff.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Provide `from_*` and `to_*` selectors to diff two revisions.");
      description.append("\n- If only one side is provided, the other side defaults to `current`.");
      description.append("\n- `revision_id` supports special values: `current`, `latest`, `oldest`, `previous`.");
      description.append("\n- Use `from_index`/`to_index` with `LocalHistory` indexes to get first changes.");
      description.append("\n- Returns diff in standard Git diff format.");
      description.append("\n\nRelated tools:");
      description.append("\n- List revisions: `LocalHistory`.");
      description.append("\n\nLocal history revision diff example:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      description.append("\n\nBetween revisions example:");
      description.append("\n  Q: ");
      description.append(QuestionExampleBetweenRevisions);
      description.append("\n  A: ");
      description.append(AnswerExample);
      description.append("\n\nFirst change example (oldest -> next newer):");
      description.append("\n  Q: ");
      description.append(QuestionExampleFirstChange);
      description.append("\n  A: ");
      description.append(AnswerExample);
      description.append("\n\nHistory location diff example:");
      description.append("\n  Q: ");
      description.append(QuestionExampleWithLocation);
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
      McpToolCallProperty revisionIdProp = new McpToolCallProperty();
      revisionIdProp.type = "string";
      revisionIdProp.description = "Revision id from LocalHistory response. Required if history_location is not provided.";
      properties.put("revision_id", revisionIdProp);
      McpToolCallProperty historyLocationProp = new McpToolCallProperty();
      historyLocationProp.type = "string";
      historyLocationProp.description = "Absolute path to the local history file. Optional alternative to revision_id.";
      properties.put("history_location", historyLocationProp);
      McpToolCallProperty fromRevisionIdProp = new McpToolCallProperty();
      fromRevisionIdProp.type = "string";
      fromRevisionIdProp.description = "Start revision id for diff. Supports: current, latest, oldest, previous.";
      properties.put("from_revision_id", fromRevisionIdProp);
      McpToolCallProperty toRevisionIdProp = new McpToolCallProperty();
      toRevisionIdProp.type = "string";
      toRevisionIdProp.description = "Target revision id for diff. Supports: current, latest, oldest, previous.";
      properties.put("to_revision_id", toRevisionIdProp);
      McpToolCallProperty fromHistoryLocationProp = new McpToolCallProperty();
      fromHistoryLocationProp.type = "string";
      fromHistoryLocationProp.description = "Absolute path to history file for the start revision.";
      properties.put("from_history_location", fromHistoryLocationProp);
      McpToolCallProperty toHistoryLocationProp = new McpToolCallProperty();
      toHistoryLocationProp.type = "string";
      toHistoryLocationProp.description = "Absolute path to history file for the target revision.";
      properties.put("to_history_location", toHistoryLocationProp);
      McpToolCallProperty fromIndexProp = new McpToolCallProperty();
      fromIndexProp.type = "integer";
      fromIndexProp.description = "Start revision index from LocalHistory response.";
      properties.put("from_index", fromIndexProp);
      McpToolCallProperty toIndexProp = new McpToolCallProperty();
      toIndexProp.type = "integer";
      toIndexProp.description = "Target revision index from LocalHistory response.";
      properties.put("to_index", toIndexProp);
      McpToolCallProperty contextLinesProp = new McpToolCallProperty();
      contextLinesProp.type = "integer";
      contextLinesProp.description = "Number of context lines to show around changes. Default: 3";
      properties.put("context_lines", contextLinesProp);
      McpToolCallProperty maxEntriesProp = new McpToolCallProperty();
      maxEntriesProp.type = "integer";
      maxEntriesProp.description = "Maximum number of history entries to search when using revision_id. Default: 20. Use 0 to search all entries.";
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
      @SerializedName("revision_id")
      public String revisionId;
      @SerializedName("history_location")
      public String historyLocation;
      @SerializedName("from_revision_id")
      public String fromRevisionId;
      @SerializedName("to_revision_id")
      public String toRevisionId;
      @SerializedName("from_history_location")
      public String fromHistoryLocation;
      @SerializedName("to_history_location")
      public String toHistoryLocation;
      @SerializedName("from_index")
      public Integer fromIndex;
      @SerializedName("to_index")
      public Integer toIndex;
      @SerializedName("context_lines")
      public Integer contextLines;
      @SerializedName("max_entries")
      public Integer maxEntries;
   }

   private static class LocalChangesResponse {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("file_path")
      public String filePath;
      @SerializedName("from_revision_id")
      public String fromRevisionId;
      @SerializedName("from_history_location")
      public String fromHistoryLocation;
      @SerializedName("to_revision_id")
      public String toRevisionId;
      @SerializedName("to_history_location")
      public String toHistoryLocation;
      @SerializedName("diff_text")
      public String diffText;
      @SerializedName("context_lines")
      public int contextLines;
      @SerializedName("has_changes")
      public boolean hasChanges;
   }

   private static class RevisionSelector {
      public String revisionId;
      public String historyLocation;
      public Integer index;
   }

   private static class ResolvedRevision {
      public final String revisionId;
      public final String historyLocation;
      private final Path path;
      private final byte[] content;

      private ResolvedRevision(Path path, byte[] content, String revisionId, String historyLocation) {
         this.path = path;
         this.content = content;
         this.revisionId = revisionId;
         this.historyLocation = historyLocation;
      }

      public static ResolvedRevision forPath(Path path, String revisionId, String historyLocation) {
         return new ResolvedRevision(path, (byte[])null, revisionId, historyLocation);
      }

      public static ResolvedRevision forContent(byte[] content, String revisionId, String historyLocation) {
         return new ResolvedRevision((Path)null, content, revisionId, historyLocation);
      }

      public byte[] getContent() {
         if (this.content != null) {
            return this.content;
         } else if (this.path == null) {
            throw new RuntimeException("Revision content is not available.");
         } else {
            try {
               if (!Files.exists(this.path, new LinkOption[0])) {
                  throw new RuntimeException("History entry does not exist at \"" + this.path + "\".");
               } else {
                  return Files.readAllBytes(this.path);
               }
            } catch (Exception e) {
               throw new RuntimeException("Failed to read revision content: " + e.getMessage(), e);
            }
         }
      }
   }

   private static class HistoryState {
      public String revisionId;
      public byte[] content;
   }
}
