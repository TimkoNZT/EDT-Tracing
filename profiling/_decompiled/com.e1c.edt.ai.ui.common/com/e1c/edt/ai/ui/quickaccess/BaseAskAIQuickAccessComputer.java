package com.e1c.edt.ai.ui.quickaccess;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.quickaccess.IQuickAccessComputer;
import org.eclipse.ui.quickaccess.IQuickAccessComputerExtension;
import org.eclipse.ui.quickaccess.QuickAccessElement;

public class BaseAskAIQuickAccessComputer implements IQuickAccessComputer, IQuickAccessComputerExtension {
   public QuickAccessElement[] computeElements() {
      return new QuickAccessElement[0];
   }

   public void resetState() {
   }

   public boolean needsRefresh() {
      return false;
   }

   public QuickAccessElement[] computeElements(String query, IProgressMonitor monitor) {
      AskAIQuickAccessElement myElement = new AskAIQuickAccessElement(query);
      return new QuickAccessElement[]{myElement};
   }
}
