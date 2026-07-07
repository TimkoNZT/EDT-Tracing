package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ResourceProvider implements IResourceProvider {
   private final ILog log;
   private final ISettings settings;

   @Inject
   public ResourceProvider(ILog log, ISettings settings) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settings);
      this.log = log;
      this.settings = settings;
   }

   public Optional<String> getTextResource(String filePath) {
      Optional<String> optionalResources = this.settings.getResources();
      if (optionalResources.isPresent()) {
         Path path = Paths.get((String)optionalResources.get(), filePath);
         if (Files.exists(path, new LinkOption[0])) {
            try {
               return Optional.ofNullable(Files.readString(path, StandardCharsets.UTF_8));
            } catch (IOException error) {
               this.log.logError(error);
            }
         }
      }

      Bundle bundle = FrameworkUtil.getBundle(this.getClass());
      URL url = bundle.getEntry(filePath);
      if (url == null) {
         this.log.logError("Resource not found: " + filePath);
         return Optional.empty();
      } else {
         try {
            Throwable var5 = null;
            Object var6 = null;

            try {
               InputStream inputStream = url.openStream();

               Optional var10000;
               try {
                  BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                  try {
                     StringBuilder content = new StringBuilder();
                     char[] buffer = new char[8192];

                     int charsRead;
                     while((charsRead = reader.read(buffer)) != -1) {
                        content.append(buffer, 0, charsRead);
                     }

                     var10000 = Optional.of(content.toString());
                  } finally {
                     if (reader != null) {
                        reader.close();
                     }

                  }
               } catch (Throwable var28) {
                  if (var5 == null) {
                     var5 = var28;
                  } else if (var5 != var28) {
                     var5.addSuppressed(var28);
                  }

                  if (inputStream != null) {
                     inputStream.close();
                  }

                  throw var5;
               }

               if (inputStream != null) {
                  inputStream.close();
               }

               return var10000;
            } catch (Throwable var29) {
               if (var5 == null) {
                  var5 = var29;
               } else if (var5 != var29) {
                  var5.addSuppressed(var29);
               }

               throw var5;
            }
         } catch (IOException error) {
            this.log.logError(error);
            return Optional.empty();
         }
      }
   }
}
