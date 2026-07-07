package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILocalContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.assistent.model.LocalContext;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.egit.ui.internal.commit.DiffDocument;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class Chat implements IChat, IChatDialog, IStateListener {
   private static final String AI_CHAT_DIR = "ai.chat";
   private static final String INSTANCE_ID = UUID.randomUUID().toString();
   private static final AtomicBoolean CLEANUP_REGISTERED = new AtomicBoolean();
   private static final String AI_CHAT = "AI Chat";
   private static final String CHAT_API_WINK_TEMPLATE = "window.chatApi.wink({client_id: \"%s\", client_uid: \"%s\"}, \"%s\", \"%s\")";
   private static final String IDE_API = "ideApi";
   private static final String EMPTY_STRING = "``";
   private static final String NULL_VALUE = "null";
   private static final Character ARGS_SEPARATOR = ',';
   private static final String WINDOW_CHAT_API_SET_TOOLS = "window.chatApi.set_tools(";
   private static final String WINDOW = "window";
   private static final String TOPIC_INSERT_CODE = "insert_code";
   private final ILog log;
   private final ISettings settings;
   private final IUI ui;
   private final IDispatcher dispatcher;
   private final IdeApiHandler handler;
   private final IContextEntities contextEntities;
   private final IJavaScript javaScript;
   private final IStateService stateService;
   private final ISessionService sessionService;
   private final IModuleNameProvider moduleNameProvider;
   private final ILocalContext localContext;
   private final IProposalsProvider proposalsProvider;
   private final IJson json;
   private final IMcpTools mcpTools;
   private final IEdtLinkHandler linkHandler;
   private final IProjectTools projectTools;
   private final IContentSourceProvider contentSourceProvider;
   private final IFileSystem fileSystem;
   private final Cache<String, AIContext> contexts = CacheBuilder.newBuilder().maximumSize(256L).weakKeys().build();
   private final List<ChangeListener<Worker.State>> initializationListeners = new ArrayList();
   private final Object chatStateLock = new Object();
   private WebView webView;
   private ChatKey lastChatKey;
   private String lastDialogPath;
   private ChangeListener<Number> widthListener;
   private ChangeListener<Number> heightListener;
   private boolean isFirst = true;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$javafx$concurrent$Worker$State;

   @Inject
   public Chat(ILog log, ISettings settings, IUI ui, IDispatcher dispatcher, IdeApiHandler handler, IContextEntities contextEntities, IJavaScript javaScript, IStateService stateService, ISessionService sessionService, IModuleNameProvider moduleNameProvider, ILocalContext localContext, IProposalsProvider proposalsProvider, IJson json, IMcpTools mcpTools, IEdtLinkHandler linkHandler, IProjectTools projectTools, IContentSourceProvider contentSourceProvider, IFileSystem fileSystem) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(handler);
      Preconditions.checkNotNull(contextEntities);
      Preconditions.checkNotNull(javaScript);
      Preconditions.checkNotNull(stateService);
      Preconditions.checkNotNull(sessionService);
      Preconditions.checkNotNull(moduleNameProvider);
      Preconditions.checkNotNull(localContext);
      Preconditions.checkNotNull(proposalsProvider);
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(mcpTools);
      Preconditions.checkNotNull(linkHandler);
      Preconditions.checkNotNull(projectTools);
      Preconditions.checkNotNull(contentSourceProvider);
      Preconditions.checkNotNull(fileSystem);
      this.log = log;
      this.settings = settings;
      this.ui = ui;
      this.dispatcher = dispatcher;
      this.handler = handler;
      this.contextEntities = contextEntities;
      this.javaScript = javaScript;
      this.stateService = stateService;
      this.sessionService = sessionService;
      this.moduleNameProvider = moduleNameProvider;
      this.localContext = localContext;
      this.proposalsProvider = proposalsProvider;
      this.json = json;
      this.mcpTools = mcpTools;
      this.linkHandler = linkHandler;
      this.projectTools = projectTools;
      this.contentSourceProvider = contentSourceProvider;
      this.fileSystem = fileSystem;
      stateService.addListener(this);
   }

   public void reviewCode(AIContext ctx, String codeSnippet) {
      Preconditions.checkNotNull(codeSnippet);
      this.chat("review_code", codeSnippet, (String)null, ctx);
   }

   public void explainCode(AIContext ctx, String codeSnippet) {
      Preconditions.checkNotNull(codeSnippet);
      this.chat("comment_code", codeSnippet, (String)null, ctx);
   }

   public void fixCode(AIContext ctx, String codeSnippet, String details) {
      Preconditions.checkNotNull(codeSnippet);
      this.chat("fix_code", codeSnippet, details, ctx);
   }

   public void generateDocComments(AIContext ctx, String method) {
      Preconditions.checkNotNull(method);
      this.chat("document_code", method, (String)null, ctx);
   }

   public void askQuestion(AIContext ctx, String userQuestion) {
      Preconditions.checkNotNull(userQuestion);
      this.chat("plain_message", userQuestion, (String)null, ctx);
   }

   public void addCode(AIContext ctx, String codeSnippet) {
      Preconditions.checkNotNull(codeSnippet);
      this.chat("insert_code", codeSnippet, (String)null, ctx);
   }

   public void addToolsResult(String chatId, String messageId, McpCallToolsResult result) {
      Optional<AIContext> ctx = this.getContext(chatId);
      this.chatInJob(ctx, () -> {
         try {
            Throwable var4 = null;
            Object var5 = null;

            try {
               AutoCloseable busyToken = this.stateService.busy();

               try {
                  String messagesJson = result.messages != null ? this.json.serialize(result.messages) : null;
                  String unknownCallsJson = result.unknownCalls != null ? this.json.serialize(result.unknownCalls) : null;
                  this.dispatcher.dispatchAsync(() -> {
                     WebEngine webEngine = this.getEngine();
                     JSObject window = (JSObject)webEngine.executeScript("window");
                     if (window != null) {
                        window.setMember("calls_messages", messagesJson);
                        window.setMember("unknown_messages", unknownCallsJson);
                     }

                     String script = String.format("window.chatApi.add_tool_calls_result(%s, %s, window.calls_messages, window.unknown_messages);", this.javaScript.escape(chatId, "``"), this.javaScript.escape(messageId, "``"));
                     this.executeScriptWithLogging(script);
                  });
               } finally {
                  if (busyToken != null) {
                     busyToken.close();
                  }

               }
            } catch (Throwable var16) {
               if (var4 == null) {
                  var4 = var16;
               } else if (var4 != var16) {
                  var4.addSuppressed(var16);
               }

               throw var4;
            }
         } catch (Exception error) {
            this.log.logError(error);
         }

      });
   }

   public void continueChat(String chatId, String text) {
      Optional<AIContext> ctx = this.getContext(chatId);
      this.chatInJob(ctx, () -> {
         try {
            Throwable var3 = null;
            Object var4 = null;

            try {
               AutoCloseable busyToken = this.stateService.busy();

               try {
                  String script = String.format("window.chatApi.continue_chat(%s, %s);", this.javaScript.escape(text, "``"), this.javaScript.escape(chatId, "null"));
                  this.dispatcher.dispatchAsync(() -> this.executeScriptWithLogging(script));
               } finally {
                  if (busyToken != null) {
                     busyToken.close();
                  }

               }
            } catch (Throwable var14) {
               if (var3 == null) {
                  var3 = var14;
               } else if (var3 != var14) {
                  var3.addSuppressed(var14);
               }

               throw var3;
            }
         } catch (Exception error) {
            this.log.logError(error);
         }

      });
   }

   public void addFiles(List<IFileDocument> documents) {
      StringBuilder errorReadingFile = new StringBuilder();
      if (documents == null) {
         documents = this.openFilesAndCreateDocuments(errorReadingFile);
         if (documents == null || documents.isEmpty()) {
            this.showErrorIfAny(errorReadingFile);
            return;
         }
      }

      for(IFileDocument document : documents) {
         AIContext ctx = new AIContext(document.getProjectId(), document.getFile().getLocation().toPortableString(), (IDocument)null);
         this.chat("insert_code", document.getDocument().get(), (String)null, ctx);
      }

      this.showErrorIfAny(errorReadingFile);
   }

   private List<IFileDocument> openFilesAndCreateDocuments(StringBuilder errorReadingFile) {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      FileDialog dialog = new FileDialog(shell, 4098);
      dialog.setText(Messages.AddFilesToChatDialogName);
      dialog.setFilterPath(this.lastDialogPath);
      String file = dialog.open();
      if (file == null) {
         return null;
      } else {
         this.lastDialogPath = dialog.getFilterPath();
         ArrayList<IFileDocument> documents = new ArrayList();

         String[] var9;
         for(String fileName : var9 = dialog.getFileNames()) {
            Path filePath = this.lastDialogPath != null ? Path.of(this.lastDialogPath, fileName) : Path.of(fileName.toString());
            this.readFileContent(filePath, fileName, errorReadingFile);
         }

         return documents;
      }
   }

   private void readFileContent(Path filePath, String fileName, StringBuilder errorReadingFile) {
      String pathString = filePath.toString();

      try {
         String projectName = this.projectTools.determineProjectName(pathString);
         boolean isProjectFile = projectName != null && !projectName.isBlank();
         String content;
         AIContext ctx;
         if (isProjectFile) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject project = root.getProject(projectName);
            if (project == null || !project.exists()) {
               throw new IOException("Project does not exist or is not accessible");
            }

            Optional<IFile> optionalFile = this.projectTools.getProjectFile(project, pathString);
            if (!optionalFile.isPresent()) {
               throw new IOException("Cannot get IFile for project");
            }

            Optional<IFileDocument> optionalDocument = this.contentSourceProvider.getFileDocument((IFile)optionalFile.get());
            if (!optionalDocument.isPresent()) {
               throw new IOException("Cannot get document for project file");
            }

            IFileDocument document = (IFileDocument)optionalDocument.get();
            content = document.getDocument().get();
            ctx = new AIContext(new ProjectId(project), pathString, document.getDocument());
         } else {
            byte[] bytes = Files.readAllBytes(filePath);
            content = new String(bytes, StandardCharsets.UTF_8);
            ctx = new AIContext(ProjectId.Default, pathString, (IDocument)null);
         }

         if (!this.fileSystem.isPrintable(content, (double)90.0F)) {
            errorReadingFile.append(MessageFormat.format(Messages.FileNotTextFormat, fileName));
            errorReadingFile.append(System.lineSeparator());
            return;
         }

         this.chat("insert_code", content, (String)null, ctx);
      } catch (IOException var14) {
         errorReadingFile.append(MessageFormat.format(Messages.ErrorReadingFile, fileName));
         errorReadingFile.append(System.lineSeparator());
      }

   }

   private void showErrorIfAny(StringBuilder errorReadingFile) {
      if (errorReadingFile.length() > 0) {
         Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
         MessageDialog.openError(shell, Messages.ErrorReadingTextFile, errorReadingFile.toString());
      }

   }

   private void chat(String topic, String subject, String details, AIContext ctx) {
      this.ui.showView("com.e1c.edt.ai.ui.views.ChatView");
      this.chatInJob(Optional.ofNullable(ctx), () -> {
         try {
            Throwable var5 = null;
            Object var6 = null;

            try {
               AutoCloseable busyToken = this.stateService.busy();

               try {
                  Optional<String> sessionId = this.getSessionId(ctx);
                  if (!sessionId.isEmpty()) {
                     ContextInfo contextInfo = this.buildContextInfo(ctx);
                     String script = this.buildChatScript(topic, subject, details, ctx, (String)sessionId.get(), contextInfo);
                     this.dispatcher.dispatchAsync(() -> {
                        Object executeScriptResult = this.executeScriptWithLogging(script);
                        if (executeScriptResult instanceof String) {
                           String chatId = (String)executeScriptResult;
                           this.contexts.put(chatId, ctx);
                        }

                     });
                     return;
                  }

                  this.log.warning("AI Chat", () -> "Cannot get session id");
               } finally {
                  if (busyToken != null) {
                     busyToken.close();
                  }

               }

            } catch (Throwable var18) {
               if (var5 == null) {
                  var5 = var18;
               } else if (var5 != var18) {
                  var5.addSuppressed(var18);
               }

               throw var5;
            }
         } catch (Exception error) {
            this.log.logError(error);
         }
      });
   }

   private Optional<String> getSessionId(AIContext ctx) {
      ProjectId projectId = (ProjectId)Optional.ofNullable(ctx).map(AIContext::getProjectId).orElse(ProjectId.Default);

      try {
         return ((Optional)this.sessionService.getSessionAsync(projectId).get()).map((i) -> i.sessionId);
      } catch (ExecutionException | InterruptedException var4) {
         return Optional.empty();
      }
   }

   private ContextInfo buildContextInfo(AIContext ctx) {
      ContextInfo info = new ContextInfo();
      if (ctx == null) {
         info.title = "null";
         return info;
      } else {
         info.title = (String)this.moduleNameProvider.getModuleName(ctx.getPath()).orElseGet(() -> ctx.getPath());
         ChatContext chatContext = new ChatContext();
         IDocument doc = ctx.getDocument();
         this.contextEntities.fill(ctx, chatContext, IStatistics.Empty, CancellationTokens.NONE);
         info.scriptLanguage = chatContext.scriptLanguage;
         info.programingLanguage = chatContext.programingLanguage;
         if (doc instanceof DiffDocument) {
            info.programingLanguage = "git diff";
         }

         return info;
      }
   }

   private String buildChatScript(String topic, String subject, String details, AIContext ctx, String sessionId, ContextInfo contextInfo) {
      StringBuilder script = new StringBuilder();
      script.append("window.chatApi.");
      script.append(topic);
      script.append('(');
      script.append(this.javaScript.escape(subject, "``"));
      script.append(ARGS_SEPARATOR);
      script.append(this.javaScript.escape(contextInfo.scriptLanguage, "``"));
      script.append(ARGS_SEPARATOR);
      script.append(this.javaScript.escape(contextInfo.programingLanguage, "``"));
      if (details != null) {
         script.append(ARGS_SEPARATOR);
         script.append(this.javaScript.escape(details, "``"));
      }

      script.append(ARGS_SEPARATOR);
      String path = (String)Optional.ofNullable(ctx).map(AIContext::getPath).orElse((Object)null);
      if ("insert_code".equals(topic)) {
         path = this.linkHandler.getFullPathForInsertCode(ctx);
         path = this.linkHandler.formatInsertCodePath(ctx, path);
      }

      script.append(this.javaScript.escape(path, "null"));
      if (topic.equals("insert_code")) {
         this.appendLineNumbers(script, ctx);
      }

      script.append(ARGS_SEPARATOR);
      script.append(this.javaScript.escape(sessionId, "null"));
      script.append(ARGS_SEPARATOR);
      script.append(this.javaScript.escape(contextInfo.title, "null"));
      String contextJson = this.json.serialize(this.buildContext(ctx));
      script.append(ARGS_SEPARATOR);
      script.append(this.javaScript.escape(contextJson, "null"));
      script.append(");");
      return script.toString();
   }

   private LocalContext buildContext(AIContext ctx) {
      LocalContext context = this.localContext.create(ctx, IStatistics.Empty, CancellationTokens.NONE);
      Optional<SourceViewer> sourceViewer = this.ui.getLastSourceViewer();
      if (sourceViewer.isPresent()) {
         context.proposals = (List)this.proposalsProvider.getProposals(ctx, (SourceViewer)sourceViewer.get(), 600, CancellationTokens.NONE).orElse((Object)null);
      }

      return context;
   }

   private void appendLineNumbers(StringBuilder script, AIContext ctx) {
      IDocument document = (IDocument)Optional.ofNullable(ctx).map(AIContext::getDocument).orElse((Object)null);
      Integer startLine = null;
      Integer endLine = null;
      if (document != null) {
         try {
            startLine = document.getLineOfOffset(ctx.getStart());
            endLine = document.getLineOfOffset(ctx.getFinish());
         } catch (BadLocationException error) {
            this.log.logError(error);
         }
      }

      script.append(ARGS_SEPARATOR);
      script.append(startLine != null ? startLine.toString() : "null");
      script.append(ARGS_SEPARATOR);
      script.append(endLine != null ? endLine.toString() : "null");
   }

   private Object executeScriptWithLogging(String script) {
      this.log.trace("chat", "AI Chat", () -> "executing script: " + script);
      Object executeScriptResult = this.getEngine().executeScript(script);
      this.log.trace("chat", "AI Chat", () -> "script executed: " + executeScriptResult);
      return executeScriptResult;
   }

   public void show(ScrollPane pane) {
      this.ensureWebViewExists();
      pane.setContent(this.webView);
      this.webView.setFocusTraversable(true);
      this.webView.setPrefWidth(pane.getWidth());
      this.webView.setPrefHeight(pane.getHeight());
      this.widthListener = (observable, oldValue, newValue) -> this.webView.setPrefWidth((Double)newValue);
      this.heightListener = (observable, oldValue, newValue) -> this.webView.setPrefHeight((Double)newValue);
      pane.widthProperty().addListener(this.widthListener);
      pane.heightProperty().addListener(this.heightListener);
      this.warmUp();
   }

   private void ensureWebViewExists() {
      if (this.webView == null) {
         WebView view = new WebView();
         this.webView = view;
         view.setLayoutX((double)-1.0F);
         view.setLayoutY((double)-1.0F);
         WebEngine webEngine = this.getEngine();
         Path userDataDirectory = getUserDataDirectory();

         try {
            Files.createDirectories(userDataDirectory);
         } catch (IOException error) {
            this.log.logError(error);
         }

         webEngine.setUserDataDirectory(userDataDirectory.toFile());
         registerCleanupHook(userDataDirectory);
         webEngine.setOnError((event) -> {
            this.log.logError(event.getMessage());
            this.log.logError(event.getException());
         });
         view.setOnDragOver((event) -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
               event.acceptTransferModes(new TransferMode[]{TransferMode.COPY});
            }

            event.consume();
         });
         view.setOnDragDropped((event) -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
               StringBuilder errorReadingFile = new StringBuilder();

               for(File file : dragboard.getFiles()) {
                  this.readFileContent(file.toPath(), file.getName(), errorReadingFile);
               }

               this.showErrorIfAny(errorReadingFile);
               event.setDropCompleted(true);
            } else {
               event.setDropCompleted(false);
            }

            event.consume();
         });
         webEngine.loadContent(this.createLoadingPage());
         Worker<Void> worker = webEngine.getLoadWorker();
         worker.runningProperty().addListener((observable, oldValue, newValue) -> this.log.trace("chat", "AI Chat", () -> "is running: " + newValue));
      }
   }

   private String createLoadingPage() {
      String theme = this.settings.getTheme();
      boolean isDark = "dark".equalsIgnoreCase(theme);
      String title = Messages.ChatLoadingTitle;
      String message = Messages.ChatLoadingMessage;
      String backgroundColor = isDark ? "#1e1e1e" : "#ffffff";
      String textColor = isDark ? "#cccccc" : "#333333";
      String spinnerColor = isDark ? "#4fc3f7" : "#0288d1";
      String spinnerBg = isDark ? "rgba(79, 195, 247, 0.2)" : "rgba(2, 136, 209, 0.2)";
      StringBuilder html = new StringBuilder();
      html.append("<!DOCTYPE html>");
      html.append("<html>");
      html.append("<head>");
      html.append("<meta charset=\"UTF-8\">");
      html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
      html.append("<style>");
      html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
      html.append("body {");
      html.append("    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;");
      html.append("    background-color: ").append(backgroundColor).append(";");
      html.append("    color: ").append(textColor).append(";");
      html.append("    display: flex;");
      html.append("    flex-direction: column;");
      html.append("    justify-content: center;");
      html.append("    align-items: center;");
      html.append("    min-height: 100vh;");
      html.append("    margin: 0;");
      html.append("}");
      html.append(".container {");
      html.append("    text-align: center;");
      html.append("    padding: 40px;");
      html.append("}");
      html.append(".spinner {");
      html.append("    width: 50px;");
      html.append("    height: 50px;");
      html.append("    border: 4px solid ").append(spinnerBg).append(";");
      html.append("    border-top-color: ").append(spinnerColor).append(";");
      html.append("    border-radius: 50%;");
      html.append("    animation: spin 1s linear infinite;");
      html.append("    margin: 0 auto 24px;");
      html.append("}");
      html.append("@keyframes spin {");
      html.append("    to { transform: rotate(360deg); }");
      html.append("}");
      html.append("h1 {");
      html.append("    font-size: 28px;");
      html.append("    font-weight: 600;");
      html.append("    margin-bottom: 12px;");
      html.append("    letter-spacing: -0.5px;");
      html.append("}");
      html.append(".message {");
      html.append("    font-size: 16px;");
      html.append("    opacity: 0.7;");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class=\"container\">");
      html.append("<div class=\"spinner\"></div>");
      html.append("<h1>").append(title).append("</h1>");
      html.append("<div class=\"message\">").append(message).append("</div>");
      html.append("</div>");
      html.append("</body>");
      html.append("</html>");
      return html.toString();
   }

   private static Path getUserDataDirectory() {
      return Path.of(ConfigurationScope.INSTANCE.getLocation().addTrailingSeparator().append("ai.chat").toFile().getAbsolutePath(), INSTANCE_ID);
   }

   private static void registerCleanupHook(Path userDataDirectory) {
      if (CLEANUP_REGISTERED.compareAndSet(false, true)) {
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
               Throwable var1 = null;
               Object var2 = null;

               try {
                  Stream<Path> stream = Files.walk(userDataDirectory);

                  try {
                     stream.sorted(Comparator.reverseOrder()).forEach((p) -> p.toFile().delete());
                  } finally {
                     if (stream != null) {
                        stream.close();
                     }

                  }
               } catch (Throwable var11) {
                  if (var1 == null) {
                     var1 = var11;
                  } else if (var1 != var11) {
                     var1.addSuppressed(var11);
                  }

                  throw var1;
               }
            } catch (RuntimeException | IOException var12) {
            }

         }, "ai-chat-userdata-cleanup"));
      }
   }

   private void chatInJob(Optional<AIContext> ctx, IChatAction chatAction) {
      this.ensureWebViewExists();
      Job job = this.dispatcher.createJob(Messages.ChatInteractionJobName, (jobCtx) -> this.chat(ctx, chatAction), false, CancellationTokens.NONE);
      job.setPriority(10);
      job.schedule();
   }

   private IStatus chat(Optional<AIContext> ctx, IChatAction chatAction) {
      try {
         URL chatUrl = this.settings.getChatUrl();
         ChatKey newChatKey = new ChatKey(chatUrl, this.settings.getClientToken());
         boolean needsInit;
         synchronized(this.chatStateLock) {
            needsInit = !Objects.equals(this.lastChatKey, newChatKey);
            if (needsInit) {
               this.handler.reset();
               this.lastChatKey = newChatKey;
            }
         }

         if (needsInit) {
            Optional<CompletableFuture<Boolean>> futureOpt = this.dispatcher.dispatch((Supplier)(() -> {
               WebEngine webEngine = this.getEngine();
               return this.initialize(webEngine, () -> webEngine.load(chatUrl.toString()));
            }));
            if (futureOpt.isPresent()) {
               try {
                  ((CompletableFuture)futureOpt.get()).get(this.settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS);
               } catch (TimeoutException var8) {
                  this.log.warning("AI Chat", () -> "Chat initialization timed out");
               } catch (InterruptedException var9) {
                  Thread.currentThread().interrupt();
                  this.log.warning("AI Chat", () -> "Chat initialization interrupted");
               } catch (ExecutionException error) {
                  this.log.warning("AI Chat", () -> "Chat initialization failed: " + error);
               }
            }

            this.wink(32);
         }

         chatAction.run();
      } catch (Throwable error) {
         return Status.warning("AI Chat: " + error.getMessage(), error);
      }

      return Status.OK_STATUS;
   }

   private CompletableFuture<Boolean> initialize(WebEngine webEngine, Runnable loader) {
      Worker<Void> worker = webEngine.getLoadWorker();
      this.log.trace("chat", "AI Chat", () -> "user agent: " + webEngine.getUserAgent());
      CompletableFuture<Boolean> result = new CompletableFuture();
      ChangeListener<Worker.State> stateListener = (observable, oldValue, newValue) -> {
         this.log.trace("chat", "AI Chat", () -> "new state: " + newValue);
         switch (newValue) {
            case SUCCEEDED:
            case CANCELLED:
            case FAILED:
               this.cleanupInitializationListeners(worker);
               result.complete(newValue == State.SUCCEEDED);
            default:
         }
      };
      synchronized(this.initializationListeners) {
         this.initializationListeners.add(stateListener);
         worker.stateProperty().addListener(stateListener);
      }

      loader.run();
      return result.orTimeout(this.settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS).exceptionally((error) -> {
         this.cleanupInitializationListeners(worker);
         this.log.trace("chat", "API error", () -> error.toString());
         return false;
      });
   }

   private void cleanupInitializationListeners(Worker<?> worker) {
      synchronized(this.initializationListeners) {
         this.initializationListeners.forEach((listener) -> worker.stateProperty().removeListener(listener));
         this.initializationListeners.clear();
      }
   }

   private void wink(int attempts) {
      if (!this.handler.isReady()) {
         List<?> tools;
         try {
            tools = (List)((List)this.mcpTools.getSpecifications().get(this.settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)).stream().map((i) -> i.function).collect(Collectors.toList());
         } catch (TimeoutException var6) {
            this.log.warning("AI Chat", () -> "MCP specifications timed out, skipping wink tools sync");
            return;
         } catch (InterruptedException var7) {
            Thread.currentThread().interrupt();
            this.log.warning("AI Chat", () -> "MCP specifications wait interrupted");
            return;
         } catch (ExecutionException error) {
            this.log.warning("AI Chat", () -> "MCP specifications failed: " + error);
            return;
         }

         String toolsJson = this.json.serialize(tools);
         WebEngine webEngine = this.getEngine();

         while(true) {
            this.executeWink(webEngine, toolsJson);
            if (this.handler.isReady() || attempts-- == 0) {
               break;
            }

            try {
               Thread.sleep(this.settings.getMinRequestDelay().toMillis());
            } catch (InterruptedException var9) {
               Thread.currentThread().interrupt();
               this.log.warning("AI Chat", () -> "Wink loop interrupted");
               break;
            }
         }

      }
   }

   private void executeWink(WebEngine webEngine, String toolsJson) {
      this.dispatcher.dispatch((Runnable)(() -> {
         try {
            JSObject window = (JSObject)webEngine.executeScript("window");
            if (window != null) {
               window.setMember("ideApi", this.handler);
               this.log.trace("chat", "AI Chat", () -> "set callback handler " + window.getMember("ideApi"));
               String winkScript = String.format("window.chatApi.wink({client_id: \"%s\", client_uid: \"%s\"}, \"%s\", \"%s\")", this.settings.getClientToken(), this.settings.getClientUniqueId(), this.settings.getLanguage(), this.settings.getTheme());
               this.log.trace("chat", "AI Chat", () -> "wink script: " + winkScript);
               Object winkResult = webEngine.executeScript(winkScript);
               this.log.trace("chat", "AI Chat", () -> "wink script executed, winked: " + this.handler.isReady() + ", result: " + winkResult);
               webEngine.executeScript("if (typeof window.chatApi['set_tools'] === 'function') { window.chatApi.set_tools(" + this.javaScript.escape(toolsJson, "``") + "); }");
            } else {
               this.log.warning("AI Chat", () -> "cannot find a chat window");
            }
         } catch (Throwable error) {
            this.handler.reset();
            this.log.logError(error);
         }

      }));
   }

   private WebEngine getEngine() {
      WebEngine webEngine = this.webView.getEngine();
      webEngine.setJavaScriptEnabled(true);
      return webEngine;
   }

   private Optional<AIContext> getContext(String chatId) {
      return Optional.ofNullable((AIContext)this.contexts.getIfPresent(chatId));
   }

   public void onServiceStateChange(ServiceState serviceState) {
      if (!this.isFirst && serviceState == ServiceState.SETTINGS_CHANGED) {
         this.isFirst = false;
         this.reset();
         this.warmUp();
      }

   }

   public void onActionStateChange(ActionState actionState) {
   }

   public void hide() {
      this.reset();
      this.warmUp();
   }

   private void warmUp() {
      this.chatInJob(Optional.empty(), () -> {
      });
   }

   private void reset() {
      this.contexts.invalidateAll();
      this.lastChatKey = null;
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$javafx$concurrent$Worker$State() {
      int[] var10000 = $SWITCH_TABLE$javafx$concurrent$Worker$State;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[State.values().length];

         try {
            var0[State.CANCELLED.ordinal()] = 5;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[State.FAILED.ordinal()] = 6;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[State.READY.ordinal()] = 1;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[State.RUNNING.ordinal()] = 3;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[State.SCHEDULED.ordinal()] = 2;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[State.SUCCEEDED.ordinal()] = 4;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$javafx$concurrent$Worker$State = var0;
         return var0;
      }
   }

   private static class ContextInfo {
      String title;
      String scriptLanguage;
      String programingLanguage;
   }

   private static class ChatKey {
      private final URL url;
      private final String token;

      public ChatKey(URL url, String token) {
         this.url = url;
         this.token = token;
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.token, this.url});
      }

      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            ChatKey other = (ChatKey)obj;
            return Objects.equals(this.token, other.token) && Objects.equals(this.url, other.url);
         }
      }
   }

   private interface IChatAction {
      void run();
   }
}
