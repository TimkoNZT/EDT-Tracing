package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.assistent.AIClientException;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.inject.Injector;
import java.util.Hashtable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

public abstract class BaseActivator extends AbstractUIPlugin implements ILog, IVersionProvider, DebugOptionsListener {
   private static final String TRACE_SOURCE_PREFIX = "com.e1c.edt.ai/";
   private static final String ICONS_PATH = "icons";
   private static BaseActivator plugin;
   private BundleContext bundleContext;
   private final AtomicReference<Injector> injectorRef = new AtomicReference();
   private volatile ISettings settings;
   private DebugTrace debugTrace;

   public static BaseActivator getDefault() {
      return plugin;
   }

   public static void injectMembers(Object instance) {
      getDefault().getInjector().injectMembers(instance);
   }

   public static Image getImage(String id) {
      return plugin.getImageRegistry().get(id);
   }

   public static ImageDescriptor getImageDescriptor(String id) {
      return plugin.getImageRegistry().getDescriptor(id);
   }

   public ImageDescriptor createImageDescriptorFromKey(String key) {
      String path = "icons" + key.substring(this.getPluginId().length());
      ImageDescriptor descriptor = (ImageDescriptor)ResourceLocator.imageDescriptorFromBundle(this.getPluginId(), path).get();
      return descriptor;
   }

   private static void log(IStatus status) {
      if (plugin != null) {
         org.eclipse.core.runtime.ILog logger = plugin.getLog();
         if (logger != null) {
            logger.log(status);
         }
      }
   }

   public void logError(Throwable throwable) {
      if (throwable != null) {
         if (throwable instanceof CompletionException) {
            CompletionException completionException = (CompletionException)throwable;
            throwable = completionException.getCause();
         }

         if (throwable instanceof ExecutionException) {
            ExecutionException completionException = (ExecutionException)throwable;
            throwable = completionException.getCause();
         }

         if (throwable instanceof AIClientException) {
            log(Status.info(throwable.getMessage()));
            return;
         }

         log(this.createErrorStatus(throwable.getMessage(), throwable));
      }

   }

   public void logError(String error) {
      if (error != null && !error.isBlank()) {
         log(this.createErrorStatus(error));
      }
   }

   public void warning(String topic, Supplier<String> details) {
      StringBuilder messsage = this.createMesssage(topic, details, Verbosity.WARNING);
      if (messsage != null && messsage.length() > 0) {
         log(Status.warning(messsage.toString()));
      }

   }

   public void optionsChanged(DebugOptions options) {
      this.debugTrace = options.newDebugTrace("com.e1c.edt.ai/");
   }

   public void trace(String tracingSource, String topic, Supplier<String> details) {
      if (this.isTracingEnabled(tracingSource)) {
         DebugTrace trace = this.debugTrace;
         if (trace != null) {
            StringBuilder messsage = this.createMesssage(topic, details, Verbosity.ERROR);
            if (messsage != null && messsage.length() > 0) {
               trace.trace(tracingSource, messsage.toString());
            }
         }

      }
   }

   public boolean isTracingEnabled(String tracingSource) {
      if (this.debugTrace == null) {
         return false;
      } else {
         return Platform.isRunning() && "true".equalsIgnoreCase(Platform.getDebugOption("com.e1c.edt.ai/" + tracingSource));
      }
   }

   private StringBuilder createMesssage(String topic, Supplier<String> details, Verbosity verbosity) {
      if (this.settings != null && this.settings.getVerbosity().getLevel() >= verbosity.getLevel()) {
         StringBuilder message = new StringBuilder();
         message.append(topic);
         message.append(System.lineSeparator());
         message.append((String)details.get());
         return message;
      } else {
         return null;
      }
   }

   private IStatus createErrorStatus(String message, Throwable throwable) {
      return new Status(4, "com.e1c.edt.ai/", 0, message, throwable);
   }

   private IStatus createErrorStatus(String message) {
      return new Status(4, "com.e1c.edt.ai/", 0, message, (Throwable)null);
   }

   public void start(BundleContext bundleContext) throws Exception {
      super.start(bundleContext);
      this.bundleContext = bundleContext;
      Hashtable<String, String> props = new Hashtable();
      props.put("listener.symbolic.name", "com.e1c.edt.ai/");
      bundleContext.registerService(DebugOptionsListener.class, this, props);
      plugin = this;
      javafx.application.Platform.setImplicitExit(false);
      this.settings = (ISettings)this.getInjector().getInstance(ISettings.class);
      Display.getDefault().disposeExec(() -> CancellationTokens.isStopped = true);
   }

   public void stop(BundleContext bundleContext) throws Exception {
      IGlobalContextTracker globalContextTracker = (IGlobalContextTracker)this.getInjector().getInstance(IGlobalContextTracker.class);
      if (globalContextTracker instanceof AutoCloseable) {
         ((AutoCloseable)globalContextTracker).close();
      }

      plugin = null;
      super.stop(bundleContext);
   }

   protected BundleContext getContext() {
      return this.bundleContext;
   }

   private Injector getInjector() {
      BaseActivator defaultActivator = getDefault();
      Injector existing = (Injector)defaultActivator.injectorRef.get();
      if (existing != null) {
         return existing;
      } else {
         Injector created;
         try {
            created = this.createInjector();
         } catch (Exception e) {
            log(this.createErrorStatus("Failed to create injector for " + this.getBundle().getSymbolicName(), e));
            throw new RuntimeException("Failed to create injector for " + this.getBundle().getSymbolicName(), e);
         }

         return defaultActivator.injectorRef.compareAndSet((Object)null, created) ? created : (Injector)defaultActivator.injectorRef.get();
      }
   }

   public ISettings trySettings() {
      return this.settings;
   }

   protected abstract Injector createInjector();

   public Version getPluginVersion() {
      Bundle bundle = getDefault().getBundle();
      return bundle.getVersion();
   }

   public String getPlatformVersion() {
      return System.getProperty("eclipse.buildId");
   }

   protected void initializeImageRegistry(ImageRegistry registry) {
      registry.put("AI", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/ai.png"));
      registry.put("INFO", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/info.png"));
      registry.put("WARNING", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/warning.png"));
      registry.put("ERROR", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/error.png"));
      registry.put("OFFLINE", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/status_offline.png"));
      registry.put("GIT_MESSAGE", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/gitmessage.png"));
      registry.put("GIT_REVIEW", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/gitreview.png"));
      registry.put("SUGGEST_YOUR_OPTION", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/suggest_your_option.png"));
      registry.put("CORRECT_ERRORS", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/correct_errors.png"));
      registry.put("IN_OTHER_WORDS", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/in_other_words.png"));
      registry.put("IMPROVE_STYLE", imageDescriptorFromPlugin(this.getPluginId(), "icons/obj16/improve_style.png"));
   }

   public abstract String getPluginId();
}
