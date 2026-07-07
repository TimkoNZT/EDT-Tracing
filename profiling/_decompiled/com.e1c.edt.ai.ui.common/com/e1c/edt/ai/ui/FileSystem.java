package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IFileDocument;
import java.io.IOException;
import java.io.Reader;
import java.lang.Character.UnicodeBlock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class FileSystem implements IFileSystem {
   public Iterable<String> getLines(IFileDocument fileDocument, int firstLineNumber, int linesNumber) {
      return () -> new LineIterator(fileDocument, firstLineNumber, linesNumber);
   }

   public boolean isPrintable(String text, double threshold) {
      if (text != null && !text.isBlank()) {
         int printable = 0;

         for(int i = 0; i < text.length(); ++i) {
            if (isPrintable(text.charAt(i))) {
               ++printable;
            }
         }

         if ((double)100.0F * (double)printable / (double)text.length() >= threshold) {
            return true;
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   private static boolean isPrintable(char c) {
      Character.UnicodeBlock block = UnicodeBlock.of(c);
      return !Character.isISOControl(c) && block != null && block != UnicodeBlock.SPECIALS;
   }

   public boolean fileExists(String filePath) throws IOException {
      return filePath != null && !filePath.isBlank() ? Files.exists(Paths.get(filePath), new LinkOption[0]) : false;
   }

   public boolean isFileEmpty(String filePath) throws IOException {
      if (filePath != null && !filePath.isBlank()) {
         Path path = Paths.get(filePath);
         if (!Files.exists(path, new LinkOption[0])) {
            return false;
         } else {
            return Files.size(path) == 0L;
         }
      } else {
         return false;
      }
   }

   public byte[] readAllBytes(String filePath) throws IOException {
      if (filePath != null && !filePath.isBlank()) {
         return Files.readAllBytes(Paths.get(filePath));
      } else {
         throw new IOException("File path cannot be null or empty");
      }
   }

   public void writeAllBytes(String filePath, byte[] data) throws IOException {
      if (filePath != null && !filePath.isBlank()) {
         Path path = Paths.get(filePath);
         Path parent = path.getParent();
         if (parent != null && !Files.exists(parent, new LinkOption[0])) {
            Files.createDirectories(parent);
         }

         Files.write(path, data, new OpenOption[0]);
      } else {
         throw new IOException("File path cannot be null or empty");
      }
   }

   public void deleteFile(String filePath) throws IOException {
      if (filePath != null && !filePath.isBlank()) {
         Files.deleteIfExists(Paths.get(filePath));
      } else {
         throw new IOException("File path cannot be null or empty");
      }
   }

   public Iterable<String> getLines(Reader reader) {
      return () -> new BufferedReaderLineIterator(reader);
   }

   private static class LineIterator implements Iterator<String> {
      private final IFileDocument fileDocument;
      private final int endLine;
      private int currentLine;

      public LineIterator(IFileDocument fileDocument, int firstLineNumber, int linesNumber) {
         this.fileDocument = fileDocument;
         IDocument doc = fileDocument.getDocument();
         int totalLines = doc.getNumberOfLines();
         this.currentLine = firstLineNumber;
         this.endLine = Math.min(firstLineNumber + linesNumber, totalLines);
      }

      public boolean hasNext() {
         return this.currentLine < this.endLine;
      }

      public String next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         } else {
            try {
               IDocument doc = this.fileDocument.getDocument();
               String line = doc.get(doc.getLineOffset(this.currentLine), doc.getLineLength(this.currentLine));
               ++this.currentLine;
               return line;
            } catch (BadLocationException e) {
               throw new RuntimeException("Error reading line: " + this.currentLine, e);
            }
         }
      }
   }

   private static class BufferedReaderLineIterator implements Iterator<String> {
      private final Reader reader;
      private String nextLine = null;
      private boolean bomSkipped = false;
      private boolean finished = false;

      public BufferedReaderLineIterator(Reader reader) {
         this.reader = reader;
      }

      public boolean hasNext() {
         if (this.finished) {
            return false;
         } else {
            this.nextLine = this.readNextLine();
            return this.nextLine != null;
         }
      }

      public String next() {
         return this.nextLine;
      }

      private String readNextLine() {
         StringBuilder currentLine = new StringBuilder();

         try {
            int ch;
            while((ch = this.reader.read()) != -1) {
               if (ch == 13) {
                  this.reader.mark(1);
                  int nextCh = this.reader.read();
                  if (nextCh == 10) {
                     String lineContent = currentLine.toString();
                     if (!this.bomSkipped) {
                        lineContent = removeBOM(lineContent);
                        this.bomSkipped = true;
                     }

                     currentLine.setLength(0);
                     return lineContent + "\r\n";
                  }

                  if (nextCh != -1) {
                     this.reader.reset();
                  }

                  String lineContent = currentLine.toString();
                  if (!this.bomSkipped) {
                     lineContent = removeBOM(lineContent);
                     this.bomSkipped = true;
                  }

                  currentLine.setLength(0);
                  return lineContent + "\r";
               }

               if (ch == 10) {
                  String lineContent = currentLine.toString();
                  if (!this.bomSkipped) {
                     lineContent = removeBOM(lineContent);
                     this.bomSkipped = true;
                  }

                  currentLine.setLength(0);
                  return lineContent + "\n";
               }

               currentLine.append((char)ch);
            }

            if (currentLine.length() > 0) {
               String lineContent = currentLine.toString();
               if (!this.bomSkipped) {
                  lineContent = removeBOM(lineContent);
                  this.bomSkipped = true;
               }

               this.finished = true;
               return lineContent;
            } else {
               this.finished = true;
               return null;
            }
         } catch (IOException e) {
            throw new RuntimeException("Error reading line", e);
         }
      }

      private static String removeBOM(String content) {
         if (content != null && !content.isEmpty()) {
            return content.startsWith("\ufeff") ? content.substring(1) : content;
         } else {
            return content;
         }
      }
   }
}
