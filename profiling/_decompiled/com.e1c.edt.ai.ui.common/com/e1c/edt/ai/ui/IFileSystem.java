package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IFileDocument;
import java.io.IOException;
import java.io.Reader;

public interface IFileSystem {
   Iterable<String> getLines(IFileDocument var1, int var2, int var3);

   Iterable<String> getLines(Reader var1);

   boolean isPrintable(String var1, double var2);

   boolean fileExists(String var1) throws IOException;

   boolean isFileEmpty(String var1) throws IOException;

   byte[] readAllBytes(String var1) throws IOException;

   void writeAllBytes(String var1, byte[] var2) throws IOException;

   void deleteFile(String var1) throws IOException;
}
