package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import java.util.Set;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class DialogsEnhancer implements IInitializable, IPartListener2 {
   private final IDispatcher dispatcher;
   private final Set<IViewEnhancer> viewEnhancers;

   @Inject
   public DialogsEnhancer(IDispatcher dispatcher, Set<IViewEnhancer> viewEnhancers) {
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(viewEnhancers);
      this.viewEnhancers = viewEnhancers;
      this.dispatcher = dispatcher;
   }

   public void initialize() {
      this.dispatcher.dispatchAsync(() -> {
         IWorkbench workbench = PlatformUI.getWorkbench();
         this.dispatcher.dispatchAsync(() -> {
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window != null) {
               IWorkbenchPage page = window.getActivePage();
               if (page != null) {
                  IViewReference[] var7;
                  for(IViewReference veiwRef : var7 = page.getViewReferences()) {
                     this.setup(veiwRef);
                  }

                  page.addPartListener(this);
               }
            }
         });
      });
   }

   public void partOpened(IWorkbenchPartReference partRef) {
      this.setup(partRef);
   }

   private void setup(IWorkbenchPartReference partRef) {
      for(IViewEnhancer viewEnhancer : this.viewEnhancers) {
         String id = partRef.getId();
         if (id != null && !id.isBlank()) {
            Supplier<IWorkbenchPart> viewPart = Suppliers.memoize(() -> partRef.getPart(false));
            viewEnhancer.getViewId().ifPresent((viewId) -> {
               if (id.equals(viewId)) {
                  viewEnhancer.setup((IWorkbenchPart)viewPart.get());
               }

            });
         }
      }

   }
}
