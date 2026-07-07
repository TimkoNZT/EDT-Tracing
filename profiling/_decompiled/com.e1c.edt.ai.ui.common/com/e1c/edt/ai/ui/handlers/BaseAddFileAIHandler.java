package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.google.inject.Inject;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class BaseAddFileAIHandler extends AbstractHandler {
   @Inject
   IChat chat;
   @Inject
   ISettings settings;

   public BaseAddFileAIHandler() {
      BaseActivator.injectMembers(this);
   }

   public boolean isEnabled() {
      return this.settings.isEnabled();
   }

   public Object execute(ExecutionEvent event) throws ExecutionException {
      this.chat.addFiles((List)null);
      return null;
   }
}
