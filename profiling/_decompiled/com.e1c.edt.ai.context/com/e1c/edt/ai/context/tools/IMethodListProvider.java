package com.e1c.edt.ai.context.tools;

import java.util.List;

public interface IMethodListProvider {
   List<String> getPublicMethodSignatures(Class<?> var1);
}
