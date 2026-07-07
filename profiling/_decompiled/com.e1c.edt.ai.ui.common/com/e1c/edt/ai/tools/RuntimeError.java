package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

class RuntimeError {
   @SerializedName("exception_type")
   public String exceptionType;
   @SerializedName("message")
   public String message;
   @SerializedName("stack_trace")
   public String stackTrace;
}
