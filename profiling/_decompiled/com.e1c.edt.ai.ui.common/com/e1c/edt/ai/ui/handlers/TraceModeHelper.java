package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

public class TraceModeHelper {
   @Inject
   private static ISettings settings;

   static {
      BaseActivator.injectMembers(new TraceModeHelper());
   }

   public static boolean isTraceMode() {
      return settings != null && settings.isEnabled() && settings.getVerbosity() == Verbosity.TRACE;
   }
}
