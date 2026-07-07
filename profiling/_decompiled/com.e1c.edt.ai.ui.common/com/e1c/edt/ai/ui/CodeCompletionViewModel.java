package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.CodeCompletionAction;
import com.e1c.edt.ai.CodeCompletionToken;
import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.Delimiters;
import com.e1c.edt.ai.HintPart;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ICodeCompletionActionHandler;
import com.e1c.edt.ai.ICodeCompletionContext;
import com.e1c.edt.ai.ICodeCompletionSession;
import com.e1c.edt.ai.ICodeCompletionStatistics;
import com.e1c.edt.ai.ICodeCompletionTokenizer;
import com.e1c.edt.ai.ICodeProvider;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.IHint;
import com.e1c.edt.ai.IHintHistory;
import com.e1c.edt.ai.IInputDelayStatistics;
import com.e1c.edt.ai.ILocalContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.Observers;
import com.e1c.edt.ai.Sources;
import com.e1c.edt.ai.StatisticsType;
import com.e1c.edt.ai.Text;
import com.e1c.edt.ai.assistent.ICodeAssistant;
import com.e1c.edt.ai.assistent.ICompletionRequestProvider;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Completion;
import com.e1c.edt.ai.assistent.model.CompletionRequest;
import com.e1c.edt.ai.assistent.model.Proposal;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;

class CodeCompletionViewModel implements ICodeCompletionViewModel<CodeCompletionContext>, VerifyKeyListener, CaretListener, TraverseListener, ModifyListener, SelectionListener, ControlListener, MouseListener, IDocumentListener {
   private final Object lockObject = new Object();
   private final ILog log;
   private final ISettings settings;
   private final ICodeAssistant codeAssistant;
   private final IAIContextProvider aiContextProvider;
   private final IDispatcher dispatcher;
   private final IHintPainter hintPainter;
   private final IVerticalRulerPainter verticalRulerPainter;
   private final IInputDelayStatistics inputRateStatistics;
   private final IClock clock;
   private final Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider;
   private final ICodeCompletionActionHandler<CodeCompletionContext> handler;
   private final IHintHistory history;
   private final IUserActions userActions;
   private final ICodeCompletionContext codeCompletionContext;
   private final Timer showTimer = new Timer(true);
   private final IUI ui;
   private final ICodeProvider codeProvider;
   private final ILocalContext localContext;
   private final IHotKeys hotKeys;
   private final IGlobalContextManager globalContextManager;
   private final ISyntaxVaidator syntaxVaidator;
   private final IProposalsProvider proposalsProvider;
   private final ICodeParser codeParser;
   private final ITextWidgetInfoUpdater textWidgetInfoUpdater;
   private final ICodeCompletionStatistics statistics;
   private final ICodeCompletionTokenizer tokenizer;
   private final IVerticalRulerManager rulerManager;
   private final IClipboard clipboard;
   private final ArrayList<CodeMethod> methods = new ArrayList();
   private ICodeCompletionSession<CodeCompletionContext> lastSession;
   private StyledText textWidget;
   private SourceViewer sourceViewer;
   private AutoCloseable feedbackToken;
   private AutoCloseable rulerManagerFreezeToken;
   private Job lastJob;
   private Job lastUpdateMethodJob;
   private Job commitJob;
   private List<Proposal> lastProposals;
   private Duration requestDuration;
   private boolean isTraversed;
   private AssistantListener assistantListener;
   private Optional<CodeMethod> prevMethod;
   private boolean isTextModifed;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$CodeCompletionAction;

   @Inject
   public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, ISettings settings, ICodeAssistant codeAssistant, IAIContextProvider aiContextProvider, IDispatcher dispatcher, IHintPainter hintPainter, IVerticalRulerPainter verticalRulerPainter, IInputDelayStatistics inputRateStatistics, IClock clock, Provider<ICodeCompletionSession<CodeCompletionContext>> sessionProvider, ICodeCompletionActionHandler<CodeCompletionContext> handler, IHintHistory history, IUserActions userActions, ICodeCompletionContext codeCompletionContext, IUI ui, ICodeProvider codeProvider, ILocalContext localContext, IHotKeys hotKeys, IGlobalContextManager globalContextManager, ISyntaxVaidator syntaxVaidator, IProposalsProvider proposalsProvider, ICodeParser codeParser, ITextWidgetInfoUpdater textWidgetInfoUpdater, ICodeCompletionStatistics statistics, ICodeCompletionTokenizer tokenizer, IVerticalRulerManager rulerManager, IClipboard clipboard) {
      this.feedbackToken = Closeables.Empty;
      this.rulerManagerFreezeToken = Closeables.Empty;
      this.lastProposals = new ArrayList();
      this.requestDuration = Duration.ZERO;
      this.assistantListener = new AssistantListener();
      this.prevMethod = Optional.empty();
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settingsStore);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(codeAssistant);
      Preconditions.checkNotNull(aiContextProvider);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(hintPainter);
      Preconditions.checkNotNull(verticalRulerPainter);
      Preconditions.checkNotNull(inputRateStatistics);
      Preconditions.checkNotNull(clock);
      Preconditions.checkNotNull(sessionProvider);
      Preconditions.checkNotNull(handler);
      Preconditions.checkNotNull(history);
      Preconditions.checkNotNull(userActions);
      Preconditions.checkNotNull(codeCompletionContext);
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(codeProvider);
      Preconditions.checkNotNull(localContext);
      Preconditions.checkNotNull(hotKeys);
      Preconditions.checkNotNull(globalContextManager);
      Preconditions.checkNotNull(syntaxVaidator);
      Preconditions.checkNotNull(proposalsProvider);
      Preconditions.checkNotNull(codeParser);
      Preconditions.checkNotNull(textWidgetInfoUpdater);
      Preconditions.checkNotNull(statistics);
      Preconditions.checkNotNull(tokenizer);
      Preconditions.checkNotNull(rulerManager);
      Preconditions.checkNotNull(clipboard);
      this.log = log;
      this.codeAssistant = codeAssistant;
      this.settings = settings;
      this.aiContextProvider = aiContextProvider;
      this.dispatcher = dispatcher;
      this.hintPainter = hintPainter;
      this.verticalRulerPainter = verticalRulerPainter;
      this.inputRateStatistics = inputRateStatistics;
      this.clock = clock;
      this.sessionProvider = sessionProvider;
      this.handler = handler;
      this.history = history;
      this.userActions = userActions;
      this.codeCompletionContext = codeCompletionContext;
      this.ui = ui;
      this.codeProvider = codeProvider;
      this.localContext = localContext;
      this.hotKeys = hotKeys;
      this.globalContextManager = globalContextManager;
      this.syntaxVaidator = syntaxVaidator;
      this.proposalsProvider = proposalsProvider;
      this.codeParser = codeParser;
      this.textWidgetInfoUpdater = textWidgetInfoUpdater;
      this.statistics = statistics;
      this.tokenizer = tokenizer;
      this.rulerManager = rulerManager;
      this.clipboard = clipboard;
   }

   public AutoCloseable activate(StyledText textWidget) {
      synchronized(this.methods) {
         this.methods.clear();
      }

      synchronized(this.lockObject) {
         this.reset();
         this.lastSession = null;
         this.isTextModifed = false;
         this.prevMethod = Optional.empty();
         this.textWidget = textWidget;
         if (!textWidget.isDisposed()) {
            this.sourceViewer = (SourceViewer)this.ui.getSourceViewer(textWidget).orElse((Object)null);
            if (this.sourceViewer != null) {
               this.addListeners(textWidget, this.sourceViewer);
               this.redraw();
               this.updateGlobalContext();
               this.warmup();
               AutoCloseable rulerManagerToken = this.rulerManager.activate(this.sourceViewer, () -> this.reset());
               AutoCloseable activationToken = Closeables.create(() -> this.deactivate(textWidget, this.sourceViewer));
               return Closeables.create(new AutoCloseable[]{rulerManagerToken, activationToken});
            }
         }

         return Closeables.Empty;
      }
   }

   private boolean isEnabled() {
      return this.settings.isEnabled();
   }

   private boolean isBalanced() {
      return CodeCompletionPolicy.MODERATE.isMeet(this.settings.getCodeCompletionPolicy());
   }

   private boolean isCreative() {
      return CodeCompletionPolicy.INTENSVE.isMeet(this.settings.getCodeCompletionPolicy());
   }

   private void reset() {
      this.cancel();
      Job localLastUpdateMethodJob;
      synchronized(this.lockObject) {
         localLastUpdateMethodJob = this.lastUpdateMethodJob;
         this.lastUpdateMethodJob = null;
      }

      if (localLastUpdateMethodJob != null) {
         localLastUpdateMethodJob.cancel();
      }

      this.lastProposals.clear();
      this.history.clear();
      this.hideHint();
   }

   private void hideHint() {
      this.dispatcher.dispatch((Runnable)(() -> {
         try {
            this.rulerManagerFreezeToken.close();
         } catch (Exception var2) {
         }

         this.hintPainter.reset();
         this.verticalRulerPainter.reset();
         this.rulerManager.reset(this.sourceViewer);
         this.redraw();
      }));
   }

   private void update(ICodeCompletionSession<CodeCompletionContext> session) {
      CodeCompletionContext content = (CodeCompletionContext)session.getContext();
      StyledText widget = content.getWidget();
      IHint hint = session.getHint();
      int offset = widget.getCaretOffset();
      this.hintPainter.pinOffset(this.textWidget, offset, true, ((CodeCompletionContext)session.getContext()).isSingleWordMode());
      this.hintPainter.setHintAt(hint.getText(HintPart.LINES).getText(), hint.getText(HintPart.TOKEN).getText(), hint.getAcceptedTokens());
      this.verticalRulerPainter.pin(this.textWidget, this.hintPainter.getDisplayedHintText());
      this.textWidget.showSelection();
      this.redraw();
   }

   private void askNew() {
      if (this.isEnabled()) {
         Duration delayBeforeShow = this.inputRateStatistics.registerAndPredictDelay();
         Duration delay = delayBeforeShow.minus(this.requestDuration);
         if (delay.toNanos() < this.settings.getMinRequestDelay().toNanos()) {
            delay = this.settings.getMinRequestDelay();
         }

         this.log.trace("code_completion", "Predicted hint delay " + delayBeforeShow.toMillis() + " ms, actual delay " + delay.toMillis() + " ms", () -> "");
         this.reset();
         this.askWithDelay(delay, delayBeforeShow, this.settings.getMinRequestDelay(), this.settings.getCodeCompletionLinesCount(), (CompletionRequestProvider)null, false, false);
      }
   }

   private void askWithDelay(Duration delayBeforeAsk, Duration delayBeforeShow, Duration maxDuration, int codeCompletionLinesCount, CompletionRequestProvider localContextProvider, boolean forced, boolean contentAssist) {
      if (this.isEnabled()) {
         this.cancel();
         synchronized(this.lockObject) {
            this.lastJob = this.dispatcher.createJob(Messages.CodeCompletionJobName, (jobCtx) -> this.getAiContext(jobCtx.CancellationTokenSource).ifPresent((aiCtx) -> {
                  LocalDateTime startTime = this.clock.now();
                  CompletionRequestProvider contextProvider = this.CreateContextProvider(localContextProvider, maxDuration, forced, contentAssist);
                  Duration newDelayBeforeShow = this.calculateDelay(startTime, delayBeforeShow);
                  this.ask(aiCtx, contextProvider, newDelayBeforeShow, codeCompletionLinesCount, jobCtx.CancellationTokenSource);
               }), false, CancellationTokens.NONE);
            this.lastJob.setSystem(true);
            this.lastJob.setPriority(10);
            this.lastJob.schedule(delayBeforeAsk.toMillis());
         }
      }
   }

   private void askWithoutDelay(boolean forced, boolean contentAssist) {
      if (this.isEnabled()) {
         this.askWithDelay(Duration.ZERO, Duration.ZERO, this.settings.getMinRequestDelay(), this.settings.getCodeCompletionLinesCount(), (CompletionRequestProvider)null, forced, contentAssist);
      }
   }

   private void warmup() {
      if (this.isEnabled()) {
         Job warmupJob = this.dispatcher.createJob(Messages.CodeCompletionJobName, (ct) -> this.CreateContextProvider((CompletionRequestProvider)null, this.settings.getTimeout(), false, false), false, CancellationTokens.NONE);
         warmupJob.setSystem(true);
         warmupJob.setPriority(50);
         warmupJob.schedule();
      }
   }

   private void updateGlobalContext() {
      this.dispatcher.dispatch((Supplier)(() -> this.aiContextProvider.create(this.sourceViewer, new AITarget(this.textWidget, true, false), CancellationTokens.NONE))).flatMap((i) -> i).ifPresent((aiCtx) -> this.globalContextManager.update(aiCtx, CancellationTokens.NONE));
   }

   private CompletionRequestProvider CreateContextProvider(CompletionRequestProvider localContextProvider, Duration maxDuration, boolean forced, boolean contentAssist) {
      return localContextProvider != null && localContextProvider.isForced() == forced && localContextProvider.isContentAssist() == contentAssist ? localContextProvider : new CompletionRequestProvider(maxDuration, forced, contentAssist);
   }

   private void deactivate(StyledText textWidget, SourceViewer sourceViewer) {
      synchronized(this.lockObject) {
         if (this.isTextModifed) {
            this.updateGlobalContext();
         }

         this.commit(this.lastSession);

         try {
            this.feedbackToken.close();
         } catch (Exception var5) {
         }

         this.methodChanged((CodeMethod)this.prevMethod.orElse((Object)null), (CodeMethod)null);
         this.reset();
         if (!textWidget.isDisposed()) {
            this.removeListeners(textWidget, sourceViewer);
            this.redraw();
         }

         this.lastSession = null;
         this.isTextModifed = false;
         this.prevMethod = Optional.empty();
      }
   }

   private void addListeners(StyledText textWidget, SourceViewer sourceViewer) {
      this.removeListeners(textWidget, sourceViewer);
      textWidget.addPaintListener(this.hintPainter);
      textWidget.addTraverseListener(this);
      textWidget.addCaretListener(this);
      textWidget.addVerifyKeyListener(this);
      textWidget.addModifyListener(this);
      textWidget.addControlListener(this);
      textWidget.addMouseListener(this);
      Optional.ofNullable(textWidget.getHorizontalBar()).ifPresent((scroll) -> scroll.addSelectionListener(this));
      Optional.ofNullable(textWidget.getVerticalBar()).ifPresent((scroll) -> scroll.addSelectionListener(this));
      Optional.ofNullable(sourceViewer.getContentAssistantFacade()).ifPresent((assistant) -> assistant.addCompletionListener(this.assistantListener));
      IDocument document = sourceViewer.getDocument();
      document.addDocumentListener(this);
   }

   private void removeListeners(StyledText textWidget, SourceViewer sourceViewer) {
      Optional.ofNullable(textWidget.getHorizontalBar()).ifPresent((scroll) -> scroll.removeSelectionListener(this));
      Optional.ofNullable(textWidget.getVerticalBar()).ifPresent((scroll) -> scroll.removeSelectionListener(this));
      Optional.ofNullable(sourceViewer.getContentAssistantFacade()).ifPresent((assistant) -> assistant.removeCompletionListener(this.assistantListener));
      textWidget.removePaintListener(this.hintPainter);
      textWidget.removeCaretListener(this);
      textWidget.removeVerifyKeyListener(this);
      textWidget.removeTraverseListener(this);
      textWidget.removeModifyListener(this);
      textWidget.removeMouseListener(this);
      IDocument document = sourceViewer.getDocument();
      document.removeDocumentListener(this);
   }

   private void ask(AIContext aiCtx, CompletionRequestProvider localContextProvider, Duration delayBeforeShow, int codeCompletionLinesCount, CancellationTokenSource cancellationTokenSource) {
      try {
         LocalDateTime startTime = this.clock.now();
         CodeCompletionContext codeCompletionCtx = new CodeCompletionContext(this.codeCompletionContext, aiCtx, this.textWidget, cancellationTokenSource);
         Boolean singleWordMode = (Boolean)this.dispatcher.dispatch((Supplier)(() -> codeCompletionCtx.isSingleWordMode())).orElse(false);
         ICodeCompletionSession<CodeCompletionContext> session = ((ICodeCompletionSession)this.sessionProvider.get()).initiaize(codeCompletionCtx, this.history, codeCompletionLinesCount, singleWordMode);
         synchronized(this.lockObject) {
            if (this.lastSession != null) {
               ((CodeCompletionContext)this.lastSession.getContext()).getCancellationTokenSource().cancel();
               this.lastSession.reset();
            }

            this.lastSession = session;
         }

         this.log.trace("code_completion", "AI context " + cancellationTokenSource, () -> aiCtx.toString());
         Duration delay = this.calculateDelay(startTime, delayBeforeShow);
         if (cancellationTokenSource.isCanceled()) {
            this.reset();
            return;
         }

         this.dispatcher.dispatch((Runnable)(() -> {
            this.hintPainter.reset();
            this.verticalRulerPainter.reset();
            this.hintPainter.pinOffset(this.textWidget, aiCtx.getCaretOffset(), this.isCreative() || delay.isNegative() || delay == Duration.ZERO, singleWordMode);
            this.hintPainter.setHintAt("", "", 0);
            this.textWidget.showSelection();
            this.redraw();
         }));
         this.getCurrentMethod(aiCtx.getTextOffset()).ifPresent((currentMethod) -> session.setMethod(currentMethod));
         IObservable<Completion> completionSource = this.codeAssistant.createSource(aiCtx.getProjectId(), localContextProvider, cancellationTokenSource);
         this.requestDuration = Duration.between(startTime, this.clock.now());
         ProcessingStatistics processingStatistics = new ProcessingStatistics();
         completionSource.subscribe(Observers.create((data) -> {
            if (!cancellationTokenSource.isCanceled()) {
               this.globalContextManager.update(aiCtx, data, cancellationTokenSource);
               String uuid = data.uuid;
               if (uuid != null && !uuid.isBlank()) {
                  session.setId(uuid);
               }

               IHint hint = session.getHint();
               String text = data.text;
               if (this.lastProposals.size() > 0) {
                  hint.append(new Text(((Proposal)this.lastProposals.get(0)).prefix, session));
                  this.lastProposals.clear();
               }

               hint.append(new Text(text, session));
               this.showWithDelay(session, this.calculateDelay(startTime, delayBeforeShow), processingStatistics);
               processingStatistics.totalDuration = processingStatistics.totalDuration.plus(Duration.between(data.startTime, this.clock.now()));
            }
         }, (error) -> {
            if (!cancellationTokenSource.isCanceled()) {
               this.log.logError(error);
               this.reset();
            }
         }, () -> {
            if (!cancellationTokenSource.isCanceled()) {
               IHint hint = session.getHint();
               if (this.lastProposals.size() > 0) {
                  hint.append(new Text(((Proposal)this.lastProposals.get(0)).prefix, Sources.UNKNOWN));
                  this.lastProposals.clear();
               }

               this.log.trace("code_completion", "AI generated text " + cancellationTokenSource, () -> {
                  StringBuilder message = new StringBuilder();
                  message.append(format(hint.toString()));
                  message.append(System.lineSeparator());
                  message.append("Total duration: ");
                  message.append(processingStatistics.totalDuration);
                  message.append(System.lineSeparator());
                  message.append("Syntax check duration: ");
                  message.append(processingStatistics.syntaxCheckDuration);
                  return message.toString();
               });
               if (!hint.isEmpty() && (this.isCreative() || !hint.isBlank())) {
                  this.showWithDelay(session, this.calculateDelay(startTime, delayBeforeShow), processingStatistics);
               } else {
                  this.hideHint();
               }

               session.complete();
            }
         }));
      } catch (CancellationException var14) {
      } catch (Exception e) {
         this.log.logError(e);
         this.reset();
      }

   }

   private Optional<CodeMethod> getCurrentMethod(int offset) {
      Optional<CodeMethod> result = this.getExistingMethod(offset);
      if (result.isPresent()) {
         return result;
      } else {
         Optional<CodeMethod> newMethod = this.codeParser.parse(this.sourceViewer).flatMap((parseResult) -> this.codeProvider.getMethod(parseResult, offset));
         if (newMethod.isPresent()) {
            synchronized(this.methods) {
               if (this.getExistingMethod(offset).isEmpty()) {
                  this.methods.add((CodeMethod)newMethod.get());
               }
            }
         }

         return newMethod;
      }
   }

   private Optional<CodeMethod> getExistingMethod(int offset) {
      synchronized(this.methods) {
         for(CodeMethod method : this.methods) {
            if (offset >= method.getStartOffest() && offset <= method.getEndOffest()) {
               return Optional.of(method);
            }
         }
      }

      return Optional.empty();
   }

   private void cancel() {
      this.showTimer.purge();
      Job localLastJob;
      ICodeCompletionSession<CodeCompletionContext> localLastSession;
      synchronized(this.lockObject) {
         localLastJob = this.lastJob;
         this.lastJob = null;
         this.lastUpdateMethodJob = null;
         localLastSession = this.lastSession;
         this.lastSession = null;
         this.isTraversed = false;
      }

      if (localLastJob != null) {
         localLastJob.cancel();
      }

      if (localLastSession != null) {
         ((CodeCompletionContext)localLastSession.getContext()).getCancellationTokenSource().cancel();
         localLastSession.reset();
      }

   }

   private void showWithDelay(final ICodeCompletionSession<CodeCompletionContext> session, Duration delayBeforeShow, final ProcessingStatistics processingStatistics) {
      this.showTimer.purge();
      if (!delayBeforeShow.isNegative() && delayBeforeShow != Duration.ZERO && !this.isCreative()) {
         this.showTimer.schedule(new TimerTask() {
            public void run() {
               CodeCompletionViewModel.this.show(session, processingStatistics);
            }
         }, delayBeforeShow.toMillis());
      } else {
         this.show(session, processingStatistics);
      }
   }

   private void show(ICodeCompletionSession<CodeCompletionContext> session, ProcessingStatistics processingStatistics) {
      if (this.isEnabled()) {
         if (!((CodeCompletionContext)session.getContext()).getCancellationTokenSource().isCanceled()) {
            CodeCompletionContext context = (CodeCompletionContext)session.getContext();
            IHint hint = session.getHint();
            if (!hint.isEmpty()) {
               if (this.isCreative() || !hint.isBlank()) {
                  String hintText = hint.getText(HintPart.LINES).getText();
                  LocalDateTime startTime = this.clock.now();
                  Optional<Code> optionalCode = this.dispatcher.dispatch((Supplier)(() -> new Code(this.textWidget.getText(), this.textWidget.getCaretOffset())));
                  if (!optionalCode.isEmpty()) {
                     Code code = (Code)optionalCode.get();
                     String validHint = (String)this.getCurrentMethod(code.offset).map((method) -> this.syntaxVaidator.getValidHint(method, code.code, code.offset, hintText, context.getCancellationTokenSource())).orElse(hintText);
                     processingStatistics.syntaxCheckDuration = processingStatistics.syntaxCheckDuration.plus(Duration.between(startTime, this.clock.now()));
                     if (validHint.length() > 0) {
                        CodeCompletionToken nextToken = this.tokenizer.getNext(1, validHint, Delimiters::isTokenDelimiter);
                        this.dispatcher.dispatch((Runnable)(() -> {
                           this.hintPainter.setHintAt(validHint, nextToken.getValue(), hint.getAcceptedTokens());
                           this.verticalRulerPainter.pin(this.textWidget, this.hintPainter.getDisplayedHintText());

                           try {
                              this.rulerManagerFreezeToken.close();
                           } catch (Exception var5) {
                           }

                           this.rulerManagerFreezeToken = this.rulerManager.freeze(this.sourceViewer);
                           this.redraw();
                        }));
                     } else if (session.isCompleted()) {
                        this.reset();
                     }

                  }
               }
            }
         }
      }
   }

   private Duration calculateDelay(LocalDateTime startTime, Duration delayBeforeShow) {
      return delayBeforeShow.minus(Duration.between(startTime, this.clock.now()));
   }

   private static String format(String text) {
      return "[" + text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "]";
   }

   public void verifyKey(VerifyEvent event) {
      if (this.isEnabled()) {
         ICodeCompletionSession<CodeCompletionContext> session;
         synchronized(this.lockObject) {
            session = this.lastSession;
         }

         CodeCompletionAction actionToProcess = this.userActions.getAction(event);
         boolean isContinuousCodeCompletion = this.isBalanced();
         CodeCompletionAction action = this.handler.handle(session, actionToProcess, event.character, this.hintPainter.getOffset(), isContinuousCodeCompletion);
         switch (action) {
            case SKIP:
               this.commit(session);
               break;
            case HANDLE:
               event.doit = false;
               break;
            case UPDATE:
               if (session != null) {
                  this.textWidget.setFocus();
                  this.update(session);
                  if (session.isDone() && !((CodeCompletionContext)session.getContext()).isSingleWordMode()) {
                     this.commit(session);
                     this.askWithoutDelay(false, false);
                  }

                  event.doit = false;
               }
               break;
            case RESET:
               this.reset();
               Boolean isSingleWordMode = (Boolean)this.dispatcher.dispatch((Supplier)(() -> ((CodeCompletionContext)session.getContext()).isSingleWordMode())).orElse(false);
               event.doit = !isSingleWordMode;
               break;
            case ASK_NEW:
               this.commit(session);
               if (!this.textWidget.isTextSelected()) {
                  this.askNew();
               }
               break;
            case SUGGEST:
               this.reset();
               this.askWithoutDelay(true, false);
               event.doit = false;
         }

         if (!event.doit) {
            this.isTraversed = false;
         }

         this.log.trace("code_completion", "AI action", () -> {
            StringBuilder message = new StringBuilder();
            message.append(actionToProcess.toString());
            message.append(" -> ");
            message.append(action);
            message.append(System.lineSeparator());
            message.append("handle: ");
            message.append(event.doit);
            message.append(System.lineSeparator());
            message.append("character code: ");
            message.append(event.character);
            message.append(System.lineSeparator());
            message.append("isContinuousCodeCompletion: ");
            message.append(isContinuousCodeCompletion);
            if (session != null) {
               message.append(System.lineSeparator());
               AIContext aiCtx = ((CodeCompletionContext)session.getContext()).getAiContext();
               message.append("offset: ");
               message.append(aiCtx.getCaretOffset());
            }

            return message.toString();
         });
      }
   }

   public void keyTraversed(TraverseEvent event) {
      synchronized(this.lockObject) {
         this.isTraversed = this.lastSession != null && this.hotKeys.isTriggered(event);
      }
   }

   public void caretMoved(CaretEvent event) {
      if (this.isEnabled()) {
         this.updateMethodAsync();
         synchronized(this.lockObject) {
            if (this.isTraversed || this.lastSession == null || this.lastSession.isAccepting()) {
               return;
            }
         }

         this.commit(this.lastSession);
         this.reset();
      }
   }

   private void updateMethodAsync() {
      if (this.isEnabled()) {
         Job localLastUpdateMethodJob;
         synchronized(this.lockObject) {
            localLastUpdateMethodJob = this.lastUpdateMethodJob;
            this.lastUpdateMethodJob = this.dispatcher.createJob(Messages.CodeCompletionJobName, (jobCtx) -> this.updateMethod(jobCtx.CancellationTokenSource), false, CancellationTokens.NONE);
            this.lastUpdateMethodJob.setSystem(true);
            this.lastUpdateMethodJob.setPriority(50);
            this.lastUpdateMethodJob.schedule(100L);
         }

         if (localLastUpdateMethodJob != null) {
            localLastUpdateMethodJob.cancel();
         }

      }
   }

   private void updateMethod(ICancellationToken cancellationToken) {
      if (this.isEnabled()) {
         Optional<Integer> offset = this.dispatcher.dispatch((Supplier)(() -> this.textWidget.getCaretOffset()));
         if (!offset.isEmpty() && !cancellationToken.isCanceled()) {
            Optional<CodeMethod> newMethod = this.getCurrentMethod((Integer)offset.get());
            if (!cancellationToken.isCanceled()) {
               String newMethodName = (String)newMethod.map((i) -> i.getUniqueName()).orElse("");
               String prevMethodName = (String)this.prevMethod.map((i) -> i.getUniqueName()).orElse("");

               try {
                  if (!newMethodName.equals(prevMethodName)) {
                     this.methodChanged((CodeMethod)this.prevMethod.orElse((Object)null), (CodeMethod)newMethod.orElse((Object)null));
                  }
               } finally {
                  this.prevMethod = newMethod;
               }

            }
         }
      }
   }

   public void modifyText(ModifyEvent e) {
      synchronized(this.lockObject) {
         this.methods.clear();
         this.isTextModifed = true;
      }
   }

   public void widgetSelected(SelectionEvent e) {
      if (this.hintPainter.getOffset() != -1) {
         this.redraw();
      }

   }

   public void widgetDefaultSelected(SelectionEvent e) {
      if (this.hintPainter.getOffset() != -1) {
         this.redraw();
      }

   }

   public void controlMoved(ControlEvent e) {
      if (this.hintPainter.getOffset() != -1) {
         this.redraw();
      }

   }

   private void redraw() {
      if (this.textWidget != null && !this.textWidget.isDisposed()) {
         this.textWidget.redraw();
         this.rulerManager.redraw(this.sourceViewer);
      }

   }

   public void controlResized(ControlEvent e) {
      if (this.hintPainter.getOffset() != -1) {
         this.redraw();
      }

   }

   public void mouseDoubleClick(MouseEvent e) {
   }

   public void mouseDown(MouseEvent e) {
      this.reset();
      int offset = this.textWidget.getOffsetAtPoint(new Point(e.x, e.y));
      if (offset < 0) {
         int line = this.textWidget.getLineIndex(e.y);
         if (line < this.textWidget.getLineCount() - 1) {
            offset = this.textWidget.getOffsetAtLine(line + 1) - 1;
         } else {
            offset = this.textWidget.getOffsetAtLine(line);
         }
      }

      this.textWidgetInfoUpdater.setLastMouseOffset(this.textWidget, offset);
   }

   public void mouseUp(MouseEvent e) {
   }

   public void documentAboutToBeChanged(DocumentEvent event) {
   }

   public void documentChanged(DocumentEvent event) {
      if (this.isEnabled()) {
         if (this.clipboard.isPasting()) {
            ICodeCompletionSession<CodeCompletionContext> session;
            synchronized(this.lockObject) {
               session = this.lastSession;
            }

            if (session != null) {
               this.commit(session);
            }

            this.log.trace("code_completion", "Clipboard paste", () -> '[' + event.fText + ']');
            this.dispatcher.dispatchAsync(() -> this.askNew());
         }

      }
   }

   private void methodChanged(CodeMethod prevMethod, CodeMethod newMethod) {
      this.log.trace("code_completion", "Method was changed", () -> {
         StringBuilder message = new StringBuilder();
         message.append("from: ");
         message.append(prevMethod != null ? prevMethod.getUniqueName() : "null");
         message.append(System.lineSeparator());
         message.append("to: ");
         message.append(newMethod != null ? newMethod.getUniqueName() : "null");
         return message.toString();
      });
      if (this.isTextModifed && newMethod != null) {
         this.isTextModifed = false;
         this.updateGlobalContext();
      }

      if (prevMethod != null) {
         this.statistics.addMethod(prevMethod, (Object)null, (i) -> (String)prevMethod.getParseResult().flatMap((parseResult) -> this.codeProvider.getMethodBody(parseResult, prevMethod)).orElse(""));
      }

   }

   private void commit(ICodeCompletionSession<CodeCompletionContext> session) {
      if (session != null) {
         if (this.isEnabled()) {
            if (this.commitJob != null) {
               this.commitJob.cancel();
            }

            Job job = this.dispatcher.createJob(Messages.CodeCompletionJobName, (jobCtx) -> ((CodeCompletionContext)session.getContext()).commit(session.getId(), ((CodeCompletionContext)session.getContext()).getAiContext().getTextOffset()), false, CancellationTokens.NONE);
            job.setSystem(true);
            job.setPriority(50);
            this.commitJob = job;
            job.schedule();
         }
      }
   }

   public Optional<AIContext> getAiContext(ICancellationToken cancellationToken) {
      return this.dispatcher.dispatch((Supplier)(() -> (AIContext)this.aiContextProvider.create(this.sourceViewer, new AITarget(this.textWidget, true, false), cancellationToken).orElse((Object)null)));
   }

   // $FF: synthetic method
   static IProposalsProvider access$0(CodeCompletionViewModel var0) {
      return var0.proposalsProvider;
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$CodeCompletionAction() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$CodeCompletionAction;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[CodeCompletionAction.values().length];

         try {
            var0[CodeCompletionAction.ACCEPT.ordinal()] = 9;
         } catch (NoSuchFieldError var13) {
         }

         try {
            var0[CodeCompletionAction.ACCEPT_CHAR.ordinal()] = 12;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[CodeCompletionAction.ACCEPT_LINE.ordinal()] = 11;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[CodeCompletionAction.ACCEPT_PART.ordinal()] = 10;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[CodeCompletionAction.ASK_NEW.ordinal()] = 5;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[CodeCompletionAction.FINISH.ordinal()] = 7;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[CodeCompletionAction.HANDLE.ordinal()] = 2;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[CodeCompletionAction.RESET.ordinal()] = 4;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[CodeCompletionAction.ROLLBACK_PART.ordinal()] = 8;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[CodeCompletionAction.SKIP.ordinal()] = 1;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[CodeCompletionAction.SUGGEST.ordinal()] = 6;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[CodeCompletionAction.TEST.ordinal()] = 13;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[CodeCompletionAction.UPDATE.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$CodeCompletionAction = var0;
         return var0;
      }
   }

   private class AssistantListener implements ICompletionListener, ICompletionListenerExtension2 {
      private CompletionRequestProvider localContext;
      private ICompletionProposal lastProp;

      public void applied(ICompletionProposal pro) {
         CodeCompletionViewModel.this.reset();
      }

      public void assistSessionStarted(ContentAssistEvent event) {
         CodeCompletionViewModel.this.reset();
         this.localContext = CodeCompletionViewModel.this.new CompletionRequestProvider(CodeCompletionViewModel.this.settings.getMinRequestDelay(), false, true);
      }

      public void assistSessionEnded(ContentAssistEvent event) {
         CodeCompletionViewModel.this.lastProposals.clear();
         this.localContext = null;
         this.lastProp = null;
      }

      public void selectionChanged(ICompletionProposal prop, boolean smartToggle) {
         if (CodeCompletionViewModel.this.isBalanced()) {
            if (this.lastProp != prop) {
               this.lastProp = prop;
               CodeCompletionViewModel.this.reset();
               Optional<Proposal> optionalProposal = CodeCompletionViewModel.this.getAiContext(CancellationTokens.NONE).flatMap((ctx) -> CodeCompletionViewModel.this.proposalsProvider.getProposal(prop, 0, ctx.getPrefix()));
               if (!optionalProposal.isEmpty()) {
                  CodeCompletionViewModel.this.lastProposals.add((Proposal)optionalProposal.get());
                  CodeCompletionViewModel.this.askWithDelay(Duration.ZERO, CodeCompletionViewModel.this.settings.getMinRequestDelay(), Duration.ZERO, 1, this.localContext, false, true);
               }
            }
         }
      }
   }

   private class CompletionRequestProvider implements ICompletionRequestProvider {
      private final Duration maxDuration;
      private final boolean forced;
      private final boolean contentAssist;
      private AIContext lastAiContext;
      private CompletionRequest lastRequest;
      private String originalPrefix;

      public CompletionRequestProvider(Duration maxDuration, boolean forced, boolean contentAssist) {
         Preconditions.checkNotNull(maxDuration);
         Preconditions.checkNotNull(forced);
         Preconditions.checkNotNull(contentAssist);
         this.maxDuration = maxDuration;
         this.forced = forced;
         this.contentAssist = contentAssist;
      }

      public synchronized Optional<CompletionRequest> get(IStatistics statistics, ICancellationToken cancellationToken) {
         CodeCompletionViewModel.this.dispatcher.checkThread(false, true);

         AIContext aiCtx;
         try {
            Throwable var4 = null;
            Object var5 = null;

            try {
               AutoCloseable measurement = statistics.measureDuration(StatisticsType.AI_CONTEXT_DURATUION);

               Optional var10000;
               try {
                  Optional<AIContext> optionalAiCtx = CodeCompletionViewModel.this.getAiContext(cancellationToken);
                  if (!optionalAiCtx.isEmpty()) {
                     aiCtx = (AIContext)optionalAiCtx.get();
                     if (this.lastRequest == null || this.lastAiContext == null || CodeCompletionViewModel.this.lastProposals.size() <= 0 || !this.lastAiContext.equals(aiCtx)) {
                        break label45;
                     }

                     this.lastRequest.localContext.prefix = this.originalPrefix + ((Proposal)CodeCompletionViewModel.this.lastProposals.get(0)).prefix;
                     var10000 = Optional.of(this.lastRequest);
                     return var10000;
                  }

                  var10000 = Optional.empty();
               } finally {
                  if (measurement != null) {
                     measurement.close();
                  }

               }

               return var10000;
            } catch (Throwable var15) {
               if (var4 == null) {
                  var4 = var15;
               } else if (var4 != var15) {
                  var4.addSuppressed(var15);
               }

               throw var4;
            }
         } catch (Exception error) {
            CodeCompletionViewModel.this.log.logError(error);
            return Optional.empty();
         }

         if (this.maxDuration != Duration.ZERO) {
            LocalDateTime expirationDate = CodeCompletionViewModel.this.clock.now().plus(this.maxDuration);
            cancellationToken = CancellationTokens.expiresAt(cancellationToken, CodeCompletionViewModel.this.clock, expirationDate);
         }

         this.lastAiContext = aiCtx;
         this.lastRequest = new CompletionRequest();
         this.lastRequest.localContext = CodeCompletionViewModel.this.localContext.create(aiCtx, statistics, cancellationToken);
         this.originalPrefix = this.lastRequest.localContext.prefix;
         if (CodeCompletionViewModel.this.lastProposals.size() > 0) {
            this.lastRequest.localContext.prefix = this.originalPrefix + ((Proposal)CodeCompletionViewModel.this.lastProposals.get(0)).prefix;
            this.lastRequest.localContext.proposals = CodeCompletionViewModel.this.lastProposals;
         } else {
            this.lastRequest.localContext.proposals = (List)CodeCompletionViewModel.this.proposalsProvider.getProposals(aiCtx, CodeCompletionViewModel.this.sourceViewer, 600, cancellationToken).orElseGet(() -> new ArrayList());
         }

         this.lastRequest.localContext.forced = this.isForced();
         this.lastRequest.localContext.contentAssist = this.isContentAssist();
         CodeCompletionViewModel.this.clipboard.getClipboardInfo().ifPresent((clipboardText) -> this.lastRequest.localContext.clipboard = clipboardText);
         return Optional.of(this.lastRequest);
      }

      public boolean isForced() {
         return this.forced;
      }

      public boolean isContentAssist() {
         return this.contentAssist;
      }
   }

   private static class ProcessingStatistics {
      public Duration totalDuration;
      public Duration syntaxCheckDuration;

      private ProcessingStatistics() {
         this.totalDuration = Duration.ZERO;
         this.syntaxCheckDuration = Duration.ZERO;
      }
   }

   private static class Code {
      public final String code;
      public final int offset;

      public Code(String code, int offset) {
         this.code = code;
         this.offset = offset;
      }
   }
}
