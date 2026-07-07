package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.ITokenCheck;
import com.e1c.edt.ai.ui.IDispatcher;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class TokenFieldEditor extends ValidatingStringFieldEditor {
   @Inject
   private ILog log;
   @Inject
   private ITokenCheck tokenCheck;
   @Inject
   private IStateService stateService;
   @Inject
   private IDispatcher dispatcher;
   private Button validateButton;

   public TokenFieldEditor(String name, String labelText, Composite parent, IValidator<String> validator) {
      super(name, labelText, parent, validator);
      this.setEmptyStringAllowed(true);
   }

   protected void doFillIntoGrid(Composite parent, int numColumns) {
      this.getLabelControl(parent);
      Text textControl = this.getTextControl(parent);
      GridData gridData = new GridData(4, 16777216, true, false);
      gridData.horizontalSpan = numColumns - 2;
      textControl.setLayoutData(gridData);
      textControl.setFont(parent.getFont());
      this.validateButton = new Button(parent, 8);
      this.validateButton.setText(Messages.TokenFieldEditor_Validate);
      this.validateButton.setLayoutData(new GridData(4, 16777216, false, false));
      this.validateButton.setEnabled(true);
      this.validateButton.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            TokenFieldEditor.this.validateToken();
         }
      });
   }

   public int getNumberOfControls() {
      return 3;
   }

   protected void adjustForNumColumns(int numColumns) {
      Label labelControl = this.getLabelControl();
      GridData gridData = (GridData)labelControl.getLayoutData();
      if (gridData == null) {
         gridData = new GridData();
         labelControl.setLayoutData(gridData);
      }

      gridData.horizontalSpan = 1;
      Text textControl = this.getTextControl();
      gridData = (GridData)textControl.getLayoutData();
      if (gridData == null) {
         gridData = new GridData();
         textControl.setLayoutData(gridData);
      }

      gridData.horizontalSpan = numColumns - 2;
   }

   public boolean doCheckState() {
      return super.doCheckState();
   }

   private void validateToken() {
      if (!this.doCheckState()) {
         MessageDialog.openError(this.validateButton.getShell(), Messages.TokenFieldEditor_ValidationError, this.getErrorMessage());
      } else {
         if (this.stateService != null) {
            this.stateService.setState(ServiceState.SETTINGS_CHANGED);
         }

         this.validateButton.setEnabled(false);
         if (this.tokenCheck != null) {
            String token = this.getStringValue();
            CompletableFuture.runAsync(() -> {
               boolean var6 = false;

               label56: {
                  try {
                     try {
                        var6 = true;
                        Boolean isValid = (Boolean)this.tokenCheck.checkTokenAsync(token).get();
                        this.dispatcher.dispatchAsync(() -> {
                           if (isValid) {
                              MessageDialog.openInformation(this.validateButton.getShell(), Messages.TokenFieldEditor_ValidationSuccess, Messages.TokenFieldEditor_TokenValid);
                           } else {
                              MessageDialog.openError(this.validateButton.getShell(), Messages.TokenFieldEditor_ValidationError, Messages.TokenFieldEditor_TokenInvalid);
                           }

                        });
                        var6 = false;
                        break label56;
                     } catch (Exception ex) {
                        if (this.log != null) {
                           this.log.logError("Token validation failed: " + ex.getMessage());
                        }
                     }

                     this.dispatcher.dispatchAsync(() -> MessageDialog.openError(this.validateButton.getShell(), Messages.TokenFieldEditor_ValidationError, Messages.TokenFieldEditor_TokenInvalid));
                     var6 = false;
                  } finally {
                     if (var6) {
                        this.dispatcher.dispatchAsync(() -> this.validateButton.setEnabled(true));
                     }
                  }

                  this.dispatcher.dispatchAsync(() -> this.validateButton.setEnabled(true));
                  return;
               }

               this.dispatcher.dispatchAsync(() -> this.validateButton.setEnabled(true));
            });
         } else {
            MessageDialog.openError(this.validateButton.getShell(), Messages.TokenFieldEditor_ValidationError, Messages.TokenFieldEditor_TokenInvalid);
            this.validateButton.setEnabled(true);
         }

      }
   }
}
