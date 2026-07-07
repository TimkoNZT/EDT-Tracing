package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Set;
import org.eclipse.ui.IStartup;
import org.osgi.framework.Version;

public class BasePluginStartup implements IStartup {
   @Inject
   Set<IInitializable> initializables;

   public void earlyStartup() {
      if (this.initializables == null) {
         BaseActivator.injectMembers(this);
      }

      Preconditions.checkNotNull(this.initializables, "initializables should be injected");
      BaseActivator activator = BaseActivator.getDefault();
      Version pluginVersion = activator.getPluginVersion();
      String platformVersion = activator.getPlatformVersion();
      activator.trace("common", platformVersion == null ? "Not 1C:EDT Platform" : "1C:EDT version: " + platformVersion.toString(), () -> "");
      activator.trace("common", pluginVersion == null ? "" : "Plugin version: " + pluginVersion.toString(), () -> "");

      for(IInitializable initializable : this.initializables) {
         initializable.initialize();
      }

   }
}
