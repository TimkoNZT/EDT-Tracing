package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class EscapeNormalizedReplacer implements IReplacementStrategy {
   private final IReplacements replacements;

   @Inject
   public EscapeNormalizedReplacer(IReplacements replacements) {
      Preconditions.checkNotNull(replacements);
      this.replacements = replacements;
   }

   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      String unescapedFind = this.unescape(find);
      if (content.contains(unescapedFind)) {
         matches.add(unescapedFind);
      }

      String[] lines = this.replacements.splitLines(content);
      String[] findLines = this.replacements.splitLines(unescapedFind);

      for(int i = 0; i <= lines.length - findLines.length; ++i) {
         String block = String.join("\n", this.slice(lines, i, i + findLines.length));
         if (this.unescape(block).equals(unescapedFind)) {
            matches.add(block);
         }
      }

      return matches;
   }

   public int getOrdinal() {
      return 5;
   }

   private String unescape(String text) {
      StringBuilder result = new StringBuilder(text.length());

      for(int i = 0; i < text.length(); ++i) {
         char current = text.charAt(i);
         if (current == '\\' && i + 1 < text.length()) {
            char next = text.charAt(i + 1);
            Character mapped = this.mapEscape(next);
            if (mapped != null) {
               result.append(mapped);
               ++i;
               continue;
            }
         }

         result.append(current);
      }

      return result.toString();
   }

   private Character mapEscape(char c) {
      switch (c) {
         case '\n':
            return '\n';
         case '"':
            return '"';
         case '$':
            return '$';
         case '\'':
            return '\'';
         case '\\':
            return '\\';
         case '`':
            return '`';
         case 'n':
            return '\n';
         case 'r':
            return '\r';
         case 't':
            return '\t';
         default:
            return null;
      }
   }

   private String[] slice(String[] array, int fromInclusive, int toExclusive) {
      String[] result = new String[toExclusive - fromInclusive];
      System.arraycopy(array, fromInclusive, result, 0, result.length);
      return result;
   }
}
