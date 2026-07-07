package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class LineTrimmedReplacer implements IReplacementStrategy {
   private final IReplacements replacements;

   @Inject
   public LineTrimmedReplacer(IReplacements replacements) {
      Preconditions.checkNotNull(replacements);
      this.replacements = replacements;
   }

   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      String[] originalLines = this.replacements.splitLines(content);
      String[] searchLines = this.replacements.removeTrailingEmptyLine(this.replacements.splitLines(find));
      if (searchLines.length == 0) {
         return matches;
      } else {
         for(int i = 0; i <= originalLines.length - searchLines.length; ++i) {
            boolean isMatch = true;

            for(int j = 0; j < searchLines.length; ++j) {
               if (!originalLines[i + j].trim().equals(searchLines[j].trim())) {
                  isMatch = false;
                  break;
               }
            }

            if (isMatch) {
               int endLine = i + searchLines.length - 1;
               matches.add(this.replacements.blockByLineRange(content, originalLines, i, endLine));
            }
         }

         return matches;
      }
   }

   public int getOrdinal() {
      return 1;
   }
}
