package com.e1c.edt.ai.tools;

public interface IReplacements {
   String[] splitLines(String var1);

   String[] removeTrailingEmptyLine(String[] var1);

   String blockByLineRange(String var1, String[] var2, int var3, int var4);
}
