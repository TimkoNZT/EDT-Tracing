package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class RegisterDimensionEntity extends ChildEntity {
   @SerializedName("tool_tip")
   public Map<String, String> toolTip;
   public List<DataType> types;
}
