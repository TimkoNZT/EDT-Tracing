package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.net.URI;

public class Web implements IWeb {
   private final ILog log;

   @Inject
   public Web(ILog log) {
      this.log = log;
   }

   public void browse(String url) {
      try {
         if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Action.BROWSE)) {
               desktop.browse(new URI(url));
            }
         }
      } catch (Exception error) {
         this.log.logError(error);
      }

   }
}
