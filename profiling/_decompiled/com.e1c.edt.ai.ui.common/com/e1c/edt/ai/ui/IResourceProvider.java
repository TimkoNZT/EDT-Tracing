package com.e1c.edt.ai.ui;

import java.util.Optional;

public interface IResourceProvider {
   String PROMTS_GIT_COMMIT = "prompts/git_commit.txt";
   String SUGGEST_YOUR_OPTION = "prompts/suggest_your_option.txt";
   String CORRECT_ERRORS = "prompts/correct_errors.txt";
   String IN_OTHER_WORDS = "prompts/in_other_words.txt";
   String IMPROVE_STYLE = "prompts/improve_style.txt";

   Optional<String> getTextResource(String var1);
}
