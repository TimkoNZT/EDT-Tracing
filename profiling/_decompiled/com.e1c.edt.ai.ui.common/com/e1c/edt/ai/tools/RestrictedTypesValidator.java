package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ToolException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class RestrictedTypesValidator implements IRestrictedTypesValidator {
   private static final Pattern TYPE_IMPORT_PATTERN = Pattern.compile("\\bimport\\s+(?:static\\s+)?([\\w\\.\\*]+)\\s*;");
   private static final Pattern TYPE_REFERENCE_PATTERN = Pattern.compile("\\b([\\w.]*\\.[A-Z]\\w*)\\b");
   private static final Pattern NEW_PATTERN = Pattern.compile("\\bnew\\s+([\\w.]*\\.[A-Z]\\w*)\\b");
   private static final Pattern CAST_PATTERN = Pattern.compile("\\(([\\w.]*\\.[A-Z]\\w*)\\)");
   private static final Pattern SIMPLE_TYPE_PATTERN = Pattern.compile("(?<![\\w.])\\b([A-Z][\\w]*)\\b");
   private final Set<String> restrictedTypes;
   private final Map<String, String> simpleNameToFullName;

   @Inject
   public RestrictedTypesValidator(IRestrictedTypesProvider restrictedTypesProvider) {
      this.restrictedTypes = restrictedTypesProvider.getRestrictedTypes();
      this.simpleNameToFullName = buildSimpleNameMap(this.restrictedTypes);
   }

   private static Map<String, String> buildSimpleNameMap(Set<String> restrictedTypes) {
      HashMap<String, String> map = new HashMap();

      for(String type : restrictedTypes) {
         if (type != null && !type.endsWith(".*")) {
            int lastDot = type.lastIndexOf(46);
            if (lastDot > 0) {
               String simpleName = type.substring(lastDot + 1);
               map.put(simpleName, type);
            }
         }
      }

      return map;
   }

   public void validate(String code) throws ToolException {
      if (code != null && !code.isEmpty()) {
         String restrictedType = this.findRestrictedType(code);
         if (restrictedType != null) {
            throw new ToolException(String.format("Type '%s' is restricted and cannot be used. Please use alternative types that are allowed.", restrictedType));
         }
      }
   }

   private String findRestrictedType(String code) {
      Matcher importMatcher = TYPE_IMPORT_PATTERN.matcher(code);

      while(importMatcher.find()) {
         String importType = importMatcher.group(1).trim();
         if (this.isRestricted(importType)) {
            return importType;
         }
      }

      Matcher newMatcher = NEW_PATTERN.matcher(code);

      while(newMatcher.find()) {
         String typeName = newMatcher.group(1);
         if (this.isRestricted(typeName)) {
            return typeName;
         }
      }

      Matcher castMatcher = CAST_PATTERN.matcher(code);

      while(castMatcher.find()) {
         String typeName = castMatcher.group(1);
         if (this.isRestricted(typeName)) {
            return typeName;
         }
      }

      Matcher typeMatcher = TYPE_REFERENCE_PATTERN.matcher(code);

      while(typeMatcher.find()) {
         String typeName = typeMatcher.group(1);
         if (typeName.contains(".") && this.isRestricted(typeName)) {
            return typeName;
         }
      }

      Matcher simpleTypeMatcher = SIMPLE_TYPE_PATTERN.matcher(code);

      while(simpleTypeMatcher.find()) {
         String simpleName = simpleTypeMatcher.group(1);
         int start = simpleTypeMatcher.start();
         int end = simpleTypeMatcher.end();
         boolean isTypeReference = false;
         if (start > 0) {
            char prevChar = code.charAt(start - 1);
            if (prevChar == '<' || prevChar == ',') {
               isTypeReference = true;
            }
         }

         if (!isTypeReference && end < code.length()) {
            char nextChar = code.charAt(end);
            if (nextChar == '.') {
               if (end + 1 < code.length()) {
                  char afterDot = code.charAt(end + 1);
                  if (Character.isLowerCase(afterDot) || Character.isDigit(afterDot)) {
                     String fullName = (String)this.simpleNameToFullName.get(simpleName);
                     if (fullName != null) {
                        return fullName;
                     }
                  }
               }
            } else if (nextChar == ';' || Character.isWhitespace(nextChar) || nextChar == '>' || nextChar == '[' || nextChar == '(' || nextChar == '=') {
               isTypeReference = true;
            }
         }

         if (isTypeReference) {
            String fullName = (String)this.simpleNameToFullName.get(simpleName);
            if (fullName != null) {
               return fullName;
            }
         }
      }

      return null;
   }

   private boolean isRestricted(String typeName) {
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
}
