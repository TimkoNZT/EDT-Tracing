package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PropertyEntity extends ChildEntity {
   @SerializedName("name_ru")
   public String nameRu;
   public String description;
   @SerializedName("data_paths")
   public List<String> dataPaths;
   public List<DataType> types;
   public List<PropertyEntity> properties;
}
