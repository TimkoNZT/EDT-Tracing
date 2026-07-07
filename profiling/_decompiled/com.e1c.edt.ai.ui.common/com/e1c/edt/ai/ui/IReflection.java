package com.e1c.edt.ai.ui;

import java.util.Optional;

public interface IReflection {
   <T, R> Optional<R> getField(Class<T> var1, Object var2, String var3, Class<R> var4);

   <T, R> Optional<R> callMethod(Class<T> var1, Object var2, String var3, Class<R> var4, Object... var5);
}
