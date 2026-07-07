package com.e1c.edt.ai.ui;

import java.util.Optional;
import org.eclipse.swt.custom.StyledText;

class TextWidgetInfo implements ITextWidgetInfoUpdater, ITextWidgetInfoProvider {
   private StyledText textWidget;
   private int lastMouseOffset;

   public void setLastMouseOffset(StyledText textWidget, int offset) {
      this.textWidget = textWidget;
      this.lastMouseOffset = offset;
   }

   public void reset() {
      this.textWidget = null;
      this.lastMouseOffset = 0;
   }

   public Optional<Integer> getLastMouseOffset(StyledText textWidget) {
      return textWidget == this.textWidget ? Optional.of(this.lastMouseOffset) : Optional.empty();
   }
}
