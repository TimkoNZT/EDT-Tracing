package com.e1c.edt.ai.tools;

import java.util.List;

public interface IJShellSession {
   int getSessionId();

   JShellExecutionResult execute(String var1);

   List<String> getExecutionHistory();

   SessionResult getSessionResult();

   void close();
}
