package com.e1c.edt.ai.ui.quickaccess;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ui.AITarget;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IAIContextProvider;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.quickaccess.QuickAccessElement;

public class AskAIQuickAccessElement extends QuickAccessElement {
   @Inject
   IAIContextProvider aiContextProvider;
   @Inject
   IChat chat;
   @Inject
   IUI ui;
   private String askText;

   public AskAIQuickAccessElement() {
      this(Messages.QuickAccessElementAskAI_0);
   }

   public AskAIQuickAccessElement(String input) {
      BaseActivator.injectMembers(this);
      this.askText = input;
   }

   public void execute() {
      AIContext ctx = (AIContext)this.ui.getLastSourceViewer().flatMap((sourceViewer) -> this.aiContextProvider.create(sourceViewer, new AITarget(sourceViewer.getTextWidget(), false, true), CancellationTokens.NONE)).orElse((Object)null);
      this.ui.showView("com.e1c.edt.ai.ui.views.ChatView").ifPresent((view) -> {
         this.chat.askQuestion(ctx, this.askText);
         view.setFocus();
      });
   }

   public String getId() {
      return this.askText;
   }

   public String getLabel() {
      return Messages.QuickAccessElementAskAI_1 + this.askText;
   }

   public ImageDescriptor getImageDescriptor() {
      ImageDescriptor image = BaseActivator.getImageDescriptor("AI");
      return image;
   }
}
