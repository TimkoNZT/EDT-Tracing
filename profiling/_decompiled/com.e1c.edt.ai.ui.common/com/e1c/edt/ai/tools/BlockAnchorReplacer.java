package com.e1c.edt.ai.tools;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class BlockAnchorReplacer implements IReplacementStrategy {
   private static final double SINGLE_CANDIDATE_SIMILARITY_THRESHOLD = (double)0.0F;
   private static final double MULTIPLE_CANDIDATES_SIMILARITY_THRESHOLD = 0.3;
   private final IReplacements replacements;

   @Inject
   public BlockAnchorReplacer(IReplacements replacements) {
      Preconditions.checkNotNull(replacements);
      this.replacements = replacements;
   }

   public Iterable<String> findCandidates(String content, String find) {
      List<String> matches = new ArrayList();
      String[] originalLines = this.replacements.splitLines(content);
      String[] searchLines = this.replacements.splitLines(find);
      if (searchLines.length < 3) {
         return matches;
      } else {
         searchLines = this.replacements.removeTrailingEmptyLine(searchLines);
         if (searchLines.length == 0) {
            return matches;
         } else {
            String firstLine = searchLines[0].trim();
            String lastLine = searchLines[searchLines.length - 1].trim();
            int searchBlockSize = searchLines.length;
            List<Candidate> candidates = new ArrayList();

            for(int i = 0; i < originalLines.length; ++i) {
               if (originalLines[i].trim().equals(firstLine)) {
                  for(int j = i + 2; j < originalLines.length; ++j) {
                     if (originalLines[j].trim().equals(lastLine)) {
                        candidates.add(new Candidate(i, j));
                        break;
                     }
                  }
               }
            }

            if (candidates.isEmpty()) {
               return matches;
            } else if (candidates.size() == 1) {
               Candidate candidate = (Candidate)candidates.get(0);
               if (this.isSingleCandidateAccepted(candidate, originalLines, searchLines, searchBlockSize)) {
                  matches.add(this.replacements.blockByLineRange(content, originalLines, candidate.startLine, candidate.endLine));
               }

               return matches;
            } else {
               Candidate bestMatch = null;
               double maxSimilarity = (double)-1.0F;

               for(Candidate candidate : candidates) {
                  double similarity = this.computeSimilarity(candidate, originalLines, searchLines, searchBlockSize);
                  if (similarity > maxSimilarity) {
                     maxSimilarity = similarity;
                     bestMatch = candidate;
                  }
               }

               if (bestMatch != null && maxSimilarity >= 0.3) {
                  matches.add(this.replacements.blockByLineRange(content, originalLines, bestMatch.startLine, bestMatch.endLine));
               }

               return matches;
            }
         }
      }
   }

   public int getOrdinal() {
      return 2;
   }

   private boolean isSingleCandidateAccepted(Candidate candidate, String[] originalLines, String[] searchLines, int searchBlockSize) {
      int actualBlockSize = candidate.endLine - candidate.startLine + 1;
      int linesToCheck = Math.min(searchBlockSize - 2, actualBlockSize - 2);
      double similarity;
      if (linesToCheck > 0) {
         similarity = (double)0.0F;

         for(int j = 1; j < searchBlockSize - 1 && j < actualBlockSize - 1; ++j) {
            String originalLine = originalLines[candidate.startLine + j].trim();
            String searchLine = searchLines[j].trim();
            int maxLen = Math.max(originalLine.length(), searchLine.length());
            if (maxLen != 0) {
               int distance = this.levenshtein(originalLine, searchLine);
               similarity += ((double)1.0F - (double)distance / (double)maxLen) / (double)linesToCheck;
               if (similarity >= (double)0.0F) {
                  break;
               }
            }
         }
      } else {
         similarity = (double)1.0F;
      }

      return similarity >= (double)0.0F;
   }

   private double computeSimilarity(Candidate candidate, String[] originalLines, String[] searchLines, int searchBlockSize) {
      int actualBlockSize = candidate.endLine - candidate.startLine + 1;
      int linesToCheck = Math.min(searchBlockSize - 2, actualBlockSize - 2);
      if (linesToCheck <= 0) {
         return (double)1.0F;
      } else {
         double similarity = (double)0.0F;

         for(int j = 1; j < searchBlockSize - 1 && j < actualBlockSize - 1; ++j) {
            String originalLine = originalLines[candidate.startLine + j].trim();
            String searchLine = searchLines[j].trim();
            int maxLen = Math.max(originalLine.length(), searchLine.length());
            if (maxLen != 0) {
               int distance = this.levenshtein(originalLine, searchLine);
               similarity += (double)1.0F - (double)distance / (double)maxLen;
            }
         }

         return similarity / (double)linesToCheck;
      }
   }

   private int levenshtein(String a, String b) {
      if (!a.isEmpty() && !b.isEmpty()) {
         int[][] matrix = new int[a.length() + 1][b.length() + 1];

         for(int i = 0; i <= a.length(); matrix[i][0] = i++) {
         }

         for(int j = 0; j <= b.length(); matrix[0][j] = j++) {
         }

         for(int i = 1; i <= a.length(); ++i) {
            for(int j = 1; j <= b.length(); ++j) {
               int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
               matrix[i][j] = Math.min(matrix[i - 1][j] + 1, Math.min(matrix[i][j - 1] + 1, matrix[i - 1][j - 1] + cost));
            }
         }

         return matrix[a.length()][b.length()];
      } else {
         return Math.max(a.length(), b.length());
      }
   }

   private static class Candidate {
      private final int startLine;
      private final int endLine;

      private Candidate(int startLine, int endLine) {
         this.startLine = startLine;
         this.endLine = endLine;
      }
   }
}
