package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GitCommitInfo {
   private final String hash;
   private final String shortHash;
   private final String authorName;
   private final String authorEmail;
   private final long commitTime;
   private final String message;
   private final List<String> changedFiles;

   public GitCommitInfo(String hash, String shortHash, String authorName, String authorEmail, long commitTime, String message, List<String> changedFiles) {
      Preconditions.checkNotNull(hash);
      Preconditions.checkNotNull(shortHash);
      Preconditions.checkNotNull(authorName);
      Preconditions.checkNotNull(authorEmail);
      Preconditions.checkNotNull(message);
      Preconditions.checkNotNull(changedFiles);
      this.hash = hash;
      this.shortHash = shortHash;
      this.authorName = authorName;
      this.authorEmail = authorEmail;
      this.commitTime = commitTime;
      this.message = message;
      this.changedFiles = changedFiles;
   }

   public String getHash() {
      return this.hash;
   }

   public String getShortHash() {
      return this.shortHash;
   }

   public String getAuthorName() {
      return this.authorName;
   }

   public String getAuthorEmail() {
      return this.authorEmail;
   }

   public long getCommitTime() {
      return this.commitTime;
   }

   public String getFormattedTime() {
      return Instant.ofEpochMilli(this.commitTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
   }

   public String getMessage() {
      return this.message;
   }

   public List<String> getChangedFiles() {
      return this.changedFiles;
   }

   public int getChangedFilesCount() {
      return this.changedFiles.size();
   }
}
