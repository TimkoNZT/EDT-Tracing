package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContentReplacer implements IContentReplacer {
   private static final String NORMALIZED_LINE_DELIMITER = "\n";
   private static final String BOM = "\ufeff";
   private final List<IReplacementStrategy> replacementStrategies;

   @Inject
   public ContentReplacer(Set<IReplacementStrategy> strategies) {
      this.replacementStrategies = (List)strategies.stream().sorted(Comparator.comparingInt(IReplacementStrategy::getOrdinal)).collect(Collectors.toList());
   }

   public ReplaceResult replace(String currentContent, String originContent, String newContent, String lineDelimiter, boolean replaceAll) {
      Preconditions.checkNotNull(currentContent);
      Preconditions.checkNotNull(originContent);
      Preconditions.checkNotNull(newContent);
      Preconditions.checkNotNull(lineDelimiter);
      String detectedLineDelimiter = this.detectLineDelimiter(currentContent);
      if (detectedLineDelimiter == null) {
         detectedLineDelimiter = lineDelimiter;
      }

      boolean currentHasBOM = currentContent.startsWith("\ufeff");
      String searchCurrentContent = this.stripBOM(currentContent);
      String searchOriginContent = this.stripBOM(originContent);
      String searchNewContent = this.stripBOM(newContent);
      String normalizedCurrentContent = this.normalizeLineDelimiters(searchCurrentContent);
      String normalizedOriginContent = this.normalizeLineDelimiters(searchOriginContent);
      String normalizedNewContent = this.normalizeLineDelimiters(searchNewContent);
      if (normalizedOriginContent.isEmpty()) {
         return this.replaceWithEmptyOrigin(normalizedCurrentContent, normalizedNewContent, detectedLineDelimiter, currentHasBOM, replaceAll);
      } else {
         ReplacementSearchResult searchResult = this.findReplacement(normalizedCurrentContent, normalizedOriginContent, replaceAll);
         if (searchResult.notFound) {
            return new ReplaceResult(currentContent, 0, 0, false);
         } else if (searchResult.multipleMatches) {
            return new ReplaceResult(currentContent, 0, 0, false, true);
         } else {
            int removedLines = this.countLinesIgnoringContext(searchResult.searchCandidate, normalizedNewContent, "\n", true);
            int addedLines = this.countLinesIgnoringContext(normalizedNewContent, searchResult.searchCandidate, "\n", false);
            if (replaceAll) {
               removedLines *= searchResult.occurrenceCount;
               addedLines *= searchResult.occurrenceCount;
            }

            String normalizedUpdatedContent;
            if (replaceAll) {
               normalizedUpdatedContent = normalizedCurrentContent.replace(searchResult.searchCandidate, normalizedNewContent);
            } else {
               normalizedUpdatedContent = normalizedCurrentContent.substring(0, searchResult.firstIndex) + normalizedNewContent + normalizedCurrentContent.substring(searchResult.firstIndex + searchResult.searchCandidate.length());
            }

            String updatedContent = this.denormalizeLineDelimiters(normalizedUpdatedContent, detectedLineDelimiter);
            updatedContent = this.restoreBOM(updatedContent, currentHasBOM);
            return new ReplaceResult(updatedContent, addedLines, removedLines, true, searchResult.occurrenceCount > 1);
         }
      }
   }

   private ReplacementSearchResult findReplacement(String content, String find, boolean replaceAll) {
      boolean foundAny = false;

      for(IReplacementStrategy strategy : this.replacementStrategies) {
         for(String candidate : strategy.findCandidates(content, find)) {
            int firstIndex = content.indexOf(candidate);
            if (firstIndex != -1) {
               foundAny = true;
               int occurrenceCount = this.countOccurrences(content, candidate);
               if (replaceAll) {
                  return ContentReplacer.ReplacementSearchResult.found(candidate, firstIndex, occurrenceCount);
               }

               int lastIndex = content.lastIndexOf(candidate);
               if (firstIndex == lastIndex) {
                  return ContentReplacer.ReplacementSearchResult.found(candidate, firstIndex, occurrenceCount);
               }
            }
         }
      }

      if (!foundAny) {
         return ContentReplacer.ReplacementSearchResult.notFound();
      } else {
         return ContentReplacer.ReplacementSearchResult.multipleMatches();
      }
   }

   private String stripBOM(String content) {
      if (content != null && !content.isEmpty()) {
         return content.startsWith("\ufeff") ? content.substring("\ufeff".length()) : content;
      } else {
         return content;
      }
   }

   private String restoreBOM(String content, boolean hadBOM) {
      return !hadBOM || content != null && content.startsWith("\ufeff") ? content : "\ufeff" + content;
   }

   private String detectLineDelimiter(String content) {
      if (content.isEmpty()) {
         return null;
      } else {
         int crCount = 0;
         int lfCount = 0;
         int crlfCount = 0;

         for(int i = 0; i < content.length(); ++i) {
            char c = content.charAt(i);
            if (c == '\r') {
               if (i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                  ++crlfCount;
                  ++i;
               } else {
                  ++crCount;
               }
            } else if (c == '\n') {
               ++lfCount;
            }
         }

         if (crlfCount > 0) {
            return "\r\n";
         } else if (crCount > 0) {
            return "\r";
         } else if (lfCount > 0) {
            return "\n";
         } else {
            return null;
         }
      }
   }

   private String normalizeLineDelimiters(String content) {
      return content.isEmpty() ? content : content.replace("\r\n", "\n").replace('\r', '\n');
   }

   private String denormalizeLineDelimiters(String content, String lineDelimiter) {
      return !content.isEmpty() && !lineDelimiter.equals("\n") ? content.replace("\n", lineDelimiter) : content;
   }

   private int countOccurrences(String str, String sub) {
      if (!str.isEmpty() && !sub.isEmpty()) {
         int count = 0;

         int idx;
         for(idx = 0; (idx = str.indexOf(sub, idx)) != -1; idx += sub.length()) {
            ++count;
         }

         return count;
      } else {
         return 0;
      }
   }

   private int countLinesIgnoringContext(String content, String otherContent, String lineDelimiter, boolean isRemoved) {
      if (content.isEmpty()) {
         return 0;
      } else {
         String[] contentLines = content.split(Pattern.quote(lineDelimiter), -1);
         String[] otherLines = otherContent.split(Pattern.quote(lineDelimiter), -1);
         int prefixLength = 0;

         for(int minPrefixLength = Math.min(contentLines.length, otherLines.length); prefixLength < minPrefixLength && contentLines[prefixLength].equals(otherLines[prefixLength]); ++prefixLength) {
         }

         int suffixLength = 0;

         for(int minSuffixLength = Math.min(contentLines.length - prefixLength, otherLines.length - prefixLength); suffixLength < minSuffixLength && contentLines[contentLines.length - 1 - suffixLength].equals(otherLines[otherLines.length - 1 - suffixLength]); ++suffixLength) {
         }

         int countedLines = contentLines.length - prefixLength - suffixLength;
         return isRemoved && countedLines == 0 && contentLines.length > 0 && otherLines.length > 0 && contentLines.length == otherLines.length && prefixLength + suffixLength == contentLines.length - 1 ? 1 : Math.max(0, countedLines);
      }
   }

   private ReplaceResult replaceWithEmptyOrigin(String normalizedCurrentContent, String normalizedNewContent, String detectedLineDelimiter, boolean currentHasBOM, boolean replaceAll) {
      int removedLines = this.countLinesIgnoringContext("", normalizedNewContent, "\n", true);
      int addedLines = this.countLinesIgnoringContext(normalizedNewContent, "", "\n", false);
      String normalizedUpdatedContent;
      if (replaceAll) {
         normalizedUpdatedContent = normalizedCurrentContent.replace("", normalizedNewContent);
         removedLines = 0;
         addedLines = 0;
      } else {
         normalizedUpdatedContent = normalizedCurrentContent.replaceFirst(Pattern.quote(""), Matcher.quoteReplacement(normalizedNewContent));
      }

      String updatedContent = this.denormalizeLineDelimiters(normalizedUpdatedContent, detectedLineDelimiter);
      updatedContent = this.restoreBOM(updatedContent, currentHasBOM);
      return new ReplaceResult(updatedContent, addedLines, removedLines, true, false);
   }

   private static class ReplacementSearchResult {
      private final boolean notFound;
      private final boolean multipleMatches;
      private final String searchCandidate;
      private final int firstIndex;
      private final int occurrenceCount;

      private ReplacementSearchResult(boolean notFound, boolean multipleMatches, String searchCandidate, int firstIndex, int occurrenceCount) {
         this.notFound = notFound;
         this.multipleMatches = multipleMatches;
         this.searchCandidate = searchCandidate;
         this.firstIndex = firstIndex;
         this.occurrenceCount = occurrenceCount;
      }

      private static ReplacementSearchResult found(String searchCandidate, int firstIndex, int occurrenceCount) {
         return new ReplacementSearchResult(false, false, searchCandidate, firstIndex, occurrenceCount);
      }

      private static ReplacementSearchResult notFound() {
         return new ReplacementSearchResult(true, false, (String)null, -1, 0);
      }

      private static ReplacementSearchResult multipleMatches() {
         return new ReplacementSearchResult(false, true, (String)null, -1, 0);
      }
   }
}
