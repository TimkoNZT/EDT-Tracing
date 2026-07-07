package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Reflection implements IReflection {
   private final ILog log;
   private final ConcurrentMap<MemberKey, Optional<Field>> fields = new ConcurrentHashMap();
   private final ConcurrentMap<MemberKey, Optional<Method>> methods = new ConcurrentHashMap();

   @Inject
   public Reflection(ILog log) {
      Preconditions.checkNotNull(log);
      this.log = log;
   }

   public <T, R> Optional<R> getField(Class<T> classOfT, Object target, String fieldName, Class<R> classOfR) {
      Preconditions.checkNotNull(target);
      Preconditions.checkNotNull(fieldName);
      return ((Optional)this.fields.computeIfAbsent(new MemberKey(classOfT, fieldName), (k) -> this.getField(classOfT, fieldName))).map((field) -> {
         try {
            Object result = field.get(target);
            return !this.checkAssignable(result, classOfR) ? null : classOfR.cast(result);
         } catch (IllegalAccessException | IllegalArgumentException error) {
            this.log.logError(error);
            return null;
         }
      });
   }

   public <T, R> Optional<R> callMethod(Class<T> classOfT, Object target, String methodName, Class<R> classOfR, Object... args) {
      Preconditions.checkNotNull(target);
      Preconditions.checkNotNull(methodName);
      return ((Optional)this.methods.computeIfAbsent(new MemberKey(classOfT, methodName), (k) -> this.getMethod(classOfT, methodName))).map((method) -> {
         try {
            Object result = method.invoke(target, args);
            return !this.checkAssignable(result, classOfR) ? null : classOfR.cast(result);
         } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException error) {
            this.log.logError(error);
            return null;
         }
      });
   }

   private <R> boolean checkAssignable(Object result, Class<R> classOfR) {
      Class<?> resultClazz = result.getClass();
      if (classOfR.isAssignableFrom(resultClazz)) {
         return true;
      } else {
         this.log.logError(classOfR.getSimpleName() + " is not assignable from " + resultClazz.getSimpleName());
         return false;
      }
   }

   private <T> Optional<Field> getField(Class<T> classOfT, String fieldName) {
      Field field;
      try {
         field = classOfT.getDeclaredField(fieldName);
         if (field != null) {
            field.setAccessible(true);
         }
      } catch (Exception error) {
         this.log.logError(error);
         return Optional.empty();
      }

      return Optional.ofNullable(field);
   }

   private <T> Optional<Method> getMethod(Class<T> classOfT, String methodName) {
      Method method;
      try {
         method = classOfT.getDeclaredMethod(methodName);
         if (method != null) {
            method.setAccessible(true);
         }
      } catch (Exception error) {
         this.log.logError(error);
         return Optional.empty();
      }

      return Optional.ofNullable(method);
   }

   private class MemberKey {
      public final Class<?> classOfT;
      public final String fieldName;

      public MemberKey(Class<?> classOfT, String fieldName) {
         this.classOfT = classOfT;
         this.fieldName = fieldName;
      }

      public int hashCode() {
         return this.classOfT.hashCode() ^ this.fieldName.hashCode();
      }

      public boolean equals(Object obj) {
         if (obj == null) {
            return false;
         } else if (obj == this) {
            return true;
         } else if (obj.getClass() != MemberKey.class) {
            return false;
         } else {
            MemberKey other = (MemberKey)obj;
            return this.classOfT.equals(other.classOfT) && this.fieldName.equals(other.fieldName);
         }
      }
   }
}
