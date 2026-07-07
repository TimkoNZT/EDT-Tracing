package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DynamicListEntity {
   public String query;
   public List<String> keyField;
   public String keyTypeName;
   @SerializedName("main_table_name")
   public String mainTableName;
   @SerializedName("main_table_name_ru")
   public String mainTableNameRu;
}
