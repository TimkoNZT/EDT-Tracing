package com.e1c.edt.ai.tools;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

class LocalHistoryUtils implements ILocalHistoryUtils {
   public List<LocalHistoryEntry> getLocalHistory(IFile file, int maxEntries) throws Exception {
      ArrayList<LocalHistoryEntry> entries = new ArrayList();
      String absolutePath = file.getLocation().toFile().getAbsolutePath();
      Path filePath = Paths.get(absolutePath);
      LocalHistoryEntry currentEntry = new LocalHistoryEntry();
      BasicFileAttributes currentAttrs = Files.readAttributes(filePath, BasicFileAttributes.class);
      currentEntry.revisionId = "current";
      currentEntry.timestamp = currentAttrs.lastModifiedTime().toMillis();
      currentEntry.formattedTime = formatTimestamp(currentEntry.timestamp);
      currentEntry.fileSize = currentAttrs.size();
      currentEntry.location = absolutePath;
      currentEntry.isCurrent = true;
      entries.add(currentEntry);
      if (maxEntries <= 1) {
         return entries;
      } else {
         List<IFileState> historyStates = getHistoryStates(file);
         historyStates.sort(Comparator.comparingLong(IFileState::getModificationTime).reversed());
         int limit = Math.min(maxEntries - 1, historyStates.size());

         for(int i = 0; i < limit && entries.size() < maxEntries; ++i) {
            IFileState state = (IFileState)historyStates.get(i);
            if (state.exists()) {
               long timestamp = state.getModificationTime();
               LocalHistoryEntry entry = new LocalHistoryEntry();
               entry.revisionId = buildRevisionId(state);
               entry.timestamp = timestamp;
               entry.formattedTime = formatTimestamp(timestamp);
               entry.fileSize = readStateSize(state);
               entry.location = "local_history:" + entry.revisionId;
               entry.isCurrent = false;
               entries.add(entry);
            }
         }

         return entries;
      }
   }

   private static List<IFileState> getHistoryStates(IFile file) throws CoreException {
      IFileState[] states = file.getHistory((IProgressMonitor)null);
      return states != null && states.length != 0 ? new ArrayList(Arrays.asList(states)) : new ArrayList();
   }

   private static String buildRevisionId(IFileState state) {
      return state.getName() + "_" + generateRevisionId(state.getModificationTime());
   }

   private static long readStateSize(IFileState state) {
      try {
         Throwable var1 = null;
         Object var2 = null;

         try {
            InputStream stream = state.getContents();

            long var10000;
            try {
               long total = 0L;

               int read;
               for(byte[] buffer = new byte[8192]; (read = stream.read(buffer)) >= 0; total += (long)read) {
               }

               var10000 = total;
            } finally {
               if (stream != null) {
                  stream.close();
               }

            }

            return var10000;
         } catch (Throwable var15) {
            if (var1 == null) {
               var1 = var15;
            } else if (var1 != var15) {
               var1.addSuppressed(var15);
            }

            throw var1;
         }
      } catch (Exception var16) {
         return -1L;
      }
   }

   private static String generateRevisionId(long timestamp) {
      return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
   }

   private static String formatTimestamp(long timestamp) {
      return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
   }
}
