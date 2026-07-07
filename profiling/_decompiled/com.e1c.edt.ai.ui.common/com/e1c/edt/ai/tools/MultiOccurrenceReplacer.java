package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

public class MultiOccurrenceReplacer implements IReplacementStrategy {
   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      if (find.isEmpty()) {
         return matches;
      } else {
         int startIndex = 0;

         while(true) {
            int index = content.indexOf(find, startIndex);
            if (index == -1) {
               return matches;
            }

            matches.add(find);
            startIndex = index + find.length();
         }
      }
   }

   public int getOrdinal() {
      return 8;
   }
}
