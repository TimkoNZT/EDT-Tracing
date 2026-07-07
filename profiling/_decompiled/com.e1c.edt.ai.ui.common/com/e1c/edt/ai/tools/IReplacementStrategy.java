package com.e1c.edt.ai.tools;

public interface IReplacementStrategy {
   Iterable<String> findCandidates(String var1, String var2);

   int getOrdinal();
}
