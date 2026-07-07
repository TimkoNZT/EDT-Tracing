package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.assistent.model.ClipboardInfo;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public class ClipboardManager implements IInitializable, IClipboard, IExecutionListener, Listener {
   private static final int MAX_SIZE = 8192;
   private static final Duration MAX_DURATION = Duration.ofMinutes(15L);
   private static final HashSet<String> COPY_COMMAND_IDS = new HashSet();
   private static final HashSet<String> PASTE_COMMAND_IDS = new HashSet();
   private final IDispatcher dispatcher;
   private final IClock clock;
   private Optional<String> text = Optional.empty();
   private Optional<IFile> file = Optional.empty();
   private Optional<IFile> currentFile = Optional.empty();
   private LocalDateTime expirationDate;
   private boolean isPasting;

   static {
      COPY_COMMAND_IDS.add("org.eclipse.ui.edit.copy");
      COPY_COMMAND_IDS.add("org.eclipse.xtend.ide.copyJavaCode");
      COPY_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.copy");
      COPY_COMMAND_IDS.add("org.eclipse.ui.edit.cut");
      COPY_COMMAND_IDS.add("org.eclipse.xtend.ide.cutJavaCode");
      COPY_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.cut");
      PASTE_COMMAND_IDS.add("org.eclipse.ui.edit.paste");
      PASTE_COMMAND_IDS.add("org.eclipse.xtend.ide.pasteJavaCode");
      PASTE_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.paste");
   }

   @Inject
   public ClipboardManager(IDispatcher dispatcher, IClock clock) {
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(clock);
      this.dispatcher = dispatcher;
      this.clock = clock;
   }

   public void initialize() {
      this.dispatcher.dispatchAsync(() -> {
         Display display = Display.getDefault();
         display.addFilter(15, this);
         display.addFilter(16, this);
         ICommandService commandService = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
         commandService.addExecutionListener(this);
      });
   }

   public Optional<ClipboardInfo> getClipboardInfo() {
      LocalDateTime expirationDate = this.expirationDate;
      if (expirationDate != null && this.clock.now().isAfter(expirationDate)) {
         return Optional.empty();
      } else {
         String currentText = (String)this.dispatcher.dispatch((Supplier)(() -> this.getTextFromClipoard())).flatMap((i) -> i).orElse((Object)null);
         String eclipseText = (String)this.text.orElse((Object)null);
         if (!Objects.equals(currentText, eclipseText)) {
            return Optional.empty();
         } else if (eclipseText != null && !eclipseText.isBlank()) {
            if (eclipseText.length() > 8192) {
               eclipseText = eclipseText.substring(0, 8192);
            }

            ClipboardInfo info = new ClipboardInfo();
            info.text = eclipseText;
            info.path = (String)this.file.map((i) -> i.getFullPath().makeRelative().toPortableString()).orElse((Object)null);
            return Optional.of(info);
         } else {
            return Optional.empty();
         }
      }
   }

   public boolean isPasting() {
      return this.isPasting;
   }

   public void preExecute(String commandId, ExecutionEvent event) {
      if (PASTE_COMMAND_IDS.contains(commandId)) {
         this.isPasting = true;
      }

   }

   public void postExecuteSuccess(String commandId, Object returnValue) {
      if (COPY_COMMAND_IDS.contains(commandId)) {
         this.file = this.currentFile;
         this.text = this.getTextFromClipoard();
         this.expirationDate = this.clock.now().plus(MAX_DURATION);
      }

      if (PASTE_COMMAND_IDS.contains(commandId)) {
         this.isPasting = false;
      }

   }

   public void notHandled(String commandId, NotHandledException exception) {
      if (PASTE_COMMAND_IDS.contains(commandId)) {
         this.isPasting = false;
      }

   }

   public void postExecuteFailure(String commandId, ExecutionException exception) {
      if (PASTE_COMMAND_IDS.contains(commandId)) {
         this.isPasting = false;
      }

   }

   private Optional<String> getTextFromClipoard() {
      Clipboard clipboard = new Clipboard(Display.getCurrent());

      try {
         Object val = clipboard.getContents(TextTransfer.getInstance());
         if (val instanceof String) {
            String text = (String)val;
            if (text != null && !text.isBlank()) {
               Optional var8 = Optional.of(text);
               return var8;
            }
         }

         Optional var5 = Optional.empty();
         return var5;
      } finally {
         clipboard.dispose();
      }
   }

   public void handleEvent(Event event) {
      if (event.type == 16) {
         this.currentFile = Optional.empty();
      } else {
         if (event.widget instanceof StyledText && event.type == 15) {
            this.currentFile = Optional.empty();

            IWorkbenchWindow[] var5;
            for(IWorkbenchWindow workbench : var5 = PlatformUI.getWorkbench().getWorkbenchWindows()) {
               IWorkbenchPage[] var9;
               for(IWorkbenchPage page : var9 = workbench.getPages()) {
                  IEditorReference[] var13;
                  for(IEditorReference editorRef : var13 = page.getEditorReferences()) {
                     IEditorPart editor = editorRef.getEditor(false);
                     if (editor != null) {
                        IEditorInput input = editor.getEditorInput();
                        if (input != null) {
                           ITextOperationTarget curSourceViewer = (ITextOperationTarget)editor.getAdapter(ITextOperationTarget.class);
                           if (curSourceViewer instanceof SourceViewer) {
                              SourceViewer surceViewer = (SourceViewer)curSourceViewer;
                              if (surceViewer.getTextWidget() != event.widget) {
                                 continue;
                              }
                           }

                           IFile editorFile = (IFile)input.getAdapter(IFile.class);
                           if (editorFile != null) {
                              this.currentFile = Optional.of(editorFile);
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }
}
