package com.e1c.edt.ai.tools;

import java.util.Collections;

public class SimpleReplacer implements IReplacementStrategy {
   public Iterable<String> findCandidates(String content, String find) {
      return Collections.singletonList(find);
   }

   public int getOrdinal() {
      return 0;
   }
}
