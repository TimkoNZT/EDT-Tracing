package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ChildEntity {
   public String id;
   public String name;
   public String comment;
   public Map<String, String> synonym;
   @SerializedName("container")
   public String container;
   @SerializedName("parent_id")
   public String parentId;
}
