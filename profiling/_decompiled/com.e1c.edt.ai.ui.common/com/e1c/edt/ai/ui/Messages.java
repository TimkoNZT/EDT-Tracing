package com.e1c.edt.ai.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
   private static final String BUNDLE_NAME = "com.e1c.edt.ai.ui.messages";
   public static String AIName;
   public static String Activation;
   public static String CodeCompletionJobName;
   public static String BackgroundJobName;
   public static String CodeCompletionBackgroundScanSubtaskName;
   public static String CodeCompletionBackgroundHashSubtaskName;
   public static String CodeCompletionBackgroundSyncSubtaskName;
   public static String ChatInteractionJobName;
   public static String FeedbackDialogBoxTitle;
   public static String FeedbackDialogTitle;
   public static String FeedbackDialogMessage;
   public static String FeedbackDialogRefersToCodeCompletion;
   public static String FeedbackDialogIssueType;
   public static String FeedbackDialogDescription;
   public static String ReplaceCode;
   public static String FixCodeRequestDetails;
   public static String FixCodeDefaultDetails;
   public static String HintHotKey_AcceptBlock;
   public static String HintHotKey_AcceptLine;
   public static String HintHotKey_AcceptAll;
   public static String HintHotKey_AcceptBack;
   public static String HintHotKey_AcceptStop;
   public static String NotActivated;
   public static String Support;
   public static String UpdateButton;
   public static String RestartButton;
   public static String CloseButton;
   public static String UpdateLink;
   public static String UpdateMessage;
   public static String UpdatePluginJob;
   public static String UpdateJobMessage;
   public static String UpdateInstalled;
   public static String UpdateError;
   public static String RestartJob;
   public static String CommitMessage;
   public static String GitReview;
   public static String ErrorReadingTextFile;
   public static String AddFilesToChatDialogName;
   public static String SuggestYourOption;
   public static String CorrectErrors;
   public static String InOtherWords;
   public static String ImproveStyle;
   public static String Activate;
   public static String Diagnostics;
   public static String Details;
   public static String ChatLoadingTitle;
   public static String ChatLoadingMessage;
   public static String FileNotText;
   public static String ErrorReadingFile;
   public static String FileNotTextFormat;

   static {
      NLS.initializeMessages("com.e1c.edt.ai.ui.messages", Messages.class);
   }

   private Messages() {
   }
}
