package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BaseExecuteRequest {
   @SerializedName("working_directory")
   public String working_directory;
   @SerializedName("args")
   public List<String> args;
   @SerializedName("timeout")
   public Long timeout;
}
