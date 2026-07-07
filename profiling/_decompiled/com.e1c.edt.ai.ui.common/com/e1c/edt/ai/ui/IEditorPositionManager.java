package com.e1c.edt.ai.ui;

import org.eclipse.ui.IEditorPart;

public interface IEditorPositionManager {
   void openFileInEditor(String var1, IEdtLinkHandler.CursorPositionInfo var2, IEdtLinkHandler.SelectionInfo var3);

   void restoreCursorPosition(IEditorPart var1, IEdtLinkHandler.CursorPositionInfo var2);

   void restoreSelection(IEditorPart var1, IEdtLinkHandler.SelectionInfo var2);
}
