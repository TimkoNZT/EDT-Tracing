package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ui.Messages;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class FixDialog extends TitleAreaDialog implements IFixDialog {
   public static final String BOUNDS_STORE_KEY = "FixDialogBounds";
   private final IPreferenceStore preferenceStore;
   private final IJson json;
   private Text detailsText;
   private String details;

   @Inject
   public FixDialog(IPreferenceStore preferenceStore, IJson json) {
      super(Display.getCurrent().getActiveShell());
      this.details = Messages.FixCodeDefaultDetails;
      Preconditions.checkNotNull(preferenceStore);
      Preconditions.checkNotNull(json);
      this.preferenceStore = preferenceStore;
      this.json = json;
   }

   public int show() {
      this.create();
      return this.open();
   }

   public void create() {
      super.create();
      this.setMessage(Messages.FixCodeRequestDetails);
   }

   protected void configureShell(Shell shell) {
      super.configureShell(shell);
      shell.setText(Messages.AIName);
      String boundsStr = this.preferenceStore.getString("FixDialogBounds");
      if (boundsStr != null) {
         this.json.deserialize(boundsStr, Rectangle.class).ifPresent((bounds) -> shell.setBounds(bounds));
      }

   }

   public boolean close() {
      this.preferenceStore.setValue("FixDialogBounds", this.json.serialize(this.getShell().getBounds()));
      return super.close();
   }

   protected Control createDialogArea(Composite parent) {
      Composite area = (Composite)super.createDialogArea(parent);
      Composite container = new Composite(area, 0);
      container.setLayoutData(new GridData(4, 4, true, true));
      GridLayout layout = new GridLayout(1, false);
      layout.marginWidth = 10;
      container.setLayout(layout);
      GridData detailsTextGrid = new GridData();
      detailsTextGrid.grabExcessHorizontalSpace = true;
      detailsTextGrid.grabExcessHorizontalSpace = true;
      detailsTextGrid.horizontalAlignment = 4;
      detailsTextGrid.grabExcessVerticalSpace = true;
      detailsTextGrid.verticalAlignment = 4;
      this.detailsText = new Text(container, 2050);
      this.detailsText.setText(this.details);
      this.detailsText.setLayoutData(detailsTextGrid);
      this.detailsText.setFocus();
      this.detailsText.selectAll();
      return area;
   }

   protected boolean isResizable() {
      return true;
   }

   private void saveData() {
      this.details = this.detailsText.getText();
   }

   protected void okPressed() {
      this.saveData();
      super.okPressed();
   }

   public String getDetails() {
      return this.details;
   }
}
