package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.ui.BaseActivator;
import org.eclipse.core.expressions.PropertyTester;

public abstract class BaseTraceModePropertyTester extends PropertyTester {
   private static final String PROPERTY_IS_TRACE_MODE = "isTraceMode";

   public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
      if (!"isTraceMode".equals(property)) {
         return false;
      } else {
         BaseActivator activator = BaseActivator.getDefault();
         if (activator == null) {
            return false;
         } else {
            ISettings settings = activator.trySettings();
            if (settings == null) {
               return false;
            } else {
               return settings.isEnabled() && settings.getVerbosity() == Verbosity.TRACE;
            }
         }
      }
   }
}
