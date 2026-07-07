package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ThemeManager implements IThemeManager {
   public boolean isDarkTheme() {
      return this.isDarkThemeByPreferences();
   }

   private boolean isDarkThemeByPreferences() {
      try {
         IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme");
         String themeId = prefs.get("themeid", "");
         return themeId.toLowerCase().contains("dark");
      } catch (Exception var3) {
         return this.isDarkThemeByColor();
      }
   }

   private boolean isDarkThemeByColor() {
      Color bgColor = Display.getDefault().getSystemColor(22);
      return bgColor.getRed() < 128 && bgColor.getGreen() < 128 && bgColor.getBlue() < 128;
   }
}
