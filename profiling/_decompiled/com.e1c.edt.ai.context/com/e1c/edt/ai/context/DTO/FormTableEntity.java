package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FormTableEntity extends FormGroupEntity {
   @SerializedName("data_path")
   public String dataPath;
   @SerializedName("table_fields")
   public List<FieldEntity> tableFields;
}
