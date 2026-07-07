package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintListener;

interface IHintPainter extends PaintListener {
   void pinOffset(StyledText var1, int var2, boolean var3, boolean var4);

   int getOffset();

   String getHintText();

   String getDisplayedHintText();

   void reset();

   void setHintAt(String var1, String var2, int var3);
}
