package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IWeb;
import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class TestReportDialog extends ErrorDialog {
   private static final int EXPLORE_ID = 11;
   private static final int EXPORT_LOG_ID = 12;
   @Inject
   private IWeb web;
   private final String url;
   private final String supportLogText;

   public TestReportDialog(Shell parentShell, String dialogTitle, String message, IStatus status, int displayMask, String url, String supportLogText) {
      super(parentShell, dialogTitle, message, status, displayMask);
      BaseActivator.injectMembers(this);
      this.url = url;
      this.supportLogText = supportLogText;
   }

   protected Control createButtonBar(Composite parent) {
      Composite bar = new Composite(parent, 0);
      bar.setLayoutData(new GridData(4, 4, true, false));
      GridLayout layout = new GridLayout(1, false);
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.verticalSpacing = 6;
      bar.setLayout(layout);
      Composite buttonsRow = new Composite(bar, 0);
      buttonsRow.setLayoutData(new GridData(131072, 16777216, true, false));
      GridLayout buttonsRowLayout = new GridLayout(3, false);
      buttonsRow.setLayout(buttonsRowLayout);
      this.createButton(buttonsRow, 11, Messages.TestReportDialog_HowToFix_Open, false);
      this.createButton(buttonsRow, 0, IDialogConstants.OK_LABEL, true);
      this.createButton(buttonsRow, 12, Messages.TestReportDialog_ExportLog_Button, false);
      return bar;
   }

   protected void buttonPressed(int id) {
      if (id == 11) {
         if (this.url != null && !this.url.isBlank()) {
            this.web.browse(this.url);
         }

      } else if (id == 12) {
         this.exportSupportLog();
      } else {
         super.buttonPressed(id);
      }
   }

   private void exportSupportLog() {
      Shell shell = this.getShell();
      if (shell != null && !shell.isDisposed()) {
         FileDialog fd = new FileDialog(shell, 8192);
         fd.setText(Messages.TestReportDialog_ExportLog_Title);
         fd.setFilterExtensions(new String[]{"*.log"});
         fd.setFilterNames(new String[]{"Log file (*.log)"});
         fd.setOverwrite(true);
         fd.setFileName("diagnostics.log");
         String pathStr = fd.open();
         if (pathStr != null && !pathStr.isBlank()) {
            try {
               Files.writeString(Path.of(pathStr), this.supportLogText == null ? "" : this.supportLogText, StandardCharsets.UTF_8);
            } catch (Exception e) {
               MessageDialog.openError(shell, Messages.TestReportDialog_Error, Messages.TestReportDialog_ExportLog_Error + e.getMessage());
            }

         }
      }
   }
}
