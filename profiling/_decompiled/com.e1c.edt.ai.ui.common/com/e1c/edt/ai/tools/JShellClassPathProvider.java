package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jdk.jshell.JShell;
import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

@Singleton
class JShellClassPathProvider implements IJShellClassPathProvider {
   private final ILog log;

   @Inject
   public JShellClassPathProvider(ILog log) {
      Preconditions.checkNotNull(log);
      this.log = log;
   }

   public void addClassPathFor(JShell shell, Class<?> clazz) {
      try {
         ProtectionDomain protectionDomain = clazz.getProtectionDomain();
         if (protectionDomain == null) {
            this.addBundleClassPathFor(shell, clazz);
            return;
         }

         CodeSource codeSource = protectionDomain.getCodeSource();
         if (codeSource == null || codeSource.getLocation() == null) {
            this.addBundleClassPathFor(shell, clazz);
            return;
         }

         URI location = codeSource.getLocation().toURI();
         Path path = Paths.get(location);
         shell.addToClasspath(path.toString());
         this.addBinIfPresent(shell, path);
      } catch (Exception e) {
         this.log.logError("Failed to add classpath for " + clazz.getName() + ": " + e.getMessage());
      }

   }

   public void addAllBundleClassPaths(JShell shell) {
      try {
         BundleContext context = FrameworkUtil.getBundle(JShellClassPathProvider.class).getBundleContext();
         if (context != null) {
            Bundle[] bundles = context.getBundles();
            Set<String> addedBundles = new HashSet();

            for(Bundle bundle : bundles) {
               this.addBundleClassPathWithDependencies(shell, bundle, addedBundles);
            }
         }
      } catch (Exception e) {
         this.log.logError("Failed to add all bundle classpaths: " + e.getMessage());
      }

   }

   private void addBundleClassPathFor(JShell shell, Class<?> clazz) {
      try {
         Bundle bundle = FrameworkUtil.getBundle(clazz);
         if (bundle == null) {
            return;
         }

         File bundleFile = FileLocator.getBundleFile(bundle);
         if (bundleFile == null) {
            return;
         }

         shell.addToClasspath(bundleFile.getAbsolutePath());
         this.addBinIfPresent(shell, bundleFile.toPath());
      } catch (Exception e) {
         this.log.logError("Failed to add OSGi classpath for " + clazz.getName() + ": " + e.getMessage());
      }

   }

   private void addBundleClassPathWithDependencies(JShell shell, Bundle bundle, Set<String> addedBundles) {
      if (bundle != null && !addedBundles.contains(bundle.getSymbolicName())) {
         try {
            if (bundle.getState() == 32 || bundle.getState() == 4) {
               File bundleFile = FileLocator.getBundleFile(bundle);
               if (bundleFile != null) {
                  shell.addToClasspath(bundleFile.getAbsolutePath());
                  this.addBinIfPresent(shell, bundleFile.toPath());
                  addedBundles.add(bundle.getSymbolicName());
                  this.addBundleWiringDependencies(shell, bundle, addedBundles);
               }
            }
         } catch (Exception e) {
            this.log.logError("Failed to add bundle classpath for " + bundle.getSymbolicName() + ": " + e.getMessage());
         }

      }
   }

   private void addBundleWiringDependencies(JShell shell, Bundle bundle, Set<String> addedBundles) {
      try {
         BundleWiring bundleWiring = (BundleWiring)bundle.adapt(BundleWiring.class);
         if (bundleWiring == null) {
            return;
         }

         List<BundleWire> requiredWires = bundleWiring.getRequiredWires("osgi.wiring.bundle");
         if (requiredWires != null) {
            for(BundleWire wire : requiredWires) {
               BundleWiring providerWiring = wire.getProviderWiring();
               if (providerWiring != null) {
                  Bundle providerBundle = providerWiring.getBundle();
                  if (providerBundle != null) {
                     this.addBundleClassPathWithDependencies(shell, providerBundle, addedBundles);
                  }
               }
            }
         }
      } catch (Exception e) {
         this.log.logError("Failed to add wiring dependencies for " + bundle.getSymbolicName() + ": " + e.getMessage());
      }

   }

   private void addBinIfPresent(JShell shell, Path root) {
      try {
         if (root == null) {
            return;
         }

         if (Files.isRegularFile(root, new LinkOption[0])) {
            return;
         }

         if (root.endsWith("bin") || root.endsWith("target\\classes") || root.endsWith("target/classes")) {
            return;
         }

         Path candidate = root.resolve("bin");
         if (Files.isDirectory(candidate, new LinkOption[0])) {
            shell.addToClasspath(candidate.toString());
         }

         candidate = root.resolve("target").resolve("classes");
         if (Files.isDirectory(candidate, new LinkOption[0])) {
            shell.addToClasspath(candidate.toString());
         }
      } catch (Exception e) {
         this.log.logError("Failed to add bin classpath: " + e.getMessage());
      }

   }
}
