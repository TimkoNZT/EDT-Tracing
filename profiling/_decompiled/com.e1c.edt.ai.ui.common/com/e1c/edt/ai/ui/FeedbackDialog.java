package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.assistent.model.IssueType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

public class FeedbackDialog extends TitleAreaDialog implements IFeedbackDialog {
   public static final String BOUNDS_STORE_KEY = "FeedbackDialogBounds";
   private final ISettingsStore settingsStore;
   private Button attachCodeCompletionCheckbox;
   private List issueTypeList;
   private StyledText issueDescriptionText;
   private boolean hasCodeCompletion;
   private IssueType issueType;
   private String issueDescription;

   @Inject
   public FeedbackDialog(IUI ui, ISettingsStore settingsStore) {
      super((Shell)ui.getShell().get());
      Preconditions.checkNotNull(settingsStore);
      this.settingsStore = settingsStore;
   }

   public int show() {
      this.create();
      return this.open();
   }

   public void create() {
      super.create();
      this.setTitle(Messages.FeedbackDialogTitle);
      this.setMessage(Messages.FeedbackDialogMessage, 0);
   }

   protected void configureShell(Shell shell) {
      super.configureShell(shell);
      shell.setText(Messages.FeedbackDialogBoxTitle);
      this.settingsStore.getValue("FeedbackDialogBounds", Rectangle.class).ifPresent((bounds) -> shell.setBounds(bounds));
   }

   public boolean close() {
      this.settingsStore.setValue("FeedbackDialogBounds", this.getShell().getBounds());
      return super.close();
   }

   protected Control createDialogArea(Composite parent) {
      Composite area = (Composite)super.createDialogArea(parent);
      Composite container = new Composite(area, 0);
      container.setLayoutData(new GridData(4, 4, true, true));
      GridLayout layout = new GridLayout(2, false);
      layout.marginWidth = 10;
      container.setLayout(layout);
      new Label(container, 0);
      GridData attachCodeCompletionGrid = new GridData();
      attachCodeCompletionGrid.grabExcessHorizontalSpace = true;
      attachCodeCompletionGrid.horizontalAlignment = 4;
      this.attachCodeCompletionCheckbox = new Button(container, 32);
      this.attachCodeCompletionCheckbox.setText(Messages.FeedbackDialogRefersToCodeCompletion);
      this.attachCodeCompletionCheckbox.setLayoutData(attachCodeCompletionGrid);
      this.attachCodeCompletionCheckbox.setFocus();
      this.attachCodeCompletionCheckbox.setEnabled(this.hasCodeCompletion);
      Label issueTypeLabel = new Label(container, 0);
      issueTypeLabel.setText(Messages.FeedbackDialogIssueType);
      GridData issueTypeLabelGrid = new GridData();
      issueTypeLabelGrid.verticalAlignment = 1;
      issueTypeLabel.setLayoutData(issueTypeLabelGrid);
      GridData issueTypeGrid = new GridData();
      issueTypeGrid.grabExcessHorizontalSpace = true;
      issueTypeGrid.horizontalAlignment = 4;
      this.issueTypeList = new List(container, 2052);
      this.issueTypeList.setLayoutData(issueTypeGrid);

      IssueType[] var13;
      for(IssueType type : var13 = IssueType.values()) {
         this.issueTypeList.add(type.Title, type.Index);
      }

      this.issueTypeList.select(IssueType.Undefined.Index);
      Label issueDescriptionLabel = new Label(container, 0);
      issueDescriptionLabel.setText(Messages.FeedbackDialogDescription);
      GridData issueDescriptionLabelGrid = new GridData();
      issueDescriptionLabelGrid.verticalAlignment = 1;
      issueDescriptionLabel.setLayoutData(issueDescriptionLabelGrid);
      GridData issueDescriptionGrid = new GridData();
      issueDescriptionGrid.grabExcessHorizontalSpace = true;
      issueDescriptionGrid.horizontalAlignment = 4;
      issueDescriptionGrid.grabExcessVerticalSpace = true;
      issueDescriptionGrid.verticalAlignment = 4;
      issueDescriptionGrid.heightHint = 100;
      this.issueDescriptionText = new StyledText(container, 2816);
      this.issueDescriptionText.setAlwaysShowScrollBars(false);
      this.issueDescriptionText.setLayoutData(issueDescriptionGrid);
      return area;
   }

   protected boolean isResizable() {
      return true;
   }

   private void saveData() {
      this.issueType = IssueType.Undefined;

      IssueType[] var4;
      for(IssueType type : var4 = IssueType.values()) {
         if (this.issueTypeList.getSelectionIndex() == type.Index) {
            this.issueType = type;
            break;
         }
      }

      this.issueDescription = this.issueDescriptionText.getText();
   }

   protected void okPressed() {
      this.saveData();
      super.okPressed();
   }

   public void setHasCodeCompletion(boolean hasCodeCompletion) {
      this.hasCodeCompletion = hasCodeCompletion;
   }

   public IssueType getIssueType() {
      return this.issueType;
   }

   public String getIssueDescription() {
      return this.issueDescription;
   }
}
