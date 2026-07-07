package com.e1c.edt.ai.ui;

import org.eclipse.jface.text.source.SourceViewer;

interface IVerticalRulerManager {
   AutoCloseable activate(SourceViewer var1, Runnable var2);

   AutoCloseable freeze(SourceViewer var1);

   void reset(SourceViewer var1);

   void redraw(SourceViewer var1);
}
