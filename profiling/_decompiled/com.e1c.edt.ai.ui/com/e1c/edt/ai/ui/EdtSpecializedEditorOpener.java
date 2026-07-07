package com.e1c.edt.ai.ui;

import com._1c.g5.v8.dt.ui.util.OpenHelper;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

public class EdtSpecializedEditorOpener implements ISpecializedEditorOpener {
   private final ILog log;

   @Inject
   public EdtSpecializedEditorOpener(ILog log) {
      Preconditions.checkNotNull(log);
      this.log = log;
   }

   public IEditorPart openInSpecializedEditor(IWorkbenchPage page, IFile file) {
      try {
         return (new OpenHelper(page)).openEditor(file, (ISelection)null);
      } catch (Exception e) {
         this.log.logError("EdtSpecializedEditorOpener failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
         return null;
      }
   }
}
