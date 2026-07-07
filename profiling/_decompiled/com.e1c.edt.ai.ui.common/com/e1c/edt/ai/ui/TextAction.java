package com.e1c.edt.ai.ui;

public enum TextAction {
   SUGGEST_YOUR_OPTION(Messages.SuggestYourOption, "SUGGEST_YOUR_OPTION", "prompts/suggest_your_option.txt"),
   CORRECT_ERRORS(Messages.CorrectErrors, "CORRECT_ERRORS", "prompts/correct_errors.txt"),
   IN_OTHER_WORDS(Messages.InOtherWords, "IN_OTHER_WORDS", "prompts/in_other_words.txt"),
   IMPROVE_STYLE(Messages.ImproveStyle, "IMPROVE_STYLE", "prompts/improve_style.txt");

   public final String title;
   public final String imageName;
   public final String resourceName;

   private TextAction(String title, String imageName, String resourceName) {
      this.title = title;
      this.imageName = imageName;
      this.resourceName = resourceName;
   }
}
