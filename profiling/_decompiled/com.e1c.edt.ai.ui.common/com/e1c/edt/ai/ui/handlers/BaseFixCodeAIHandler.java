package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class BaseFixCodeAIHandler extends AbstractHandler {
   @Inject
   IUI ui;
   @Inject
   IChat chat;
   @Inject
   ICodeTools codeTools;
   @Inject
   IFixDialog fixDialog;
   @Inject
   ISettings settings;

   public BaseFixCodeAIHandler() {
      BaseActivator.injectMembers(this);
   }

   public boolean isEnabled() {
      return this.settings.isEnabled() && this.codeTools.hasTarget(CodeAction.FIX);
   }

   public Object execute(ExecutionEvent event) throws ExecutionException {
      this.ui.getLastSourceViewer().flatMap((sourceViewer) -> this.codeTools.createContextForTarget(sourceViewer, CodeAction.FIX)).ifPresent((ctx) -> {
         if (this.fixDialog.show() == 0) {
            this.chat.fixCode(ctx, ctx.getText(), this.fixDialog.getDetails());
         }

      });
      return null;
   }
}
