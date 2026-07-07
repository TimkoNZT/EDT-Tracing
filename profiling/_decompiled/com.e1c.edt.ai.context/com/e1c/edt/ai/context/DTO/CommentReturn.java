package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CommentReturn {
   @SerializedName("return_description")
   public List<CommentDescriptionPart> returnDescription;
   @SerializedName("return_types")
   public List<CommentType> returnTypes;
}
