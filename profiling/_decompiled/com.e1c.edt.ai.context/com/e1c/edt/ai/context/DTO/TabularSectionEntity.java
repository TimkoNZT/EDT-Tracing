package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class TabularSectionEntity extends ChildEntity {
   public List<AttributeEntity> attributes;
   public List<FieldEntity> fields;
   @SerializedName("tool_tip")
   public Map<String, String> toolTip;
}
