package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.context.ContextModuleFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class Activator extends BaseActivator {
   protected Injector createInjector() {
      Module mergedModule = ContextModuleFactory.create(this).with(new Module[]{new AIUICommonModule(), new AIUIModule(this)});
      return Guice.createInjector(new Module[]{mergedModule});
   }

   public String getPluginId() {
      return "com.e1c.edt.ai.ui";
   }
}
