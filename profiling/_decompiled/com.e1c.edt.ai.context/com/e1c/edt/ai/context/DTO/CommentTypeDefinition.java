package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CommentTypeDefinition {
   public String name;
   @SerializedName("field_definitions")
   public List<CommentFieldDefinition> fieldDefinitions;
}
