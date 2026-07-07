package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class FormFieldEntity extends ChildEntity {
   @SerializedName("field_type")
   public String fieldType;
   @SerializedName("tool_tip")
   public Map<String, String> toolTip;
   @SerializedName("data_path")
   public String dataPath;
}
