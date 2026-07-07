package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FieldEntity extends ChildEntity {
   @SerializedName("name_ru")
   public String nameRu;
   public List<DataType> types;
}
