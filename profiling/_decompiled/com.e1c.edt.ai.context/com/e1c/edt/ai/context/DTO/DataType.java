package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

public class DataType {
   public String type;
   @SerializedName("type_ru")
   public String typeRu;
   public List<ObjectEntityField> fields;
   public String uuid;
   public List<String> comment;

   public int hashCode() {
      return Objects.hash(new Object[]{this.type, this.typeRu, this.uuid});
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         DataType other = (DataType)obj;
         return Objects.equals(this.type, other.type) && Objects.equals(this.typeRu, other.typeRu) && Objects.equals(this.uuid, other.uuid);
      }
   }
}
