package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ITraceScenario;
import com.e1c.edt.ai.TraceScenarioType;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.google.inject.Inject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class BaseTokenNotFoundAIHandler extends AbstractHandler {
   @Inject
   IChat chat;
   @Inject
   ISettings settings;
   @Inject
   ITraceScenario traceScenario;

   public BaseTokenNotFoundAIHandler() {
      BaseActivator.injectMembers(this);
   }

   public boolean isEnabled() {
      return this.settings.isEnabled() && this.settings.getVerbosity() == Verbosity.TRACE;
   }

   public Object execute(ExecutionEvent event) throws ExecutionException {
      this.traceScenario.activate(TraceScenarioType.TOKEN_NOT_FOUND);
      return null;
   }
}
