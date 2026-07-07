package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class FormButtonEntity extends ChildEntity {
   public Map<String, String> title;
   @SerializedName("data_path")
   public String dataPath;
}
