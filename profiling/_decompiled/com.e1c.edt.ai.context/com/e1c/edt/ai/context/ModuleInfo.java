package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.google.common.base.Preconditions;
import org.eclipse.emf.common.util.URI;

public class ModuleInfo {
   private static final URI BasePath = URI.createURI("platform:/resource/");
   private final Module module;
   private final String filePath;

   public ModuleInfo(Module module, String filePath) {
      Preconditions.checkNotNull(module);
      this.module = module;
      this.filePath = filePath;
   }

   public Module getModule() {
      return this.module;
   }

   public String getFilePath() {
      return this.filePath;
   }

   public String getFilePath2() {
      URI uri = this.module.eResource().getURI();
      return uri.deresolve(BasePath).path();
   }
}
