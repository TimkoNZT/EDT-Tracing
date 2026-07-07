package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintListener;

interface IVerticalRulerPainter extends PaintListener {
   void pin(StyledText var1, String var2);

   void updateRange();

   void reset();
}
