package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

public class NoOpSpecializedEditorOpener implements ISpecializedEditorOpener {
   public IEditorPart openInSpecializedEditor(IWorkbenchPage page, IFile file) {
      return null;
   }
}
