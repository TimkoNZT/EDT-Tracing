package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class JShellExecutionResult extends SessionResult {
   @SerializedName("std_out")
   public String stdOut;
   @SerializedName("std_err")
   public String stdErr;
   @SerializedName("compilation_errors")
   public List<CompilationError> compilationErrors;
   @SerializedName("runtime_errors")
   public List<RuntimeError> runtimeErrors;
}
