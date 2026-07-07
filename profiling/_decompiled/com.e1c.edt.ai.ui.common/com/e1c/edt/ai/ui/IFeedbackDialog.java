package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.assistent.model.IssueType;

interface IFeedbackDialog {
   int show();

   void setHasCodeCompletion(boolean var1);

   IssueType getIssueType();

   String getIssueDescription();
}
