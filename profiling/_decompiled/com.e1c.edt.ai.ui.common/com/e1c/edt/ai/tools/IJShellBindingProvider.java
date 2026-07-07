package com.e1c.edt.ai.tools;

import java.util.Collection;
import java.util.Map;

public interface IJShellBindingProvider {
   Map<String, JShellBindingDescription> getBindings();

   String getDescription();

   Collection<Class<?>> getSignificantClasses();

   Collection<String> getImports();

   String getUseCases();
}
