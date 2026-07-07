package com.e1c.edt.ai.tools;

import java.util.Set;

public interface IRestrictedTypesProvider {
   Set<String> getRestrictedTypes();

   boolean isRestricted(String var1);
}
