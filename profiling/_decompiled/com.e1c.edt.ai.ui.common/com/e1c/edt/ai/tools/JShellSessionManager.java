package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.jshell.JShell;

@Singleton
class JShellSessionManager implements IJShellSessionManager {
   private static final int MAX_SESSIONS = 64;
   private static final int SESSION_EXPIRY_HOURS = 1;
   private final Cache<Integer, JShellSession> cache;
   private final ILog log;
   private final Set<IJShellBindingProvider> bindingProviders;
   private final IRestrictedTypesValidator restrictedTypesValidator;
   private final IJShellClassPathProvider classPathProvider;
   private final AtomicInteger sessionCounter = new AtomicInteger(0);

   @Inject
   public JShellSessionManager(ILog log, Set<IJShellBindingProvider> bindingProviders, IRestrictedTypesValidator restrictedTypesValidator, IJShellClassPathProvider classPathProvider) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(bindingProviders);
      Preconditions.checkNotNull(restrictedTypesValidator);
      Preconditions.checkNotNull(classPathProvider);
      this.log = log;
      this.bindingProviders = bindingProviders;
      this.restrictedTypesValidator = restrictedTypesValidator;
      this.classPathProvider = classPathProvider;
      this.cache = CacheBuilder.newBuilder().maximumSize(64L).expireAfterAccess(1L, TimeUnit.HOURS).removalListener((notification) -> {
         JShellSession session = (JShellSession)notification.getValue();
         if (session != null) {
            session.close();
         }

      }).build();
   }

   public IJShellSession getOrCreateSession(int sessionId) {
      if (sessionId == 0) {
         return this.createSession();
      } else {
         JShellSession session = (JShellSession)this.cache.getIfPresent(sessionId);
         if (session != null) {
            return session;
         } else {
            throw new ToolException("Session with ID " + sessionId + " not found. Sessions expire after " + 1 + " hour(s) of inactivity. Please create a new session first.", (Throwable)null, ToolErrorType.RETRYABLE);
         }
      }
   }

   public IJShellSession getSession(int sessionId) {
      return sessionId == 0 ? null : (IJShellSession)this.cache.getIfPresent(sessionId);
   }

   private JShellSession createSession() {
      SessionSnapshot snapshot = this.buildSessionSnapshot();
      ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
      ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
      DelegatingClassLoader sessionClassLoader = new DelegatingClassLoader(JShellSessionManager.class.getClassLoader(), snapshot.classLoaders);
      JShellSharedExecutionControlProvider executionControlProvider = new JShellSharedExecutionControlProvider(sessionClassLoader);
      executionControlProvider.setOutputBuffers(outBuffer, errBuffer);
      JShell shell = JShell.builder().executionEngine(executionControlProvider, Map.of()).build();
      String classpath = System.getProperty("java.class.path");
      shell.addToClasspath(classpath);
      this.classPathProvider.addClassPathFor(shell, JShellSessionManager.class);

      for(Collection<Class<?>> significantClasses : snapshot.significantClassesByProvider.values()) {
         for(Class<?> clazz : significantClasses) {
            this.classPathProvider.addClassPathFor(shell, clazz);
         }
      }

      this.classPathProvider.addAllBundleClassPaths(shell);
      int sessionId = this.sessionCounter.incrementAndGet();
      JShellSession session = new JShellSession(sessionId, shell, outBuffer, errBuffer, this.restrictedTypesValidator, this.bindingProviders);

      for(Collection<String> imports : snapshot.importsByProvider.values()) {
         for(String imp : imports) {
            try {
               JShellExecutionResult result = session.execute(imp);
               if (!result.compilationErrors.isEmpty()) {
                  this.log.logError("Failed to import: " + imp);
               }
            } catch (Exception e) {
               this.log.logError("Failed to import package: " + e.getMessage());
            }
         }
      }

      for(Map<String, JShellBindingDescription> bindings : snapshot.bindingsByProvider.values()) {
         for(Map.Entry<String, JShellBindingDescription> entry : bindings.entrySet()) {
            JShellBindingDescription description = (JShellBindingDescription)entry.getValue();
            Object value = description.getValue();
            Class<?> explicitType = description.getExplicitType();
            String varName = (String)entry.getKey();
            if (value != null) {
               Class<?> bindingType = explicitType != null ? explicitType : value.getClass();
               int objectId = JShellObjectBridge.store(session.getSessionId(), value);
               String className = bindingType.getName();
               this.classPathProvider.addClassPathFor(shell, bindingType);
               String bindCode = String.format("%s %s = (%s)com.e1c.edt.ai.tools.JShellObjectBridge.retrieve(%d, %d);", className, varName, className, session.getSessionId(), objectId);
               JShellExecutionResult result = session.execute(bindCode);
               if (!result.compilationErrors.isEmpty() || !result.runtimeErrors.isEmpty()) {
                  StringBuilder errorMessage = new StringBuilder();
                  errorMessage.append("JShell session creation failed, cannot bind: ```java\n");
                  errorMessage.append(bindCode);
                  errorMessage.append("\n```\n");
                  if (!result.compilationErrors.isEmpty()) {
                     errorMessage.append("\nCompilation errors:\n");

                     for(CompilationError error : result.compilationErrors) {
                        errorMessage.append("  - Message: ").append(error.message).append("\n");
                        if (error.code != null) {
                           errorMessage.append("    Error code: ").append(error.code).append("\n");
                        }

                        if (error.position >= 0L) {
                           errorMessage.append("    Position: ").append(error.position).append("\n");
                        }

                        if (error.startPosition >= 0L) {
                           errorMessage.append("    Start position: ").append(error.startPosition).append("\n");
                        }

                        if (error.endPosition >= 0L) {
                           errorMessage.append("    End position: ").append(error.endPosition).append("\n");
                        }

                        if (error.isResolutionError) {
                           errorMessage.append("    Type: Resolution error\n");
                        }

                        if (error.isUnreachableError) {
                           errorMessage.append("    Type: Unreachable code error\n");
                        }

                        if (error.isNotAStatementError) {
                           errorMessage.append("    Type: Not a statement error\n");
                        }
                     }
                  }

                  if (!result.runtimeErrors.isEmpty()) {
                     errorMessage.append("\nRuntime errors:\n");

                     for(RuntimeError error : result.runtimeErrors) {
                        if (error.exceptionType != null) {
                           errorMessage.append("  - Exception type: ").append(error.exceptionType).append("\n");
                        }

                        if (error.message != null) {
                           errorMessage.append("    Message: ").append(error.message).append("\n");
                        }

                        if (error.stackTrace != null && !error.stackTrace.isBlank()) {
                           errorMessage.append("    Stack trace:\n").append(error.stackTrace).append("\n");
                        }
                     }
                  }

                  String error = errorMessage.toString();
                  this.log.logError(error);
                  throw new ToolException(error, (Throwable)null, ToolErrorType.RETRYABLE);
               }
            }
         }
      }

      this.cache.put(session.getSessionId(), session);
      return session;
   }

   private SessionSnapshot buildSessionSnapshot() {
      SessionSnapshot snapshot = new SessionSnapshot();
      addClassLoader(snapshot.classLoaders, Thread.currentThread().getContextClassLoader());
      addClassLoader(snapshot.classLoaders, JShellSessionManager.class.getClassLoader());

      for(IJShellBindingProvider provider : this.bindingProviders) {
         addClassLoader(snapshot.classLoaders, provider.getClass().getClassLoader());
         Map<String, JShellBindingDescription> bindings = provider.getBindings();
         if (bindings == null) {
            bindings = Map.of();
         }

         snapshot.bindingsByProvider.put(provider, bindings);

         for(JShellBindingDescription description : bindings.values()) {
            if (description != null) {
               addClassLoader(snapshot.classLoaders, description.getExplicitType());
               Object value = description.getValue();
               if (value != null) {
                  addClassLoader(snapshot.classLoaders, value.getClass().getClassLoader());
               }
            }
         }

         Collection<Class<?>> significantClasses = provider.getSignificantClasses();
         if (significantClasses == null) {
            significantClasses = List.of();
         }

         snapshot.significantClassesByProvider.put(provider, significantClasses);

         for(Class<?> clazz : significantClasses) {
            addClassLoader(snapshot.classLoaders, clazz);
         }

         Collection<String> imports = provider.getImports();
         snapshot.importsByProvider.put(provider, imports != null ? imports : List.of());
      }

      return snapshot;
   }

   private static void addClassLoader(Set<ClassLoader> classLoaders, Class<?> clazz) {
      if (clazz != null) {
         addClassLoader(classLoaders, clazz.getClassLoader());
      }
   }

   private static void addClassLoader(Set<ClassLoader> classLoaders, ClassLoader classLoader) {
      if (classLoader != null) {
         classLoaders.add(classLoader);
      }

   }

   public void invalidateSession(int sessionId) {
      this.cache.invalidate(sessionId);
   }

   public void invalidateAll() {
      this.cache.invalidateAll();
   }

   private static final class SessionSnapshot {
      private final Map<IJShellBindingProvider, Map<String, JShellBindingDescription>> bindingsByProvider = new LinkedHashMap();
      private final Map<IJShellBindingProvider, Collection<Class<?>>> significantClassesByProvider = new LinkedHashMap();
      private final Map<IJShellBindingProvider, Collection<String>> importsByProvider = new LinkedHashMap();
      private final Set<ClassLoader> classLoaders = new LinkedHashSet();
   }

   private static final class DelegatingClassLoader extends ClassLoader {
      private final List<ClassLoader> delegates = new ArrayList();

      private DelegatingClassLoader(ClassLoader parent, Set<ClassLoader> classLoaders) {
         super(parent);

         for(ClassLoader classLoader : classLoaders) {
            if (classLoader != null && classLoader != parent) {
               this.delegates.add(classLoader);
            }
         }

      }

      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
         synchronized(this.getClassLoadingLock(name)) {
            Class<?> alreadyLoaded = this.findLoadedClass(name);
            if (alreadyLoaded != null) {
               return alreadyLoaded;
            } else {
               Class var12;
               try {
                  var12 = super.loadClass(name, resolve);
               } catch (ClassNotFoundException e) {
                  for(ClassLoader delegate : this.delegates) {
                     try {
                        var12 = delegate.loadClass(name);
                     } catch (ClassNotFoundException var9) {
                        continue;
                     }

                     return var12;
                  }

                  throw e;
               }

               return var12;
            }
         }
      }
   }
}
