package com.e1c.edt.ai.ui;

import org.eclipse.swt.widgets.Shell;

public class UINotificationService implements IUINotificationService {
   private UINotification lastNotification;

   public void createNotification(Shell parentShell, String message, String linkText, String url, UINotificationType type) {
      UINotification popup = new UINotification(parentShell, message, type, linkText, url);
      popup.setBlockOnOpen(false);
      this.closeNotificationIfOpen();
      this.lastNotification = popup;
      popup.open();
   }

   public void createNotificationWithAction(Shell parentShell, String message, Runnable action, UINotificationActionType actionType, UINotificationType type) {
      UINotification popup = new UINotification(parentShell, message, type, (String)null, (String)null, action, actionType);
      popup.setBlockOnOpen(false);
      this.closeNotificationIfOpen();
      popup.open();
   }

   public void closeNotificationIfOpen() {
      if (this.lastNotification != null) {
         this.lastNotification.close();
         this.lastNotification = null;
      }

   }

   public static enum UINotificationActionType {
      UPDATE(Messages.UpdateButton, Messages.UpdatePluginJob),
      RELOAD(Messages.RestartButton, Messages.RestartJob);

      private final String buttonText;
      private final String jobName;

      private UINotificationActionType(String buttonText, String jobName) {
         this.buttonText = buttonText;
         this.jobName = jobName;
      }

      public String getActionText() {
         return this.buttonText;
      }

      public String getJobName() {
         return this.jobName;
      }
   }
}
