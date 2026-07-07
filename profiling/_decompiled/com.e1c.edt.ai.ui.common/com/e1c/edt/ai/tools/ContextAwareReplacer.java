package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ContextAwareReplacer implements IReplacementStrategy {
   private final IReplacements replacements;

   @Inject
   public ContextAwareReplacer(IReplacements replacements) {
      Preconditions.checkNotNull(replacements);
      this.replacements = replacements;
   }

   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      String[] findLines = this.replacements.splitLines(find);
      if (findLines.length < 3) {
         return matches;
      } else {
         findLines = this.replacements.removeTrailingEmptyLine(findLines);
         if (findLines.length == 0) {
            return matches;
         } else {
            String[] contentLines = this.replacements.splitLines(content);
            String firstLine = findLines[0].trim();
            String lastLine = findLines[findLines.length - 1].trim();

            for(int i = 0; i < contentLines.length; ++i) {
               if (contentLines[i].trim().equals(firstLine)) {
                  for(int j = i + 2; j < contentLines.length; ++j) {
                     if (contentLines[j].trim().equals(lastLine)) {
                        String[] blockLines = this.slice(contentLines, i, j + 1);
                        if (blockLines.length == findLines.length && this.hasReasonableSimilarity(blockLines, findLines)) {
                           matches.add(String.join("\n", blockLines));
                        }
                        break;
                     }
                  }
               }
            }

            return matches;
         }
      }
   }

   public int getOrdinal() {
      return 7;
   }

   private boolean hasReasonableSimilarity(String[] blockLines, String[] findLines) {
      int matchingLines = 0;
      int totalNonEmptyLines = 0;

      for(int i = 1; i < blockLines.length - 1; ++i) {
         String blockLine = blockLines[i].trim();
         String findLine = findLines[i].trim();
         if (!blockLine.isEmpty() || !findLine.isEmpty()) {
            ++totalNonEmptyLines;
            if (blockLine.equals(findLine)) {
               ++matchingLines;
            }
         }
      }

      if (totalNonEmptyLines != 0 && !((double)matchingLines / (double)totalNonEmptyLines >= (double)0.5F)) {
         return false;
      } else {
         return true;
      }
   }

   private String[] slice(String[] array, int fromInclusive, int toExclusive) {
      String[] result = new String[toExclusive - fromInclusive];
      System.arraycopy(array, fromInclusive, result, 0, result.length);
      return result;
   }
}
