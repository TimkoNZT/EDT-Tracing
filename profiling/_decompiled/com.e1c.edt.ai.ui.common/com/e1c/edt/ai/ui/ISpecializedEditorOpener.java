package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

public interface ISpecializedEditorOpener {
   IEditorPart openInSpecializedEditor(IWorkbenchPage var1, IFile var2);
}
