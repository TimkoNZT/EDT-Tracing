package com._1c.g5.v8.dt.internal.debug.ui;

import com._1c.g5.v8.dt.common.Pair;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;

public class CachingBslSourceDisplay extends BslSourceDisplay {
   private final Map<Pair<EObject, EStructuralFeature>, IEditorPart> editorCache = new ConcurrentHashMap();
   private final Map<IWorkbenchWindow, IPartListener2> cacheCleaners = new ConcurrentHashMap();
   private final CacheCleaner cacheCleaner = new CacheCleaner();

   public CachingBslSourceDisplay() {
      PlatformUI.getWorkbench().addWindowListener(this.cacheCleaner);
   }

   protected IEditorPart openModuleEditor(EObject moduleOwner, EStructuralFeature reference, IWorkbenchPage page, IBslStackFrame stackFrame, boolean forceSourceLookup) {
      Pair<EObject, EStructuralFeature> cacheKey = Pair.newPair(moduleOwner, reference);
      IEditorPart editor = (IEditorPart)this.editorCache.get(cacheKey);
      IWorkbenchWindow workbenchWindow = page.getWorkbenchWindow();
      if (editor != null && !forceSourceLookup) {
         workbenchWindow.getActivePage().bringToTop(editor);
      } else {
         editor = super.openModuleEditor(moduleOwner, reference, page, stackFrame, forceSourceLookup);
         this.cacheCleaners.computeIfAbsent(workbenchWindow, (window) -> {
            window.getPartService().addPartListener(this.cacheCleaner);
            return this.cacheCleaner;
         });
         if (editor instanceof MultiPageEditorPart) {
            ((MultiPageEditorPart)editor).addPageChangedListener(this.cacheCleaner);
         }

         this.editorCache.put(cacheKey, editor);
      }

      return editor;
   }

   private class CacheCleaner implements IPartListener2, IWindowListener, IPageChangedListener {
      public void windowOpened(IWorkbenchWindow window) {
      }

      public void windowDeactivated(IWorkbenchWindow window) {
      }

      public void windowClosed(IWorkbenchWindow window) {
         window.getPartService().removePartListener(CachingBslSourceDisplay.this.cacheCleaner);
         CachingBslSourceDisplay.this.cacheCleaners.remove(window);
      }

      public void windowActivated(IWorkbenchWindow window) {
      }

      public void partActivated(IWorkbenchPartReference partRef) {
      }

      public void partBroughtToTop(IWorkbenchPartReference partRef) {
      }

      public void partClosed(IWorkbenchPartReference partRef) {
         this.clearCache(partRef.getPart(false));
      }

      public void partDeactivated(IWorkbenchPartReference partRef) {
      }

      public void partOpened(IWorkbenchPartReference partRef) {
      }

      public void partHidden(IWorkbenchPartReference partRef) {
      }

      public void partVisible(IWorkbenchPartReference partRef) {
      }

      public void partInputChanged(IWorkbenchPartReference partRef) {
      }

      public void pageChanged(PageChangedEvent event) {
         IPageChangeProvider provider = event.getPageChangeProvider();
         if (provider instanceof IWorkbenchPart) {
            this.clearCache((IWorkbenchPart)provider);
         }

      }

      private void clearCache(IWorkbenchPart part) {
         CachingBslSourceDisplay.this.editorCache.values().remove(part);
         if (CachingBslSourceDisplay.this.editorCache.isEmpty()) {
            IWorkbenchWindow workbenchWindow = part.getSite().getPage().getWorkbenchWindow();
            workbenchWindow.getPartService().removePartListener(this);
            CachingBslSourceDisplay.this.cacheCleaners.remove(workbenchWindow);
         }

      }
   }
}
