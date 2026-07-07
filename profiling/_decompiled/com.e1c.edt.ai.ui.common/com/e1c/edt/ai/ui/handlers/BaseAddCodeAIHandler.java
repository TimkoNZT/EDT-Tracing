package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class BaseAddCodeAIHandler extends AbstractHandler {
   @Inject
   IUI ui;
   @Inject
   IChat chat;
   @Inject
   ICodeTools codeTools;
   @Inject
   ISettings settings;

   public BaseAddCodeAIHandler() {
      BaseActivator.injectMembers(this);
   }

   public boolean isEnabled() {
      return this.settings.isEnabled() && this.codeTools.hasTarget(CodeAction.ADD);
   }

   public Object execute(ExecutionEvent event) throws ExecutionException {
      this.ui.getLastSourceViewer().flatMap((sourceViewer) -> this.codeTools.createContextForTarget(sourceViewer, CodeAction.ADD)).ifPresent((ctx) -> this.chat.addCode(ctx, ctx.getText()));
      return null;
   }
}
