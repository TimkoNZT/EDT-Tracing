package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.assistent.DiagnosticResult;
import com.e1c.edt.ai.assistent.IDiagnosticContext;
import com.e1c.edt.ai.assistent.IDiagnosticTest;
import org.eclipse.swt.widgets.Shell;

public interface IDiagnosticReportDialogProvider {
   void openErrorDialog(Shell var1, IDiagnosticTest var2, DiagnosticResult var3, IDiagnosticContext var4);
}
