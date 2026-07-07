package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CodeCompletionAction;
import org.eclipse.swt.events.VerifyEvent;

interface IUserActions {
   String getCodeCompletionLabels(char var1);

   CodeCompletionAction getAction(VerifyEvent var1);
}
