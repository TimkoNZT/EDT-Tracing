package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import java.util.Optional;

public interface IEdtLinkHandler {
   String formatInsertCodePath(AIContext var1, String var2);

   String getFullPathForInsertCode(AIContext var1);

   String extractFilePath(String var1);

   boolean isRecognizedHref(String var1);

   Optional<CursorPositionInfo> extractCursorPosition(String var1);

   Optional<SelectionInfo> extractSelection(String var1);

   public static class CursorPositionInfo {
      private final int line;
      private final int column;

      public CursorPositionInfo(int line, int column) {
         this.line = line;
         this.column = column;
      }

      public int getLine() {
         return this.line;
      }

      public int getColumn() {
         return this.column;
      }
   }

   public static class SelectionInfo {
      private final int startLine;
      private final int startColumn;
      private final int endLine;
      private final int endColumn;

      public SelectionInfo(int startLine, int startColumn, int endLine, int endColumn) {
         this.startLine = startLine;
         this.startColumn = startColumn;
         this.endLine = endLine;
         this.endColumn = endColumn;
      }

      public int getStartLine() {
         return this.startLine;
      }

      public int getStartColumn() {
         return this.startColumn;
      }

      public int getEndLine() {
         return this.endLine;
      }

      public int getEndColumn() {
         return this.endColumn;
      }
   }
}
