package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import javafx.embed.swt.FXCanvas;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class BaseChatView extends ViewPart {
   public static final String ID = "com.e1c.edt.ai.ui.views.ChatView";
   private FXCanvas canvas;
   @Inject
   IChatDialog chatDialog;

   public void createPartControl(Composite parent) {
      Preconditions.checkNotNull(parent);
      if (this.chatDialog == null) {
         BaseActivator.injectMembers(this);
      }

      Preconditions.checkNotNull(this.chatDialog, "chatDialog should be injected");
      parent.setLayout(new GridLayout());
      GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(parent);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
      this.canvas = new FXCanvas(parent, 2048);
      GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(this.canvas);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(this.canvas);
      ScrollPane scrollPane = new ScrollPane();
      scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
      scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER);
      scrollPane.setBorder(Border.EMPTY);
      scrollPane.setBackground(Background.EMPTY);
      scrollPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      scrollPane.setPadding(Insets.EMPTY);
      AnchorPane anchorPane = new AnchorPane();
      anchorPane.setBackground(Background.EMPTY);
      anchorPane.setBorder(Border.EMPTY);
      anchorPane.setPadding(Insets.EMPTY);
      anchorPane.getChildren().add(scrollPane);
      AnchorPane.setTopAnchor(scrollPane, (double)0.0F);
      AnchorPane.setBottomAnchor(scrollPane, (double)0.0F);
      AnchorPane.setLeftAnchor(scrollPane, (double)0.0F);
      AnchorPane.setRightAnchor(scrollPane, (double)0.0F);
      Scene scene = new Scene(anchorPane);
      this.canvas.setScene(scene);
      this.chatDialog.show(scrollPane);
   }

   public void setFocus() {
      this.canvas.setFocus();
   }

   public void dispose() {
      if (this.chatDialog != null) {
         this.chatDialog.hide();
      }

      super.dispose();
   }
}
