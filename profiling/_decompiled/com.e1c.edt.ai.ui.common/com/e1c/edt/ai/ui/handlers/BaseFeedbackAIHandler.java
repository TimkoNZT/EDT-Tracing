package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IIssueFeedbackViewModel;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class BaseFeedbackAIHandler extends AbstractHandler {
   @Inject
   Provider<IIssueFeedbackViewModel> issueFeedbackViewModelProvider;
   @Inject
   ISettings settings;

   public BaseFeedbackAIHandler() {
      BaseActivator.injectMembers(this);
   }

   public boolean isEnabled() {
      return this.settings.isEnabled();
   }

   public Object execute(ExecutionEvent event) throws ExecutionException {
      ((IIssueFeedbackViewModel)this.issueFeedbackViewModelProvider.get()).getFeedback();
      return null;
   }
}
