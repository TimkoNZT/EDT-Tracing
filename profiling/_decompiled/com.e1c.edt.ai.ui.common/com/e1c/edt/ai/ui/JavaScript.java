package com.e1c.edt.ai.ui;

import java.util.HashMap;

class JavaScript implements IJavaScript {
   private static HashMap<Character, String> SpecialChars = new HashMap();

   static {
      SpecialChars.put(' ', " ");
      SpecialChars.put('\r', "\\r");
      SpecialChars.put('\n', "\\n");
      SpecialChars.put('\t', "\\t");
      SpecialChars.put('^', "\\^");
      SpecialChars.put('$', "\\$");
      SpecialChars.put('\\', "\\\\");
      SpecialChars.put('.', "\\.");
      SpecialChars.put('*', "\\*");
      SpecialChars.put('+', "\\+");
      SpecialChars.put('?', "\\?");
      SpecialChars.put('(', "\\(");
      SpecialChars.put(')', "\\)");
      SpecialChars.put('[', "\\[");
      SpecialChars.put(']', "\\]");
      SpecialChars.put('{', "\\{");
      SpecialChars.put('}', "\\}");
      SpecialChars.put('|', "\\|");
      SpecialChars.put('/', "\\/");
   }

   public String escape(String text, String defaultValue) {
      if (text != null && !text.isEmpty()) {
         StringBuilder str = new StringBuilder(2 + text.length() * 4);
         str.append('`');

         int[] var7;
         for(int ch : var7 = text.chars().toArray()) {
            if (Character.isLetterOrDigit(ch)) {
               str.append((char)ch);
            } else {
               String chText = (String)SpecialChars.get((char)ch);
               if (chText != null) {
                  str.append(chText);
               } else {
                  str.append(String.format("\\u%04X", ch));
               }
            }
         }

         str.append('`');
         return str.toString();
      } else {
         return defaultValue;
      }
   }
}
