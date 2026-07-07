package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ILinkProvider;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class EdtLinkHandler implements IEdtLinkHandler {
   private final IUI ui;
   private final ILog log;
   private final ILinkProvider linkProvider;

   @Inject
   public EdtLinkHandler(IUI ui, ILog log, ILinkProvider linkProvider) {
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(linkProvider);
      this.ui = ui;
      this.log = log;
      this.linkProvider = linkProvider;
   }

   public String formatInsertCodePath(AIContext ctx, String path) {
      if (ctx != null && path != null && !path.isBlank()) {
         String normalizedPath = path.replace('\\', '/');
         IDocument document = ctx.getDocument();
         if (document == null) {
            return this.linkProvider.file(normalizedPath);
         } else {
            boolean hasSelection = !ctx.getText().equals(ctx.getSource());

            try {
               if (hasSelection) {
                  int selectionStart = ctx.getSourceOffset() - ctx.getTextOffset();
                  int selectionFinish = selectionStart + ctx.getText().length();
                  Position startPosition = this.getPosition(document, selectionStart);
                  Position endPosition = this.getPosition(document, selectionFinish);
                  return this.linkProvider.file(normalizedPath, startPosition.line, startPosition.column, endPosition.line, endPosition.column);
               } else {
                  Position caretPosition = this.getPosition(document, ctx.getSourceOffset());
                  return this.linkProvider.file(normalizedPath, caretPosition.line, caretPosition.column);
               }
            } catch (BadLocationException error) {
               this.log.logError(error);
               return this.linkProvider.file(normalizedPath);
            }
         }
      } else {
         return path;
      }
   }

   public String getFullPathForInsertCode(AIContext ctx) {
      if (ctx == null) {
         return "";
      } else {
         String path = ctx.getPath();
         if (path != null && !path.isBlank()) {
            return !path.matches("^[A-Za-z]:[/\\\\].*") && !path.startsWith("/") && !path.startsWith("\\\\") ? (String)this.ui.getLastSourceViewer().flatMap((sourceViewer) -> this.ui.getFile(sourceViewer)).map((file) -> file.getLocation().toPortableString()).orElse("") : path;
         } else {
            return "";
         }
      }
   }

   public String extractFilePath(String href) {
      if (!this.isRecognizedHref(href)) {
         return "";
      } else {
         String filePath = href.substring(this.linkProvider.getFileProtocol().length());
         int colonIndex = filePath.indexOf(":");
         if (colonIndex > 0) {
            filePath = filePath.substring(0, colonIndex);
         }

         filePath = filePath.replace("%3A", ":");
         return filePath;
      }
   }

   public boolean isRecognizedHref(String href) {
      return href != null && !href.isBlank() ? href.startsWith(this.linkProvider.getFileProtocol()) : false;
   }

   public Optional<IEdtLinkHandler.CursorPositionInfo> extractCursorPosition(String href) {
      if (!this.isRecognizedHref(href)) {
         return Optional.empty();
      } else {
         String filePath = href.substring(this.linkProvider.getFileProtocol().length());
         int colonIndex = filePath.indexOf(":");
         if (colonIndex < 0) {
            return Optional.empty();
         } else {
            String positionPart = filePath.substring(colonIndex + 1);
            String[] parts = positionPart.split("\\:");
            if (parts.length < 2) {
               return Optional.empty();
            } else {
               try {
                  int line = Integer.parseInt(parts[0]);
                  int column = Integer.parseInt(parts[1]);
                  return Optional.of(new IEdtLinkHandler.CursorPositionInfo(line, column));
               } catch (NumberFormatException var8) {
                  return Optional.empty();
               }
            }
         }
      }
   }

   public Optional<IEdtLinkHandler.SelectionInfo> extractSelection(String href) {
      if (!this.isRecognizedHref(href)) {
         return Optional.empty();
      } else {
         String filePath = href.substring(this.linkProvider.getFileProtocol().length());
         int colonIndex = filePath.indexOf(":");
         if (colonIndex < 0) {
            return Optional.empty();
         } else {
            String positionPart = filePath.substring(colonIndex + 1);
            String[] parts = positionPart.split("\\:");
            if (parts.length < 4) {
               return Optional.empty();
            } else {
               try {
                  int startLine = Integer.parseInt(parts[0]);
                  int startColumn = Integer.parseInt(parts[1]);
                  int endLine = Integer.parseInt(parts[2]);
                  int endColumn = Integer.parseInt(parts[3]);
                  if (startLine > endLine) {
                     return Optional.empty();
                  } else {
                     return startLine == endLine && startColumn > endColumn ? Optional.empty() : Optional.of(new IEdtLinkHandler.SelectionInfo(startLine, startColumn, endLine, endColumn));
                  }
               } catch (NumberFormatException var10) {
                  return Optional.empty();
               }
            }
         }
      }
   }

   private Position getPosition(IDocument document, int offset) throws BadLocationException {
      int safeOffset = Math.max(0, Math.min(offset, document.getLength()));
      int line = document.getLineOfOffset(safeOffset);
      int lineOffset = document.getLineOffset(line);
      return new Position(line + 1, safeOffset - lineOffset + 1);
   }

   private static class Position {
      private final int line;
      private final int column;

      private Position(int line, int column) {
         this.line = line;
         this.column = column;
      }
   }
}
