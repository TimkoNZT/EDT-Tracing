package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.OS;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.IVerticalRulerInfoExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

class VerticalRulerManager implements IVerticalRulerManager {
   private final IDispatcher dispatcher;
   private final IVerticalRulerPainter painterListener;
   private final IEnvironment environment;
   private final IReflection reflection;

   @Inject
   public VerticalRulerManager(IDispatcher dispatcher, IVerticalRulerPainter painterListener, IEnvironment environment, IReflection reflection) {
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(painterListener);
      Preconditions.checkNotNull(environment);
      Preconditions.checkNotNull(reflection);
      this.dispatcher = dispatcher;
      this.painterListener = painterListener;
      this.environment = environment;
      this.reflection = reflection;
   }

   public AutoCloseable activate(SourceViewer viewer, Runnable onReset) {
      Preconditions.checkNotNull(onReset);
      if (viewer == null) {
         return Closeables.Empty;
      } else {
         return this.environment.getOS() != OS.WINDOWS ? Closeables.Empty : (AutoCloseable)Optional.ofNullable(viewer).flatMap((v) -> this.getCompositeRuler(v)).map((ruler) -> {
            AnnotationModelListener modelListener = this.createModelListener(ruler);
            ViewportListener viewportListener = new ViewportListener(viewer);
            this.addListeners(viewer, ruler, modelListener, viewportListener);
            return Closeables.create(() -> this.removeListeners(viewer, ruler, modelListener, viewportListener));
         }).orElse(Closeables.Empty);
      }
   }

   public AutoCloseable freeze(SourceViewer viewer) {
      if (viewer == null) {
         return Closeables.Empty;
      } else {
         return this.environment.getOS() != OS.WINDOWS ? Closeables.Empty : (AutoCloseable)Optional.ofNullable(viewer).flatMap((v) -> this.getCompositeRuler(v)).map((ruler) -> {
            Iterator<IVerticalRulerColumn> decorators = ruler.getDecoratorIterator();
            ArrayList<AutoCloseable> tokens = new ArrayList();

            while(decorators.hasNext()) {
               IVerticalRulerColumn column = (IVerticalRulerColumn)decorators.next();
               Control columnControl = column.getControl();
               boolean isEnabeld = columnControl.isEnabled();
               columnControl.setEnabled(false);
               tokens.add(Closeables.create(() -> columnControl.setEnabled(isEnabeld)));
            }

            return Closeables.create((AutoCloseable[])tokens.toArray(new AutoCloseable[tokens.size()]));
         }).orElse(Closeables.Empty);
      }
   }

   public void reset(SourceViewer viewer) {
      this.dispatcher.dispatch((Runnable)(() -> Optional.ofNullable(viewer).map((v) -> v.getTextWidget()).ifPresent((w) -> w.redraw())));
   }

   private AnnotationModelListener createModelListener(CompositeRuler ruler) {
      Runnable run = () -> this.redraw(ruler.getControl());
      return new AnnotationModelListener(() -> this.dispatcher.dispatchAsync(() -> {
            run.run();
            this.dispatcher.dispatchAsync(run);
         }));
   }

   public void redraw(SourceViewer viewer) {
      this.dispatcher.dispatch((Runnable)(() -> this.redrawInternal(viewer)));
   }

   private void redrawInternal(SourceViewer viewer) {
      Optional.ofNullable(viewer).flatMap((v) -> this.getCompositeRuler(v)).map((ruler) -> ruler.getDecoratorIterator()).ifPresent((columns) -> {
         while(columns.hasNext()) {
            IVerticalRulerColumn column = (IVerticalRulerColumn)columns.next();
            this.redraw(column.getControl());
         }

      });
   }

   private void redraw(Control control) {
      if (!control.isDisposed()) {
         if (control instanceof Composite) {
            Composite composite = (Composite)control;
            control.redraw();

            Control[] var6;
            for(Control child : var6 = composite.getChildren()) {
               this.redraw(child);
            }
         } else {
            control.redraw();
         }

      }
   }

   private void addListeners(SourceViewer viewer, CompositeRuler ruler, AnnotationModelListener modelListener, ViewportListener viewportListener) {
      Iterator<IVerticalRulerColumn> decorators = ruler.getDecoratorIterator();

      while(decorators.hasNext()) {
         IVerticalRulerColumn column = (IVerticalRulerColumn)decorators.next();
         if (column instanceof IVerticalRulerInfoExtension) {
            IVerticalRulerInfoExtension info = (IVerticalRulerInfoExtension)column;
            IAnnotationModel model = info.getModel();
            if (model != null) {
               model.addAnnotationModelListener(modelListener);
            }
         }

         Control control = column.getControl();
         control.addPaintListener(this.painterListener);
      }

      viewer.addViewportListener(viewportListener);
   }

   private void removeListeners(SourceViewer viewer, CompositeRuler ruler, AnnotationModelListener modelListener, ViewportListener viewportListener) {
      Iterator<IVerticalRulerColumn> decorators = ruler.getDecoratorIterator();

      while(decorators.hasNext()) {
         IVerticalRulerColumn column = (IVerticalRulerColumn)decorators.next();
         if (column instanceof IVerticalRulerInfoExtension) {
            IVerticalRulerInfoExtension info = (IVerticalRulerInfoExtension)column;
            info.getModel().removeAnnotationModelListener(modelListener);
         }

         Control control = column.getControl();
         control.removePaintListener(this.painterListener);
      }

      viewer.removeViewportListener(viewportListener);
   }

   private Optional<CompositeRuler> getCompositeRuler(SourceViewer viewer) {
      return this.getVerticalRuler(viewer).map((ruler) -> ruler instanceof CompositeRuler ? (CompositeRuler)ruler : null);
   }

   private Optional<IVerticalRuler> getVerticalRuler(SourceViewer viewer) {
      return this.reflection.callMethod(SourceViewer.class, viewer, "getVerticalRuler", IVerticalRuler.class);
   }

   private class AnnotationModelListener implements IAnnotationModelListener {
      private final Runnable onModelChanged;

      public AnnotationModelListener(Runnable onModelChanged) {
         Preconditions.checkNotNull(onModelChanged);
         this.onModelChanged = onModelChanged;
      }

      public void modelChanged(IAnnotationModel model) {
         this.onModelChanged.run();
      }
   }

   private class ViewportListener implements IViewportListener {
      private final SourceViewer viewer;

      public ViewportListener(SourceViewer viewer) {
         this.viewer = viewer;
      }

      public void viewportChanged(int verticalOffset) {
         VerticalRulerManager.this.dispatcher.dispatchAsync(() -> VerticalRulerManager.this.dispatcher.dispatchAsync(() -> {
               VerticalRulerManager.this.painterListener.updateRange();
               VerticalRulerManager.this.redrawInternal(this.viewer);
            }));
      }
   }
}
