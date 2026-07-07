package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;

class ContentProvider implements IContentProvider {
   private final IUI ui;

   @Inject
   public ContentProvider(IUI ui) {
      Preconditions.checkNotNull(ui);
      this.ui = ui;
   }

   public Content get(StyledText textWidget, int offset) {
      return (Content)this.ui.getSourceViewer(textWidget).map((sourceViewer) -> this.get(textWidget, sourceViewer, offset)).orElseGet(() -> new Content(textWidget.getText(), offset, "", 0));
   }

   private Content get(StyledText textWidget, SourceViewer sourceViewer, int offset) {
      String text = sourceViewer.getDocument().get();
      int widgetOffset = sourceViewer.widgetOffset2ModelOffset(offset);
      ISelection selection = sourceViewer.getSelection();
      if (!selection.isEmpty() && selection instanceof ITextSelection) {
         ITextSelection textSelection = (ITextSelection)selection;
         int selectionStart = textSelection.getOffset();
         int selectionFinish = textSelection.getOffset() + textSelection.getLength();
         String selectionText = text.substring(selectionStart, selectionFinish);
         return new Content(text, widgetOffset, selectionText, widgetOffset - selectionStart);
      } else {
         return new Content(text, widgetOffset, "", 0);
      }
   }
}
