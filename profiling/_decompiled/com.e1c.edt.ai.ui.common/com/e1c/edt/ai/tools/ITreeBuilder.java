package com.e1c.edt.ai.tools;

public interface ITreeBuilder {
   void addDirectory(String var1, int var2);

   void addFile(String var1, int var2);

   void endDirectory();

   String build();
}
