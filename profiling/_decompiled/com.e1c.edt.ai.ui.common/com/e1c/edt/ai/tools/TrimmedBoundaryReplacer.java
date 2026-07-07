package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TrimmedBoundaryReplacer implements IReplacementStrategy {
   private final IReplacements replacements;

   @Inject
   public TrimmedBoundaryReplacer(IReplacements replacements) {
      Preconditions.checkNotNull(replacements);
      this.replacements = replacements;
   }

   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      String trimmedFind = find.trim();
      if (trimmedFind.equals(find)) {
         return matches;
      } else {
         if (content.contains(trimmedFind)) {
            matches.add(trimmedFind);
         }

         String[] lines = this.replacements.splitLines(content);
         String[] findLines = this.replacements.splitLines(find);

         for(int i = 0; i <= lines.length - findLines.length; ++i) {
            String block = String.join("\n", this.slice(lines, i, i + findLines.length));
            if (block.trim().equals(trimmedFind)) {
               matches.add(block);
            }
         }

         return matches;
      }
   }

   public int getOrdinal() {
      return 6;
   }

   private String[] slice(String[] array, int fromInclusive, int toExclusive) {
      String[] result = new String[toExclusive - fromInclusive];
      System.arraycopy(array, fromInclusive, result, 0, result.length);
      return result;
   }
}
