package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class BaseGenerateDocCommentsAIHandler extends AbstractHandler {
   @Inject
   IUI ui;
   @Inject
   IChat chat;
   @Inject
   ICodeTools codeTools;
   @Inject
   ISettings settings;

   public BaseGenerateDocCommentsAIHandler() {
      BaseActivator.injectMembers(this);
   }

   public boolean isEnabled() {
      return this.settings.isEnabled() && this.codeTools.hasTarget(CodeAction.GENERATE_COMMENT);
   }

   public Object execute(ExecutionEvent event) throws ExecutionException {
      Optional<TargetMethod> optionalTargetMethod = this.codeTools.getTargetMethod();
      if (optionalTargetMethod.isPresent()) {
         TargetMethod targetMethod = (TargetMethod)optionalTargetMethod.get();
         this.codeTools.selectMethodComment(targetMethod);
         this.chat.generateDocComments(targetMethod.ctx, targetMethod.methodText);
      } else {
         this.ui.getLastSourceViewer().flatMap((sourceViewer) -> this.codeTools.createContextForTarget(sourceViewer, CodeAction.GENERATE_COMMENT)).ifPresent((ctx) -> this.chat.generateDocComments(ctx, ctx.getText()));
      }

      return null;
   }
}
