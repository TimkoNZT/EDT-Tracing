package com.e1c.edt.ai.tools;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class PatternMatcher implements IPatternMatcher {
   public boolean matches(String path, String pattern) {
      String normalizedPath = path.replace("\\", "/");
      String normalizedPattern = pattern.replace("\\", "/");
      String[] pathParts = normalizedPath.split("/");
      String[] patternParts = normalizedPattern.split("/");
      return this.matchGlob(pathParts, 0, patternParts, 0);
   }

   private boolean matchGlob(String[] pathParts, int pathIndex, String[] patternParts, int patternIndex) {
      if (patternIndex == patternParts.length) {
         return pathIndex == pathParts.length;
      } else if (pathIndex == pathParts.length) {
         for(int i = patternIndex; i < patternParts.length; ++i) {
            if (!patternParts[i].equals("**")) {
               return false;
            }
         }

         return true;
      } else {
         String currentPattern = patternParts[patternIndex];
         if (!currentPattern.equals("**")) {
            return this.matchSegment(pathParts[pathIndex], currentPattern) ? this.matchGlob(pathParts, pathIndex + 1, patternParts, patternIndex + 1) : false;
         } else if (patternIndex + 1 == patternParts.length) {
            return true;
         } else if (this.matchGlob(pathParts, pathIndex, patternParts, patternIndex + 1)) {
            return true;
         } else {
            for(int i = pathIndex; i < pathParts.length; ++i) {
               if (this.matchGlob(pathParts, i + 1, patternParts, patternIndex + 1)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private boolean matchSegment(String segment, String pattern) {
      return segment.matches(this.translateSegmentToRegex(pattern));
   }

   private String translateSegmentToRegex(String pattern) {
      StringBuilder regex = new StringBuilder();
      int i = 0;

      while(i < pattern.length()) {
         char c = pattern.charAt(i);
         switch (c) {
            case '*':
               regex.append(".*");
               ++i;
               break;
            case '.':
               regex.append("\\.");
               ++i;
               break;
            case '?':
               regex.append(".");
               ++i;
               break;
            case '[':
               int end = pattern.indexOf(93, i);
               if (end == -1) {
                  regex.append(Pattern.quote("["));
                  ++i;
               } else {
                  regex.append(this.processCharacterClass(pattern.substring(i + 1, end)));
                  i = end + 1;
               }
               break;
            case '{':
               int end = this.findMatchingBrace(pattern, i);
               if (end == -1) {
                  regex.append(Pattern.quote("{"));
                  ++i;
                  break;
               }

               String content = pattern.substring(i + 1, end);
               List<String> alternatives = this.splitTopLevelCommas(content);
               if (alternatives.size() < 2) {
                  regex.append(Pattern.quote(pattern.substring(i, end + 1)));
               } else {
                  regex.append("(");

                  for(int k = 0; k < alternatives.size(); ++k) {
                     if (k > 0) {
                        regex.append("|");
                     }

                     regex.append(this.translateSegmentToRegex((String)alternatives.get(k)));
                  }

                  regex.append(")");
               }

               i = end + 1;
               break;
            default:
               regex.append(Pattern.quote(String.valueOf(c)));
               ++i;
         }
      }

      return regex.toString();
   }

   private int findMatchingBrace(String s, int start) {
      int depth = 0;

      for(int i = start; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (c == '{') {
            ++depth;
         } else if (c == '}') {
            --depth;
            if (depth == 0) {
               return i;
            }
         }
      }

      return -1;
   }

   private List<String> splitTopLevelCommas(String s) {
      List<String> parts = new ArrayList();
      int depth = 0;
      int start = 0;

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (c != '{' && c != '[') {
            if (c != '}' && c != ']') {
               if (c == ',' && depth == 0) {
                  parts.add(s.substring(start, i));
                  start = i + 1;
               }
            } else {
               --depth;
            }
         } else {
            ++depth;
         }
      }

      parts.add(s.substring(start));
      return parts;
   }

   private String processCharacterClass(String content) {
      if (content.isEmpty()) {
         return Pattern.quote("[]");
      } else {
         boolean negate = content.charAt(0) == '!' || content.charAt(0) == '^';
         int start = negate ? 1 : 0;
         StringBuilder regex = new StringBuilder();
         regex.append(negate ? "[^" : "[");
         int i = start;

         while(i < content.length()) {
            char c = content.charAt(i);
            if (c == '-' && i > start && i + 1 < content.length()) {
               char prev = content.charAt(i - 1);
               char next = content.charAt(i + 1);
               if (prev < next) {
                  regex.append("-").append(this.escapeForCharClass(next));
                  i += 2;
               } else {
                  regex.append("\\-");
                  ++i;
               }
            } else {
               regex.append(this.escapeForCharClass(c));
               ++i;
            }
         }

         regex.append("]");
         return regex.toString();
      }
   }

   private String escapeForCharClass(char c) {
      switch (c) {
         case '-':
         case '[':
         case '\\':
         case ']':
         case '^':
            return "\\" + c;
         default:
            return String.valueOf(c);
      }
   }
}
