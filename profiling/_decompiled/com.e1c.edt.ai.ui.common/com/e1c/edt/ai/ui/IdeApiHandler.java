package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.assistent.ITextPreprocessor;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.widgets.Shell;

public class IdeApiHandler {
   private static final String AI_CHAT = "AI Chat";
   private final ILog log;
   private final IUI ui;
   private final IDispatcher dispatcher;
   private final ITextPreprocessor textPreprocessor;
   private final Provider<IChat> chatProvider;
   private final IJson json;
   private final IMcpTools mcpTools;
   private final IEdtLinkHandler linkHandler;
   private final IEditorPositionManager editorPositionManager;
   private final IMarkdownUtils markdownUtils;
   private final IWeb web;
   private boolean isReady;

   @Inject
   public IdeApiHandler(ILog log, IUI ui, IDispatcher dispatcher, ITextPreprocessor textPreprocessor, Provider<IChat> chatProvider, IJson json, IMcpTools mcpTools, IEdtLinkHandler linkHandler, IEditorPositionManager editorPositionManager, IMarkdownUtils markdownUtils, IWeb web) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(textPreprocessor);
      Preconditions.checkNotNull(chatProvider);
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(mcpTools);
      Preconditions.checkNotNull(linkHandler);
      Preconditions.checkNotNull(editorPositionManager);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(web);
      this.log = log;
      this.ui = ui;
      this.dispatcher = dispatcher;
      this.textPreprocessor = textPreprocessor;
      this.chatProvider = chatProvider;
      this.json = json;
      this.mcpTools = mcpTools;
      this.linkHandler = linkHandler;
      this.editorPositionManager = editorPositionManager;
      this.markdownUtils = markdownUtils;
      this.web = web;
   }

   public void wink(String parameter) {
      Preconditions.checkNotNull(parameter);
      this.isReady = true;
      this.log.trace("chat", "AI Chat", () -> "winked: " + parameter);
   }

   public void paste_code(String code) {
      if (code != null) {
         String processedCode = this.textPreprocessor.process(code);
         this.ui.getLastSourceViewer().ifPresent((sourceViewer) -> {
            ISelection selection = sourceViewer.getSelection();
            StyledText textWidget = sourceViewer.getTextWidget();
            StyledTextContent content = textWidget.getContent();
            if (selection instanceof TextSelection) {
               TextSelection textSelection = (TextSelection)selection;
               if (textSelection.getLength() > 0) {
                  Optional<Shell> shellOptional = this.ui.getShell();
                  if (shellOptional.isPresent() && !MessageDialog.openQuestion((Shell)shellOptional.get(), Messages.AIName, Messages.ReplaceCode)) {
                     return;
                  }
               }

               content.replaceTextRange(sourceViewer.modelOffset2WidgetOffset(textSelection.getOffset()), textSelection.getLength(), processedCode);
            } else {
               content.replaceTextRange(textWidget.getCaretOffset(), 0, processedCode);
            }
         });
      }
   }

   public void callTools(String chatId, String messageId, String callToolsJson) {
      Job job = this.dispatcher.createJob(Messages.ChatInteractionJobName, (jobCtx) -> {
         Optional<McpToolCalls> callToolsOptional = this.json.deserialize(callToolsJson, McpToolCalls.class);
         if (callToolsOptional.isEmpty()) {
            this.log.logError("Cannot deserialize calls: " + callToolsJson);
         } else {
            McpToolCalls calls = (McpToolCalls)callToolsOptional.get();

            for(McpToolCall call : calls) {
               call.sourceChatId = chatId;
               call.sourceMessageId = messageId;
               call.callKind = ToolCallKind.CALL;
            }

            this.mcpTools.callTools(calls, CancellationTokens.NONE).whenComplete((result, error) -> {
               if (error != null) {
                  this.log.logError(error);
               } else {
                  IChat chat = (IChat)this.chatProvider.get();
                  chat.addToolsResult(chatId, messageId, result);
               }
            });
         }
      }, true, CancellationTokens.NONE);
      job.setPriority(10);
      job.schedule();
   }

   public String renderTools(String chatId, String messageId, String callToolsJson) {
      Optional<McpToolCalls> callToolsOptional = this.json.deserialize(callToolsJson, McpToolCalls.class);
      if (callToolsOptional.isEmpty()) {
         this.log.logError("Cannot deserialize calls: " + callToolsJson);
         return null;
      } else {
         McpToolCalls calls = (McpToolCalls)callToolsOptional.get();

         for(McpToolCall call : calls) {
            call.sourceChatId = chatId;
            call.sourceMessageId = messageId;
            call.callKind = ToolCallKind.RENDER;
         }

         try {
            McpCallToolsResult result = (McpCallToolsResult)this.mcpTools.callTools(calls, CancellationTokens.NONE).get();
            String messagesJson = result.messages != null ? this.json.serialize(result.messages) : null;
            return messagesJson;
         } catch (ExecutionException | InterruptedException error) {
            this.log.logError(error);
            return null;
         }
      }
   }

   public void trace(String message) {
      this.log.trace("chat", "AI Chat", () -> message);
   }

   public boolean link(String title, String href) {
      String safeHref = href != null ? href.trim() : "";
      if (safeHref.startsWith("/")) {
         return false;
      } else {
         String decodedHref = this.markdownUtils.decodeUrl(safeHref);
         String processedHref = escapeColonsInPath(decodedHref);
         if (!this.linkHandler.isRecognizedHref(processedHref)) {
            this.web.browse(decodedHref);
            return true;
         } else {
            String safeTitle = title != null ? title.trim() : "";
            if (safeTitle.isEmpty() && processedHref.isEmpty()) {
               return false;
            } else {
               String filePath = this.linkHandler.extractFilePath(processedHref);
               if (filePath.isEmpty()) {
                  return false;
               } else {
                  this.dispatcher.dispatchAsync(() -> {
                     IEdtLinkHandler.SelectionInfo selection = (IEdtLinkHandler.SelectionInfo)this.linkHandler.extractSelection(processedHref).orElse((Object)null);
                     IEdtLinkHandler.CursorPositionInfo cursorPosition = (IEdtLinkHandler.CursorPositionInfo)this.linkHandler.extractCursorPosition(processedHref).orElse((Object)null);
                     this.editorPositionManager.openFileInEditor(filePath, cursorPosition, selection);
                  });
                  return true;
               }
            }
         }
      }
   }

   private static String escapeColonsInPath(String href) {
      if (href != null && !href.isEmpty()) {
         int protocolEnd = href.indexOf("://");
         if (protocolEnd < 0) {
            return href;
         } else {
            String pathPart = href.substring(protocolEnd + 3);
            int firstColon = pathPart.indexOf(58);
            if (firstColon > 0 && firstColon < 3) {
               pathPart = pathPart.substring(0, firstColon) + "%3A" + pathPart.substring(firstColon + 1);
            }

            return href.substring(0, protocolEnd + 3) + pathPart;
         }
      } else {
         return href;
      }
   }

   public boolean isReady() {
      return this.isReady;
   }

   public void reset() {
      this.isReady = false;
   }
}
