package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

public final class JShellSharedExecutionControlProvider implements ExecutionControlProvider {
   private final ClassLoader parent;
   private ByteArrayOutputStream outBuffer;
   private ByteArrayOutputStream errBuffer;

   public JShellSharedExecutionControlProvider(ClassLoader parent) {
      this.parent = parent;
      this.outBuffer = null;
      this.errBuffer = null;
   }

   public void setOutputBuffers(ByteArrayOutputStream outBuffer, ByteArrayOutputStream errBuffer) {
      this.outBuffer = outBuffer;
      this.errBuffer = errBuffer;
   }

   public String name() {
      return "local-shared";
   }

   public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) {
      return new CapturingExecutionControl(new SharedLoaderDelegate(this.parent), this.outBuffer, this.errBuffer, this.parent);
   }

   private static final class SharedLoaderDelegate implements LoaderDelegate {
      private final SharedClassLoader loader;
      private final Map<String, Class<?>> klasses = new ConcurrentHashMap();

      private SharedLoaderDelegate(ClassLoader parent) {
         this.loader = new SharedClassLoader(parent);
      }

      public void load(ExecutionControl.ClassBytecodes[] cbcs) throws ExecutionControl.ClassInstallException, ExecutionControl.NotImplementedException, ExecutionControl.EngineTerminationException {
         boolean[] loaded = new boolean[cbcs.length];

         try {
            for(ExecutionControl.ClassBytecodes cbc : cbcs) {
               this.loader.declare(cbc.name(), cbc.bytecodes());
            }

            for(int i = 0; i < cbcs.length; ++i) {
               ExecutionControl.ClassBytecodes cbc = cbcs[i];
               Class<?> klass = this.loader.loadClass(cbc.name());
               this.klasses.put(cbc.name(), klass);
               loaded[i] = true;
               klass.getDeclaredMethods();
            }

         } catch (Throwable ex) {
            throw new ExecutionControl.ClassInstallException("load: " + ex.getMessage(), loaded);
         }
      }

      public void classesRedefined(ExecutionControl.ClassBytecodes[] cbcs) {
         for(ExecutionControl.ClassBytecodes cbc : cbcs) {
            this.loader.declare(cbc.name(), cbc.bytecodes());
         }

      }

      public void addToClasspath(String path) throws ExecutionControl.EngineTerminationException, ExecutionControl.InternalException {
         try {
            String[] var5;
            for(String entry : var5 = path.split(File.pathSeparator)) {
               this.loader.addURL((new File(entry)).toURI().toURL());
            }

         } catch (Exception ex) {
            throw new ExecutionControl.InternalException(ex.toString());
         }
      }

      public Class<?> findClass(String name) throws ClassNotFoundException {
         Class<?> klass = (Class)this.klasses.get(name);
         if (klass == null) {
            throw new ClassNotFoundException(name + " not found");
         } else {
            return klass;
         }
      }
   }

   private static final class SharedClassLoader extends URLClassLoader {
      private final Map<String, byte[]> classBytes = new ConcurrentHashMap();
      private final ClassLoader osgiClassLoader;

      private SharedClassLoader(ClassLoader parent) {
         super(new URL[0], parent);
         this.osgiClassLoader = parent;
      }

      private void declare(String name, byte[] bytes) {
         this.classBytes.put(name, bytes);
      }

      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
         synchronized(this.getClassLoadingLock(name)) {
            Class<?> loadedClass = this.findLoadedClass(name);
            if (loadedClass != null) {
               return loadedClass;
            } else {
               byte[] bytes = (byte[])this.classBytes.get(name);
               if (bytes != null) {
                  loadedClass = this.defineClass(name, bytes, 0, bytes.length);
                  if (resolve) {
                     this.resolveClass(loadedClass);
                  }

                  return loadedClass;
               } else {
                  try {
                     loadedClass = this.osgiClassLoader.loadClass(name);
                     if (loadedClass != null) {
                        if (resolve) {
                           this.resolveClass(loadedClass);
                        }

                        Class var10000 = loadedClass;
                        return var10000;
                     }
                  } catch (ClassNotFoundException var7) {
                  }

                  return super.loadClass(name, resolve);
               }
            }
         }
      }

      protected Class<?> findClass(String name) throws ClassNotFoundException {
         byte[] bytes = (byte[])this.classBytes.get(name);
         return bytes != null ? this.defineClass(name, bytes, 0, bytes.length) : super.findClass(name);
      }

      public void addURL(URL url) {
         super.addURL(url);
      }
   }

   private static final class CapturingExecutionControl implements ExecutionControl {
      private final LoaderDelegate loaderDelegate;
      private final ByteArrayOutputStream outBuffer;
      private final ByteArrayOutputStream errBuffer;
      private final ClassLoader contextClassLoader;

      CapturingExecutionControl(LoaderDelegate loaderDelegate, ByteArrayOutputStream outBuffer, ByteArrayOutputStream errBuffer, ClassLoader contextClassLoader) {
         Preconditions.checkNotNull(loaderDelegate);
         Preconditions.checkNotNull(outBuffer);
         Preconditions.checkNotNull(errBuffer);
         this.loaderDelegate = loaderDelegate;
         this.outBuffer = outBuffer;
         this.errBuffer = errBuffer;
         this.contextClassLoader = contextClassLoader;
      }

      public void close() {
      }

      public String invoke(String className, String methodName) throws ExecutionControl.RunException, ExecutionControl.EngineTerminationException, ExecutionControl.InternalException {
         PrintStream originalOut = System.out;
         PrintStream originalErr = System.err;
         Thread currentThread = Thread.currentThread();
         ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();

         String var11;
         try {
            if (this.contextClassLoader != null) {
               currentThread.setContextClassLoader(this.contextClassLoader);
            }

            System.setOut(new PrintStream(this.outBuffer, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(this.errBuffer, true, StandardCharsets.UTF_8));
            Class<?> klass = this.loaderDelegate.findClass(className);
            Method method = klass.getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object result = method.invoke((Object)null);
            var11 = this.formatResultValue(result);
         } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new ExecutionControl.InternalException(((ReflectiveOperationException)e).toString());
         } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
               throw (RuntimeException)cause;
            }

            throw new RuntimeException(cause);
         } finally {
            currentThread.setContextClassLoader(originalContextClassLoader);
            System.setOut(originalOut);
            System.setErr(originalErr);
         }

         return var11;
      }

      public Object extensionCommand(String command, Object arg) throws ExecutionControl.RunException, ExecutionControl.EngineTerminationException, ExecutionControl.InternalException {
         throw new ExecutionControl.NotImplementedException("extensionCommand");
      }

      public void load(ExecutionControl.ClassBytecodes[] cbcs) throws ExecutionControl.ClassInstallException, ExecutionControl.NotImplementedException, ExecutionControl.EngineTerminationException {
         PrintStream originalOut = System.out;
         PrintStream originalErr = System.err;
         Thread currentThread = Thread.currentThread();
         ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();

         try {
            if (this.contextClassLoader != null) {
               currentThread.setContextClassLoader(this.contextClassLoader);
            }

            System.setOut(new PrintStream(this.outBuffer, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(this.errBuffer, true, StandardCharsets.UTF_8));
            this.loaderDelegate.load(cbcs);
         } finally {
            currentThread.setContextClassLoader(originalContextClassLoader);
            System.setOut(originalOut);
            System.setErr(originalErr);
         }

      }

      public void redefine(ExecutionControl.ClassBytecodes[] cbcs) throws ExecutionControl.ClassInstallException, ExecutionControl.NotImplementedException, ExecutionControl.EngineTerminationException {
         this.loaderDelegate.classesRedefined(cbcs);
      }

      public void addToClasspath(String path) throws ExecutionControl.EngineTerminationException, ExecutionControl.InternalException {
         this.loaderDelegate.addToClasspath(path);
      }

      public void stop() throws ExecutionControl.EngineTerminationException, ExecutionControl.InternalException {
         throw new ExecutionControl.NotImplementedException("stop");
      }

      public String varValue(String className, String varName) throws ExecutionControl.EngineTerminationException, ExecutionControl.InternalException, ExecutionControl.RunException {
         throw new ExecutionControl.NotImplementedException("varValue");
      }

      private String formatResultValue(Object result) {
         if (result == null) {
            return "null";
         } else {
            Class<?> resultClass = result.getClass();
            return !(result instanceof String) && !(result instanceof Number) && !(result instanceof Boolean) && !(result instanceof Character) && !resultClass.isEnum() ? resultClass.getName() + "@" + Integer.toHexString(System.identityHashCode(result)) : result.toString();
         }
      }
   }
}
