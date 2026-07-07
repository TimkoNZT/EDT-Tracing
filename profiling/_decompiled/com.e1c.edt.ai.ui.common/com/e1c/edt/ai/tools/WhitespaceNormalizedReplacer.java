package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WhitespaceNormalizedReplacer implements IReplacementStrategy {
   private final IReplacements replacements;

   @Inject
   public WhitespaceNormalizedReplacer(IReplacements replacements) {
      Preconditions.checkNotNull(replacements);
      this.replacements = replacements;
   }

   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      String normalizedFind = this.normalizeWhitespace(find);
      String[] lines = this.replacements.splitLines(content);

      for(String line : lines) {
         String normalizedLine = this.normalizeWhitespace(line);
         if (normalizedLine.equals(normalizedFind)) {
            matches.add(line);
         } else if (normalizedLine.contains(normalizedFind)) {
            String trimmedFind = find.trim();
            if (!trimmedFind.isEmpty()) {
               String[] words = trimmedFind.split("\\s+");
               String pattern = (String)Stream.of(words).map(Pattern::quote).collect(Collectors.joining("\\s+"));

               try {
                  Matcher matcher = Pattern.compile(pattern).matcher(line);
                  if (matcher.find()) {
                     matches.add(matcher.group());
                  }
               } catch (PatternSyntaxException var15) {
               }
            }
         }
      }

      String[] findLines = this.replacements.splitLines(find);
      if (findLines.length > 1) {
         for(int i = 0; i <= lines.length - findLines.length; ++i) {
            String block = String.join("\n", this.slice(lines, i, i + findLines.length));
            if (this.normalizeWhitespace(block).equals(normalizedFind)) {
               matches.add(block);
            }
         }
      }

      return matches;
   }

   public int getOrdinal() {
      return 3;
   }

   private String normalizeWhitespace(String text) {
      return text.replaceAll("\\s+", " ").trim();
   }

   private String[] slice(String[] array, int fromInclusive, int toExclusive) {
      String[] result = new String[toExclusive - fromInclusive];
      System.arraycopy(array, fromInclusive, result, 0, result.length);
      return result;
   }
}
