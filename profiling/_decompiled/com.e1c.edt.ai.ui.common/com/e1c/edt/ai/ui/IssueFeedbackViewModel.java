package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICodeCompletionStatistics;
import com.e1c.edt.ai.assistent.IFeedbackService;
import com.e1c.edt.ai.assistent.model.IssueType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;

class IssueFeedbackViewModel implements IIssueFeedbackViewModel {
   private final IFeedbackDialog feedbackDialog;
   private final ICodeCompletionStatistics codeCompletionStatistics;
   private final IFeedbackService feedbackService;

   @Inject
   public IssueFeedbackViewModel(IFeedbackDialog feedbackDialog, ICodeCompletionStatistics codeCompletionStatistics, IFeedbackService feedbackService) {
      Preconditions.checkNotNull(feedbackDialog);
      Preconditions.checkNotNull(codeCompletionStatistics);
      Preconditions.checkNotNull(feedbackService);
      this.feedbackDialog = feedbackDialog;
      this.codeCompletionStatistics = codeCompletionStatistics;
      this.feedbackService = feedbackService;
   }

   public void getFeedback() {
      Optional<String> lastAcceptedSourceId = this.codeCompletionStatistics.getLastAcceptedSourceId();
      this.feedbackDialog.setHasCodeCompletion(lastAcceptedSourceId.isPresent());
      if (this.feedbackDialog.show() == 0) {
         IssueType issueType = this.feedbackDialog.getIssueType();
         String issueDescription = this.feedbackDialog.getIssueDescription();
         this.feedbackService.issueAsync((String)lastAcceptedSourceId.orElse((Object)null), issueType, issueDescription);
      }
   }
}
