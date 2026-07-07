package com.e1c.edt.ai.context.DTO;

import java.util.Objects;

public class Entity {
   public String ref;
   public String uuid;
   public int start;
   public int finish;

   public int hashCode() {
      return Objects.hash(new Object[]{this.finish, this.ref, this.start, this.uuid});
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         Entity other = (Entity)obj;
         return this.finish == other.finish && Objects.equals(this.ref, other.ref) && this.start == other.start && Objects.equals(this.uuid, other.uuid);
      }
   }

   public String toString() {
      return this.uuid + " - " + this.ref + " [" + this.start + ", " + this.finish + "]";
   }
}
