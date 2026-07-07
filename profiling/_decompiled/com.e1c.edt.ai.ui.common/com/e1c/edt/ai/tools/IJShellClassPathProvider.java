package com.e1c.edt.ai.tools;

import jdk.jshell.JShell;

public interface IJShellClassPathProvider {
   void addClassPathFor(JShell var1, Class<?> var2);

   void addAllBundleClassPaths(JShell var1);
}
