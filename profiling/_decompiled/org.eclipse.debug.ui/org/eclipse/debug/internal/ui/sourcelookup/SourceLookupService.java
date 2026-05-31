package org.eclipse.debug.internal.ui.sourcelookup;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.launch.DebugElementAdapterFactory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

public class SourceLookupService implements IDebugContextListener, ISourceDisplay {
   private IWorkbenchWindow fWindow;
   private final IDebugContextService fDebugContextService;

   public SourceLookupService(IWorkbenchWindow window) {
      this.fWindow = window;
      this.fDebugContextService = DebugUITools.getDebugContextManager().getContextService(window);
      this.fDebugContextService.addDebugContextListener(this);
   }

   public void dispose() {
      this.fDebugContextService.removeDebugContextListener(this);
      this.fWindow = null;
   }

   public synchronized void debugContextChanged(DebugContextEvent event) {
      if ((event.getFlags() & 1) > 0 && (this.isDebugViewActive(event) || this.canActivateDebugView())) {
         this.displaySource(event.getContext(), event.getDebugContextProvider().getPart(), false);
      }

   }

   private boolean isDebugViewActive(DebugContextEvent event) {
      if (this.isDisposed()) {
         return false;
      } else {
         IWorkbenchPage activePage = this.fWindow.getActivePage();
         if (activePage != null) {
            IViewPart debugView = null;
            IWorkbenchPart part = event.getDebugContextProvider().getPart();
            if (part != null) {
               debugView = activePage.findView(part.getSite().getId());
            }

            if (debugView == null) {
               debugView = activePage.findView("org.eclipse.debug.ui.DebugView");
            }

            return debugView != null;
         } else {
            return false;
         }
      }
   }

   private boolean canActivateDebugView() {
      if (this.isDisposed()) {
         return false;
      } else {
         IPreferenceStore preferenceStore = DebugUIPlugin.getDefault().getPreferenceStore();
         String[] switchPreferences = new String[]{"org.eclipse.debug.ui.switch_to_perspective", "org.eclipse.debug.ui.switch_perspective_on_suspend"};

         for(String switchPreference : switchPreferences) {
            String preferenceValue = preferenceStore.getString(switchPreference);
            if (!"never".equals(preferenceValue)) {
               return true;
            }
         }

         boolean canActivateDebugView = preferenceStore.getBoolean("org.eclipse.debug.ui.activate_debug_view");
         return canActivateDebugView;
      }
   }

   private boolean isDisposed() {
      return this.fWindow == null;
   }

   protected synchronized void displaySource(ISelection selection, IWorkbenchPart part, boolean force) {
      if (!this.isDisposed()) {
         if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection)selection;
            if (structuredSelection.size() == 1) {
               Object context = structuredSelection.getFirstElement();
               IWorkbenchPage page = null;
               if (part == null) {
                  page = this.fWindow.getActivePage();
               } else {
                  page = part.getSite().getPage();
               }

               this.displaySource(context, page, force);
            }
         }

      }
   }

   public void displaySource(Object context, IWorkbenchPage page, boolean forceSourceLookup) {
      if (context instanceof IAdaptable adaptable) {
         ISourceDisplay adapter = (ISourceDisplay)adaptable.getAdapter(ISourceDisplay.class);
         if (adapter == null && !(context instanceof PlatformObject)) {
            adapter = (ISourceDisplay)(new DebugElementAdapterFactory()).getAdapter(context, ISourceDisplay.class);
         }

         if (adapter != null) {
            adapter.displaySource(context, page, forceSourceLookup);
         }
      }

   }
}
