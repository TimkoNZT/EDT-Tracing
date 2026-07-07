package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class BaseOpenChatViewHandler extends AbstractHandler {
   @Inject
   IUI ui;
   @Inject
   ISettings settings;

   public BaseOpenChatViewHandler() {
      BaseActivator.injectMembers(this);
   }

   public boolean isEnabled() {
      return this.settings.isEnabled();
   }

   public Object execute(ExecutionEvent event) throws ExecutionException {
      this.ui.showView("com.e1c.edt.ai.ui.views.ChatView").ifPresent((view) -> view.setFocus());
      return null;
   }
}
