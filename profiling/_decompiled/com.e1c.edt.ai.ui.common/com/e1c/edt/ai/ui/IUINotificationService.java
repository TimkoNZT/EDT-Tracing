package com.e1c.edt.ai.ui;

import org.eclipse.swt.widgets.Shell;

public interface IUINotificationService {
   void createNotification(Shell var1, String var2, String var3, String var4, UINotificationType var5);

   void createNotificationWithAction(Shell var1, String var2, Runnable var3, UINotificationService.UINotificationActionType var4, UINotificationType var5);

   void closeNotificationIfOpen();
}
