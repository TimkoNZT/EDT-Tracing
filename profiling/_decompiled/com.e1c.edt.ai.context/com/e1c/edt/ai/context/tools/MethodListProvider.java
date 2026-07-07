package com.e1c.edt.ai.context.tools;

import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class MethodListProvider implements IMethodListProvider {
   public List<String> getPublicMethodSignatures(Class<?> clazz) {
      Method[] methods = clazz.getMethods();
      return (List)Arrays.stream(methods).filter((method) -> method.getName().startsWith("create") || method.getName().startsWith("get")).map(this::buildMethodSignature).distinct().sorted().collect(Collectors.toList());
   }

   private String buildMethodSignature(Method method) {
      StringBuilder signature = new StringBuilder();
      signature.append(method.getName()).append("(");
      Parameter[] parameters = method.getParameters();

      for(int i = 0; i < parameters.length; ++i) {
         if (i > 0) {
            signature.append(", ");
         }

         Parameter param = parameters[i];
         String typeName = param.getType().getSimpleName();
         signature.append(typeName);
      }

      signature.append(")");
      return signature.toString();
   }
}
