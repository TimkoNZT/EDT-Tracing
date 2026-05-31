package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class SourceLookupManager implements IWindowListener {
   private static SourceLookupManager fgDefault;
   private final Map<IWorkbenchWindow, SourceLookupService> fServices = new HashMap();

   private SourceLookupManager() {
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow[] workbenchWindows = workbench.getWorkbenchWindows();

      for(IWorkbenchWindow window : workbenchWindows) {
         this.windowOpened(window);
      }

      workbench.addWindowListener(this);
   }

   public static SourceLookupManager getDefault() {
      if (fgDefault == null) {
         fgDefault = new SourceLookupManager();
      }

      return fgDefault;
   }

   public void windowActivated(IWorkbenchWindow window) {
   }

   public void windowDeactivated(IWorkbenchWindow window) {
   }

   public void windowClosed(IWorkbenchWindow window) {
      SourceLookupService service = (SourceLookupService)this.fServices.get(window);
      if (service != null) {
         this.fServices.remove(window);
         service.dispose();
      }

   }

   public void windowOpened(IWorkbenchWindow window) {
      SourceLookupService service = (SourceLookupService)this.fServices.get(window);
      if (service == null) {
         service = new SourceLookupService(window);
         this.fServices.put(window, service);
      }

   }

   public void displaySource(Object context, IWorkbenchPage page, boolean forceSourceLookup) {
      IWorkbenchWindow window = page.getWorkbenchWindow();
      SourceLookupService service = (SourceLookupService)this.fServices.get(window);
      if (service != null) {
         service.displaySource(context, page, forceSourceLookup);
      }

   }
}
