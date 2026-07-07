package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndentationFlexibleReplacer implements IReplacementStrategy {
   private static final Pattern LEADING_WHITESPACE_PATTERN = Pattern.compile("^(\\s*)");
   private final IReplacements replacements;

   @Inject
   public IndentationFlexibleReplacer(IReplacements replacements) {
      Preconditions.checkNotNull(replacements);
      this.replacements = replacements;
   }

   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      String normalizedFind = this.removeIndentation(find);
      String[] contentLines = this.replacements.splitLines(content);
      String[] findLines = this.replacements.splitLines(find);

      for(int i = 0; i <= contentLines.length - findLines.length; ++i) {
         String block = String.join("\n", this.slice(contentLines, i, i + findLines.length));
         if (this.removeIndentation(block).equals(normalizedFind)) {
            matches.add(block);
         }
      }

      return matches;
   }

   public int getOrdinal() {
      return 4;
   }

   private String removeIndentation(String text) {
      String[] lines = this.replacements.splitLines(text);
      int minIndent = Integer.MAX_VALUE;
      boolean hasNonEmptyLines = false;

      for(String line : lines) {
         if (!line.trim().isEmpty()) {
            hasNonEmptyLines = true;
            Matcher matcher = LEADING_WHITESPACE_PATTERN.matcher(line);
            int indentLength = 0;
            if (matcher.find()) {
               indentLength = matcher.group(1).length();
            }

            minIndent = Math.min(minIndent, indentLength);
         }
      }

      if (!hasNonEmptyLines) {
         return text;
      } else {
         String[] normalized = new String[lines.length];

         for(int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
               normalized[i] = line;
            } else {
               int from = Math.min(minIndent, line.length());
               normalized[i] = line.substring(from);
            }
         }

         return String.join("\n", normalized);
      }
   }

   private String[] slice(String[] array, int fromInclusive, int toExclusive) {
      String[] result = new String[toExclusive - fromInclusive];
      System.arraycopy(array, fromInclusive, result, 0, result.length);
      return result;
   }
}
