package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICodeCompletionContext;
import com.e1c.edt.ai.Text;
import com.google.common.base.Preconditions;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;

class CodeCompletionContext implements ICodeCompletionContext {
   private final ICodeCompletionContext baseContext;
   private final AIContext aiContext;
   private final StyledText textWidget;
   private final CancellationTokenSource cancellationTokenSource;

   public CodeCompletionContext(ICodeCompletionContext baseContext, AIContext aiContext, StyledText textWidget, CancellationTokenSource cancellationTokenSource) {
      Preconditions.checkNotNull(baseContext);
      Preconditions.checkNotNull(aiContext);
      Preconditions.checkNotNull(textWidget);
      Preconditions.checkNotNull(cancellationTokenSource);
      this.baseContext = baseContext;
      this.aiContext = aiContext;
      this.textWidget = textWidget;
      this.cancellationTokenSource = cancellationTokenSource;
   }

   public boolean isSingleWordMode() {
      int offset = this.textWidget.getCaretOffset();
      StyledTextContent contet = this.textWidget.getContent();
      int contentLength = contet.getCharCount();
      String prefix = contet.getTextRange(offset, contentLength - offset);

      for(int pos = 0; pos < prefix.length(); ++pos) {
         char ch = prefix.charAt(pos);
         if (!Character.isWhitespace(ch)) {
            return true;
         }

         if (ch == '\n') {
            return false;
         }
      }

      return false;
   }

   public void apply(Text text, int offset) {
      Preconditions.checkNotNull(text);
      this.replace(offset, 0, text.getText());
      this.baseContext.apply(text, offset);
   }

   public void rollback(int offset, int length) {
      this.replace(offset, length, "");
      this.baseContext.rollback(offset, length);
   }

   public void commit(String lastSourceId, int lastOffset) {
      this.baseContext.commit(lastSourceId, lastOffset);
   }

   public StyledText getWidget() {
      return this.textWidget;
   }

   public CancellationTokenSource getCancellationTokenSource() {
      return this.cancellationTokenSource;
   }

   public AIContext getAiContext() {
      return this.aiContext;
   }

   private void replace(int start, int replaceLength, String text) {
      StyledTextContent contet = this.textWidget.getContent();
      int contentLength = contet.getCharCount();
      if (start > contentLength) {
         start = contentLength;
      }

      contet.replaceTextRange(start, replaceLength, text);
      this.textWidget.setCaretOffset(start + text.length());
      this.textWidget.showSelection();
   }
}
