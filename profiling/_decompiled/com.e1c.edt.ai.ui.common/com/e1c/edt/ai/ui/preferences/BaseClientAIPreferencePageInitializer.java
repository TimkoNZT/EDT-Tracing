package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.ui.BaseActivator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class BaseClientAIPreferencePageInitializer extends AbstractPreferenceInitializer {
   public void initializeDefaultPreferences() {
      IPreferenceStore store = BaseActivator.getDefault().getPreferenceStore();
      store.setDefault("stringPreferenceCodeCompletionPolicy", CodeCompletionPolicy.MODERATE.getId());
      store.setDefault("stringPreferenceClientID", "");
      store.setDefault("stringPreferenceLLMParameters", "");
      store.setDefault("stringPreferenceCodeCompletionLinesCount", 5);
   }
}
