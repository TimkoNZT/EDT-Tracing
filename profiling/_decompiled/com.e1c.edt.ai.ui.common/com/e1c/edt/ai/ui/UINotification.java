package com.e1c.edt.ai.ui;

import com.google.inject.Inject;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class UINotification extends PopupDialog {
   @Inject
   private IDispatcher dispatcher;
   @Inject
   private IWeb web;
   private final String message;
   private final String linkText;
   private final String url;
   private final UINotificationType type;
   private Runnable action;
   private UINotificationService.UINotificationActionType actionType;

   public UINotification(Shell parentShell, String message, UINotificationType type, String linkText, String url) {
      super(parentShell, 16392, false, false, false, false, false, (String)null, (String)null);
      BaseActivator.injectMembers(this);
      this.message = message;
      this.type = type;
      this.linkText = linkText;
      this.url = url;
   }

   public UINotification(Shell parentShell, String message, UINotificationType type, String linkText, String url, Runnable action, UINotificationService.UINotificationActionType actionType) {
      this(parentShell, message, type, linkText, url);
      this.action = action;
      this.actionType = actionType;
   }

   protected Control createDialogArea(Composite parent) {
      Color bg = parent.getDisplay().getSystemColor(29);
      Color fg = parent.getDisplay().getSystemColor(28);
      parent.setBackground(bg);
      parent.setForeground(fg);
      parent.setBackgroundMode(2);
      Composite canvas = new Composite(parent, 0);
      GridLayout layout = new GridLayout(2, false);
      layout.marginWidth = 10;
      layout.marginHeight = 10;
      layout.verticalSpacing = 5;
      canvas.setLayout(layout);
      Label iconLabel = new Label(canvas, 0);
      iconLabel.setImage(BaseActivator.getImage(this.type.getImageId()));
      iconLabel.setLayoutData(new GridData(16384, 16777216, false, true));
      iconLabel.setBackground(bg);
      Composite textContainer = new Composite(canvas, 0);
      textContainer.setBackground(bg);
      GridLayout textLayout = new GridLayout(1, false);
      textLayout.marginWidth = 0;
      textLayout.marginHeight = 0;
      textContainer.setLayout(textLayout);
      textContainer.setLayoutData(new GridData(4, 16777216, true, true));
      Label textLabel = new Label(textContainer, 4);
      textLabel.setText(this.message);
      textLabel.setBackground(bg);
      textLabel.setForeground(fg);
      GridData textData = new GridData(4, 128, true, false);
      textData.heightHint = textLabel.computeSize(-1, -1).y;
      textLabel.setLayoutData(textData);
      if (this.linkText != null && !this.linkText.isEmpty()) {
         Link linkLabel = new Link(textContainer, 4);
         linkLabel.setText("<a href=\"" + this.url + "\">" + this.linkText + "</a>");
         linkLabel.setBackground(bg);
         linkLabel.setForeground(fg);
         linkLabel.setToolTipText(this.url);
         linkLabel.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
               UINotification.this.web.browse(UINotification.this.url);
            }
         });
         GridData linkData = new GridData(4, 128, true, false);
         linkData.heightHint = linkLabel.computeSize(-1, -1).y;
         linkLabel.setLayoutData(linkData);
      }

      Composite buttonContainer = new Composite(textContainer, 0);
      GridLayout buttonLayout = new GridLayout(2, false);
      buttonLayout.marginWidth = 0;
      buttonContainer.setLayout(buttonLayout);
      GridData buttonContainerData = new GridData(131072, 128, true, false);
      buttonContainerData.horizontalSpan = 2;
      buttonContainer.setLayoutData(buttonContainerData);
      buttonContainer.setBackground(bg);
      buttonContainer.setForeground(fg);
      if (this.action != null) {
         Button actionButton = new Button(buttonContainer, 8);
         actionButton.setText(this.actionType.getActionText());
         actionButton.setBackground(bg);
         actionButton.setForeground(fg);
         actionButton.setLayoutData(new GridData(131072, 128, false, false));
         actionButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
               UINotification.this.dispatcher.dispatchAsync(UINotification.this.action);
               UINotification.this.close();
            }
         });
      }

      Button closeButton = new Button(buttonContainer, 8);
      closeButton.setText(Messages.CloseButton);
      closeButton.setLayoutData(new GridData(131072, 128, false, false));
      closeButton.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            UINotification.this.close();
         }
      });
      return canvas;
   }

   protected void initializeBounds() {
      super.initializeBounds();
      int heightDelta = 20;
      int margin = 20;
      int minWidth = 150;
      int minHeight = 80;
      Shell shell = this.getShell();
      Point preferred = shell.computeSize(-1, -1);
      Rectangle ideBounds = this.getParentShell().getMonitor().getClientArea();
      int ideWidth = ideBounds.width;
      int ideHeight = ideBounds.height;
      int targetWidth = Math.max(preferred.x + heightDelta, minWidth);
      if (targetWidth > ideWidth - 2 * margin) {
         targetWidth = ideWidth - 2 * margin;
      }

      Point recomputed = shell.computeSize(targetWidth, -1);
      int targetHeight = Math.max(recomputed.y, minHeight);
      if (targetHeight > ideHeight - 2 * margin) {
         targetHeight = ideHeight - 2 * margin;
      }

      shell.setSize(targetWidth, targetHeight);
      int x = ideBounds.x + ideWidth - targetWidth - margin;
      int y = ideBounds.y + ideHeight - targetHeight - margin;
      int minX = ideBounds.x + margin;
      int maxX = ideBounds.x + ideWidth - targetWidth - margin;
      x = Math.min(Math.max(x, minX), maxX);
      int minY = ideBounds.y + margin;
      int maxY = ideBounds.y + ideHeight - targetHeight - margin;
      y = Math.min(Math.max(y, minY), maxY);
      shell.setLocation(x, y);
   }

   public int open() {
      int result = super.open();
      return result;
   }
}
