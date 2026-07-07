package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.OS;
import com.e1c.edt.ai.Range;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.DPIUtil;

class VerticalRulerPainter implements IVerticalRulerPainter {
   private final IGCTools gcTools;
   private final IEnvironment environment;
   StyledText textWidget;
   String hintText;
   private Range range;

   @Inject
   public VerticalRulerPainter(IGCTools gcTools, IEnvironment environment) {
      this.range = Range.EMPTY;
      Preconditions.checkNotNull(gcTools);
      Preconditions.checkNotNull(environment);
      this.gcTools = gcTools;
      this.environment = environment;
   }

   public synchronized void pin(StyledText textWidget, String hintText) {
      this.textWidget = textWidget;
      this.hintText = hintText;
      this.updateRange();
   }

   public synchronized void updateRange() {
      if (this.textWidget != null && this.hintText != null && this.environment.getOS() == OS.WINDOWS && !this.hintText.isEmpty()) {
         String[] lines = this.hintText.split("\n");
         int linesCount = lines.length;
         if (linesCount < 2) {
            this.range = Range.EMPTY;
         } else {
            int hintOffset = this.textWidget.getCaretOffset();
            int y = this.textWidget.getLocationAtOffset(hintOffset).y + this.textWidget.getLineHeight();
            int h = (this.textWidget.getLineHeight() + 1) * (linesCount - 1);
            if (h <= 0) {
               this.range = Range.EMPTY;
            } else {
               this.range = new Range(y, (int)((double)h * (DPIUtil.getDeviceZoom() >= 200 ? 0.9 : (double)1.0F)));
            }
         }
      } else {
         this.range = Range.EMPTY;
      }
   }

   public synchronized void reset() {
      this.hintText = "";
      this.range = Range.EMPTY;
   }

   public synchronized void paintControl(PaintEvent e) {
      if (this.range != Range.EMPTY) {
         GC gc = e.gc;
         if (!gc.isDisposed()) {
            Rectangle bounds = gc.getClipping();
            if (bounds != null) {
               int y = this.range.getStart();
               int h = this.range.getLength();
               if (y >= 0) {
                  this.gcTools.copyArea(gc, bounds.x, y, bounds.width, bounds.height - y - h, bounds.x, y + h);
               }

               gc.fillRectangle(bounds.x, y, bounds.width, h);
               gc.setAlpha(200);
               gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
            }
         }
      }
   }
}
