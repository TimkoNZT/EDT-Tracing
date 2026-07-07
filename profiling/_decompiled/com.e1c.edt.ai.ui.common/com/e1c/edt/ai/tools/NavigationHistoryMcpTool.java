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
import com.e1c.edt.ai.ui.IDispatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.INavigationHistory;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class NavigationHistoryMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "NavigationHistory";
   private static final int DEFAULT_MAX_ENTRIES = 20;
   private static String QuestionExample = "{\n  \"max_entries\": 20\n}";
   private static String AnswerExample = "{\n  \"entries\": [\n    {\n      \"index\": 5,\n      \"text\": \"Module.bsl\",\n      \"project_name\": \"MyProject\",\n      \"path\": \"C:\\\\\\\\Projects\\\\\\\\MyProject\\\\\\\\src\\\\\\\\CommonModules\\\\\\\\Module.bsl\",\n      \"is_current\": true\n    }\n  ],\n  \"has_more\": false\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IDispatcher dispatcher;
   private final IMarkdownUtils markdownUtils;

   @Inject
   public NavigationHistoryMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IDispatcher dispatcher, IMarkdownUtils markdownUtils) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(markdownUtils);
      this.json = json;
      this.messageFactory = messageFactory;
      this.dispatcher = dispatcher;
      this.markdownUtils = markdownUtils;
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
         int maxEntries = request.maxEntries != null && request.maxEntries > 0 ? request.maxEntries : 20;
         if (call.callKind == ToolCallKind.RENDER) {
            details.requestMarkdown = MessageFormat.format(Messages.NavigationHistoryTitleTemplate, maxEntries);
            return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
         } else {
            return CompletableFuture.supplyAsync(() -> {
               if (cancellationToken.isCanceled()) {
                  throw new ToolException("Operation was cancelled before execution.", (Throwable)null, ToolErrorType.RETRYABLE);
               } else {
                  List<NavigationEntry> entries = (List)this.dispatcher.dispatch((Supplier)(() -> this.collectHistory(maxEntries))).orElseGet(() -> new ArrayList());
                  NavigationHistoryResponse response = new NavigationHistoryResponse();
                  response.entries = entries;
                  response.hasMore = entries.size() == maxEntries;
                  String content = this.json.serialize(response);
                  StringBuilder responseMarkdown = new StringBuilder();
                  responseMarkdown.append(MessageFormat.format(Messages.NavigationHistoryFoundTemplate, this.markdownUtils.createStyledText(String.valueOf(entries.size()), TextColor.GREEN, FontWeight.BOLD, false)));
                  if (!entries.isEmpty()) {
                     responseMarkdown.append("\n\n<details><summary>").append(Messages.ViewNavigationHistory).append("</summary>\n\n");

                     for(NavigationEntry entry : entries) {
                        if (entry.absoluteFilePath != null) {
                           responseMarkdown.append("- ");
                           if (entry.text != null && !entry.text.isBlank()) {
                              responseMarkdown.append(this.markdownUtils.escapeForMarkdown(entry.text));
                           }

                           responseMarkdown.append(" ").append(this.markdownUtils.formatFilePath(entry.absoluteFilePath));
                           responseMarkdown.append("\n");
                        }
                     }

                     responseMarkdown.append("</details>");
                  } else {
                     responseMarkdown.append("\n\n").append(Messages.NoNavigationHistoryFound);
                  }

                  details.responseMarkdown = responseMarkdown.toString();
                  return this.messageFactory.createMessage(this, call, content, details);
               }
            }).exceptionally((throwable) -> {
               throw new ToolException(throwable.getMessage(), throwable, ToolErrorType.RETRYABLE);
            });
         }
      }
   }

   private List<NavigationEntry> collectHistory(int maxEntries) {
      IWorkbench workbench = PlatformUI.getWorkbench();
      if (workbench == null) {
         throw new RuntimeException("Workbench is not available.");
      } else {
         IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
         if (window == null) {
            throw new RuntimeException("Active workbench window is not available.");
         } else {
            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
               throw new RuntimeException("Active workbench page is not available.");
            } else {
               INavigationHistory history = page.getNavigationHistory();
               if (history == null) {
                  throw new RuntimeException("Navigation history is not available.");
               } else {
                  INavigationLocation current = history.getCurrentLocation();
                  INavigationLocation[] locations = history.getLocations();
                  if (locations != null && locations.length != 0) {
                     int total = locations.length;
                     int start = maxEntries > 0 ? Math.max(0, total - maxEntries) : 0;
                     ArrayList<NavigationEntry> entries = new ArrayList();

                     for(int i = start; i < total; ++i) {
                        INavigationLocation location = locations[i];
                        if (location != null) {
                           NavigationEntry entry = new NavigationEntry();
                           entry.index = i;
                           entry.text = location.getText();
                           entry.isCurrent = location == current;
                           Object input = location.getInput();
                           if (input instanceof IFileEditorInput) {
                              Optional.ofNullable(((IFileEditorInput)input).getFile()).ifPresent((filex) -> {
                                 entry.absoluteFilePath = filex.getLocation().toOSString();
                                 entry.projectName = filex.getProject().getName();
                              });
                           }

                           if (input instanceof IURIEditorInput) {
                              URI uri = ((IURIEditorInput)input).getURI();
                              if (uri != null) {
                                 File file = new File(uri);
                                 entry.absoluteFilePath = file.getAbsolutePath();
                                 entry.projectName = "The file \"" + entry.absoluteFilePath + "\" does not exist within the IDE project context.";
                              }
                           }

                           if (input instanceof IAdaptable) {
                              Optional.ofNullable((IFile)((IAdaptable)input).getAdapter(IFile.class)).ifPresent((filex) -> {
                                 entry.absoluteFilePath = filex.getLocation().toOSString();
                                 entry.projectName = filex.getProject().getName();
                              });
                           }

                           entries.add(entry);
                        }
                     }

                     if (!entries.isEmpty() && entries.stream().noneMatch((entryx) -> Boolean.TRUE.equals(entryx.isCurrent))) {
                        ((NavigationEntry)entries.get(entries.size() - 1)).isCurrent = true;
                     }

                     return entries;
                  } else {
                     return new ArrayList();
                  }
               }
            }
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "NavigationHistory";
      StringBuilder description = new StringBuilder();
      description.append("Lists IDE navigation history for the active workbench page.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Returns recent navigation locations in editor history.");
      description.append("\n- Each entry includes text plus file info when available.");
      description.append("\n- The last visited entry is marked as current.");
      description.append("\n- Response includes has_more flag indicating if more navigation entries are available.");
      description.append("\n\nRelated tools:");
      description.append("\n- Read file: `Read`.");
      description.append("\n- Open files in context: `GetProjects`.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty maxEntriesProp = new McpToolCallProperty();
      maxEntriesProp.type = "integer";
      maxEntriesProp.description = "Maximum number of history entries to return. Default: 20";
      properties.put("max_entries", maxEntriesProp);
      parameters.properties = properties;
      parameters.required = new ArrayList();
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("max_entries")
      public Integer maxEntries;
   }

   private static class NavigationEntry {
      @SerializedName("index")
      public Integer index;
      @SerializedName("text")
      public String text;
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("path")
      public String absoluteFilePath;
      @SerializedName("is_current")
      public Boolean isCurrent;
   }

   private static class NavigationHistoryResponse {
      @SerializedName("entries")
      public List<NavigationEntry> entries;
      @SerializedName("has_more")
      public boolean hasMore;
   }
}
