package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Comment {
   public List<CommentDescriptionPart> description;
   public CommentParameters parameters;
   @SerializedName("example_description")
   public List<CommentDescriptionPart> exampleDescription;
   @SerializedName("call_options_description")
   public List<CommentDescriptionPart> callOptionsDescription;
   @SerializedName("return")
   public CommentReturn returnInfo;
}
