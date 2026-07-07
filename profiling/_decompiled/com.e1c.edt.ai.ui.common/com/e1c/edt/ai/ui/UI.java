package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

class UI implements IInitializable, IUI, Listener {
   private final ILog log;
   private final ICodeCompletionViewModel<CodeCompletionContext> codeCompletionViewModel;
   private final IDispatcher dispatcher;
   private final ITextWidgetInfoUpdater textWidgetInfoUpdater;
   private StyledText textWidget;
   private SourceViewer lastSourceViewer;
   private AutoCloseable queryToken;

   @Inject
   public UI(ILog log, ICodeCompletionViewModel<CodeCompletionContext> codeCompletionViewModel, IDispatcher dispatcher, ITextWidgetInfoUpdater textWidgetInfoUpdater) {
      this.queryToken = Closeables.Empty;
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(codeCompletionViewModel);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(textWidgetInfoUpdater);
      this.log = log;
      this.codeCompletionViewModel = codeCompletionViewModel;
      this.dispatcher = dispatcher;
      this.textWidgetInfoUpdater = textWidgetInfoUpdater;
   }

   public void initialize() {
      this.dispatcher.dispatchAsync(() -> {
         Display display = Display.getDefault();
         display.addFilter(15, this);
         display.addFilter(16, this);
         Control curControl = display.getFocusControl();
         if (curControl instanceof Widget) {
            Event initEvent = new Event();
            initEvent.type = 15;
            initEvent.widget = curControl;
            this.handleEvent(initEvent);
         }

      });
   }

   public Optional<Shell> getShell() {
      return Optional.ofNullable(Display.getCurrent()).map((dysplay) -> dysplay.getActiveShell());
   }

   public synchronized void handleEvent(Event event) {
      Preconditions.checkNotNull(event);
      if (event.type == 15 && event.widget != this.textWidget && event.widget instanceof StyledText) {
         StyledText newTextWidget = (StyledText)event.widget;
         if (this.isValidWidget(newTextWidget)) {
            try {
               this.queryToken.close();
            } catch (Exception var4) {
            }

            this.textWidget = newTextWidget;
            this.lastSourceViewer = (SourceViewer)this.getSourceViewer(newTextWidget).orElse((Object)null);
            this.textWidgetInfoUpdater.reset();
            this.queryToken = Closeables.Empty;
            this.dispatcher.dispatchAsync(() -> this.queryToken = this.codeCompletionViewModel.activate(newTextWidget));
         } else {
            this.textWidget = null;
         }
      }

   }

   public synchronized Optional<StyledText> getTextWidget() {
      StyledText widget = this.textWidget;
      return !this.isValidWidget(widget) ? Optional.empty() : Optional.of(widget);
   }

   public synchronized Optional<SourceViewer> getLastSourceViewer() {
      return Optional.ofNullable(this.lastSourceViewer);
   }

   public Optional<SourceViewer> getSourceViewer(StyledText textWidget) {
      Preconditions.checkNotNull(textWidget);
      if (textWidget.isDisposed()) {
         return Optional.empty();
      } else {
         IWorkbenchWindow[] var5;
         for(IWorkbenchWindow workbench : var5 = PlatformUI.getWorkbench().getWorkbenchWindows()) {
            IWorkbenchPage[] var9;
            for(IWorkbenchPage page : var9 = workbench.getPages()) {
               IEditorReference[] var13;
               for(IEditorReference editorRef : var13 = page.getEditorReferences()) {
                  IEditorPart editor = editorRef.getEditor(false);
                  if (editor != null) {
                     ITextOperationTarget curSourceViewer = (ITextOperationTarget)editor.getAdapter(ITextOperationTarget.class);
                     if (curSourceViewer instanceof SourceViewer) {
                        SourceViewer surceViewer = (SourceViewer)curSourceViewer;
                        if (surceViewer.getTextWidget() == textWidget) {
                           return Optional.of(surceViewer);
                        }
                     }
                  }
               }
            }
         }

         return Optional.empty();
      }
   }

   public Optional<IFile> getFile(SourceViewer sourceViewer) {
      IWorkbenchWindow[] var5;
      for(IWorkbenchWindow workbench : var5 = PlatformUI.getWorkbench().getWorkbenchWindows()) {
         IWorkbenchPage[] var9;
         for(IWorkbenchPage page : var9 = workbench.getPages()) {
            IEditorReference[] var13;
            for(IEditorReference editorRef : var13 = page.getEditorReferences()) {
               IEditorPart editor = editorRef.getEditor(false);
               if (editor != null) {
                  ITextOperationTarget curSourceViewer = (ITextOperationTarget)editor.getAdapter(ITextOperationTarget.class);
                  if (sourceViewer == curSourceViewer) {
                     Optional<IFile> file = Optional.ofNullable(editor.getEditorInput()).map((input) -> (IFile)input.getAdapter(IFile.class));
                     if (file.isPresent()) {
                        return file;
                     }
                  }
               }
            }
         }
      }

      return Optional.empty();
   }

   public Optional<IViewPart> showView(String viewId) {
      Preconditions.checkNotNull(viewId);
      return this.getActivePage().map((activePage) -> {
         try {
            return activePage.showView(viewId);
         } catch (PartInitException e) {
            this.log.logError(e);
            return null;
         }
      });
   }

   private boolean isValidWidget(StyledText widget) {
      return widget != null && !widget.isDisposed() && widget.isEnabled() && widget.getVisible() && this.getSourceViewer(widget).isPresent();
   }

   private Optional<IWorkbenchPage> getActivePage() {
      return Optional.ofNullable(PlatformUI.getWorkbench()).map((workbench) -> workbench.getActiveWorkbenchWindow()).map((window) -> window.getActivePage());
   }
}
