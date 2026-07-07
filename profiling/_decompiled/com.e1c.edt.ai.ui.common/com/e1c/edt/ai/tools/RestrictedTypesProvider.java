package com.e1c.edt.ai.tools;

import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Singleton
public class RestrictedTypesProvider implements IRestrictedTypesProvider {
   private static final String RESTRICTED_TYPES_FILE = "restricted-types.properties";
   private final Set<String> restrictedTypes = this.loadRestrictedTypes();

   public Set<String> getRestrictedTypes() {
      return Collections.unmodifiableSet(this.restrictedTypes);
   }

   public boolean isRestricted(String typeName) {
      if (typeName == null) {
         return false;
      } else if (this.restrictedTypes.contains(typeName)) {
         return true;
      } else {
         for(String restricted : this.restrictedTypes) {
            if (restricted.endsWith(".*")) {
               String packagePrefix = restricted.substring(0, restricted.length() - 1);
               if (typeName.startsWith(packagePrefix)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private Set<String> loadRestrictedTypes() {
      HashSet<String> types = new HashSet();
      Properties properties = new Properties();

      try {
         Throwable var3 = null;
         Object var4 = null;

         try {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("restricted-types.properties");

            InputStream var10000;
            try {
               if (inputStream != null) {
                  properties.load(inputStream);

                  for(String key : properties.stringPropertyNames()) {
                     if (key != null && !key.trim().isEmpty() && !key.trim().startsWith("#")) {
                        types.add(key.trim());
                     }
                  }

                  return types;
               }
            } finally {
               var10000 = inputStream;
               if (inputStream != null) {
                  var10000 = inputStream;
                  inputStream.close();
               }

            }

            return var10000;
         } catch (Throwable var15) {
            if (var3 == null) {
               var3 = var15;
            } else if (var3 != var15) {
               var3.addSuppressed(var15);
            }

            throw var3;
         }
      } catch (IOException var16) {
         return types;
      }
   }
}
