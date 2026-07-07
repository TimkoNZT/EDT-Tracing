package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import org.eclipse.swt.widgets.Shell;

public class Notifications implements INotifications {
   private final IUI ui;
   private final IDispatcher dispatcher;
   private final IUINotificationService notificationService;
   private final ISettings settings;

   @Inject
   public Notifications(IUI ui, IDispatcher dispatcher, IUINotificationService notificationService, ISettings settings) {
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(notificationService);
      Preconditions.checkNotNull(settings);
      this.ui = ui;
      this.dispatcher = dispatcher;
      this.notificationService = notificationService;
      this.settings = settings;
   }

   public boolean showMissingTokenInfo() {
      return this.createNotification(Messages.NotActivated, Messages.Activation, this.settings.getHomePage(), UINotificationType.INFO);
   }

   public boolean showTokenError() {
      return this.createNotification(com.e1c.edt.ai.Messages.StatusTokenFailed, Messages.Support, this.settings.getHomePage() + ServiceState.TOKEN_ERROR.getUrlPath(), UINotificationType.ERROR);
   }

   public boolean showSSLError() {
      return this.createNotification(com.e1c.edt.ai.Messages.StatusSSLFailed, Messages.Support, this.settings.getHomePage() + ServiceState.SSL_ERROR.getUrlPath(), UINotificationType.ERROR);
   }

   private boolean createNotification(String title, String buttonText, String url, UINotificationType type) {
      return (Boolean)this.dispatcher.dispatch((Supplier)(() -> {
         Shell shell = (Shell)this.ui.getShell().orElse((Object)null);
         if (shell != null) {
            this.notificationService.closeNotificationIfOpen();
            this.notificationService.createNotification(shell, title, buttonText, url, type);
            return true;
         } else {
            return false;
         }
      })).orElse(false);
   }
}
