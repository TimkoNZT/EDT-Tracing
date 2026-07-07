package com.e1c.edt.ai.context;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.eclipse.core.runtime.Plugin;

public class ContextModuleFactory {
   public static Modules.OverriddenModuleBuilder create(Plugin plugin) {
      return Modules.override(new Module[]{new ContextModule(plugin)});
   }
}
