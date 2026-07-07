package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IDefaultSettings;

class DefaultSettings implements IDefaultSettings {
   public String getUrl() {
      return "https://code.1c.ai/";
   }

   public String getHomePage() {
      return "https://code.1c.ai/";
   }

   public String getUpdateUrl() {
      return "https://code.1c.ai/plugin/";
   }

   public String getPluginFeature() {
      return "com.e1c.edt.ai.feature.feature.group";
   }
}
