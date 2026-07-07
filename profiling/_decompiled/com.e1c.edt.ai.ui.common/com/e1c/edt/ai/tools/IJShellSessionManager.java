package com.e1c.edt.ai.tools;

public interface IJShellSessionManager {
   IJShellSession getOrCreateSession(int var1);

   IJShellSession getSession(int var1);

   void invalidateSession(int var1);

   void invalidateAll();
}
