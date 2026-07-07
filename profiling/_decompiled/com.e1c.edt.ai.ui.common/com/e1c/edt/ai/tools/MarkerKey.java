package com.e1c.edt.ai.tools;

import java.util.Objects;

public final class MarkerKey {
   private final String path;
   private final Integer startLine;
   private final String message;

   public MarkerKey(String path, Integer startLine, String message) {
      this.path = path;
      this.startLine = startLine;
      this.message = message;
   }

   public String getPath() {
      return this.path;
   }

   public Integer getStartLine() {
      return this.startLine;
   }

   public String getMessage() {
      return this.message;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj != null && this.getClass() == obj.getClass()) {
         MarkerKey markerKey = (MarkerKey)obj;
         return Objects.equals(this.startLine, markerKey.startLine) && Objects.equals(this.path, markerKey.path) && Objects.equals(this.message, markerKey.message);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.path, this.startLine, this.message});
   }
}
