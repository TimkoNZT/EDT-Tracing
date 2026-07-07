package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import org.eclipse.core.resources.IFile;

public class ProjectFile {
   public static final Comparator<ProjectFile> COMPARATOR = Comparator.comparing((file) -> file.updateTime != null ? file.updateTime : LocalDateTime.MIN);
   public final String path;
   public final AIContext aiCtx;
   public final IFile file;
   private LocalDateTime updateTime;
   private String hash;
   private long modificationStamp = -1L;

   public ProjectFile(AIContext aiCtx, String path, IFile file, LocalDateTime updateTime) {
      Preconditions.checkNotNull(aiCtx);
      Preconditions.checkNotNull(path);
      Preconditions.checkNotNull(file);
      Preconditions.checkNotNull(updateTime);
      this.aiCtx = aiCtx;
      this.path = path;
      this.file = file;
      this.updateTime = updateTime;
   }

   public Duration getAge(LocalDateTime now) {
      return Duration.between(this.updateTime, now);
   }

   public void update(LocalDateTime updateTime, String hash, long modificationStamp) {
      this.updateTime = updateTime;
      this.hash = hash;
      this.modificationStamp = modificationStamp;
   }

   public long getModificationStamp() {
      return this.modificationStamp;
   }

   public String getHash() {
      return this.hash;
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.path});
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         ProjectFile other = (ProjectFile)obj;
         return Objects.equals(this.path, other.path);
      }
   }
}
