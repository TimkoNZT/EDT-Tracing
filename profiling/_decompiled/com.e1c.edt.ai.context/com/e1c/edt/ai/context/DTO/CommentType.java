package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CommentType {
   public List<CommentDescriptionPart> description;
   @SerializedName("source_description")
   public List<CommentDescriptionPart> sourceDescription;
   @SerializedName("source_extension_description")
   public List<CommentDescriptionPart> sourceExtensionDescription;
   @SerializedName("type_definitions")
   public List<CommentTypeDefinition> typeDefinitions;
}
