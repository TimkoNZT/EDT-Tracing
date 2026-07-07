package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import org.eclipse.swt.graphics.GC;

class GCTools implements IGCTools {
   public void copyArea(GC gc, int srcX, int srcY, int width, int height, int destX, int destY) {
      Preconditions.checkNotNull(gc);
      Preconditions.checkArgument(!gc.isDisposed());
      if (width >= 1 && height >= 1) {
         gc.copyArea(srcX, srcY, width, height, destX, destY);
      }
   }
}
