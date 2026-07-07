package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import org.eclipse.swt.custom.StyledText;

public class AITarget {
   private final StyledText textWidget;
   private final boolean limitSize;
   private final boolean preferSelection;

   public AITarget(StyledText textWidget, boolean limitSize, boolean preferSelection) {
      Preconditions.checkNotNull(textWidget);
      this.textWidget = textWidget;
      this.limitSize = limitSize;
      this.preferSelection = preferSelection;
   }

   public StyledText getTextWidget() {
      return this.textWidget;
   }

   public boolean getLimitSize() {
      return this.limitSize;
   }

   public boolean isPreferSelection() {
      return this.preferSelection;
   }
}
