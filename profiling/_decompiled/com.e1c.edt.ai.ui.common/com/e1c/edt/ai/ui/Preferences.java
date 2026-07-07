package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class Preferences implements IPreferences {
   private final ILog log;

   @Inject
   public Preferences(ILog log) {
      this.log = log;
   }

   public void show(String pageId) {
      try {
         Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
         if (shell != null && !shell.isDisposed()) {
            PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, pageId, (String[])null, (Object)null);
            if (dialog != null) {
               dialog.open();
            }
         }
      } catch (Exception e) {
         this.log.logError(e);
      }

   }
}
