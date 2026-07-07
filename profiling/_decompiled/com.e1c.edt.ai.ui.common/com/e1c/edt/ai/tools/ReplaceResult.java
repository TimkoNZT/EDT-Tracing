package com.e1c.edt.ai.tools;

public class ReplaceResult {
   private final String updatedContent;
   private final int addedLines;
   private final int removedLines;
   private final boolean success;
   private final boolean multipleOccurrences;

   public ReplaceResult(String updatedContent, int addedLines, int removedLines, boolean success) {
      this(updatedContent, addedLines, removedLines, success, false);
   }

   public ReplaceResult(String updatedContent, int addedLines, int removedLines, boolean success, boolean multipleOccurrences) {
      this.updatedContent = updatedContent;
      this.addedLines = addedLines;
      this.removedLines = removedLines;
      this.success = success;
      this.multipleOccurrences = multipleOccurrences;
   }

   public String getUpdatedContent() {
      return this.updatedContent;
   }

   public int getAddedLines() {
      return this.addedLines;
   }

   public int getRemovedLines() {
      return this.removedLines;
   }

   public boolean isSuccess() {
      return this.success;
   }

   public boolean hasMultipleOccurrences() {
      return this.multipleOccurrences;
   }
}
