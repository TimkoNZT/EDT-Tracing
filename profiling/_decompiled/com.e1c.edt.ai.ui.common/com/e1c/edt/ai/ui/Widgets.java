package com.e1c.edt.ai.ui;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class Widgets implements IWidgets {
   public Stream<Control> getChildren(Composite target) {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ControlIterator(target), 1024), false);
   }

   private class ControlIterator implements Iterator<Control> {
      private final LinkedList<Control> controls = new LinkedList();

      public ControlIterator(Composite target) {
         this.controls.addLast(target);
      }

      public boolean hasNext() {
         return !this.controls.isEmpty();
      }

      public Control next() {
         Control control = (Control)this.controls.pollFirst();
         if (control instanceof Composite) {
            Composite nextComposite = (Composite)control;

            Control[] var6;
            for(Control child : var6 = nextComposite.getChildren()) {
               this.controls.addLast(child);
            }
         }

         return control;
      }
   }
}
