package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

public class SessionResult {
   @SerializedName("repl_session_id")
   public int sessionId;
   @SerializedName("available_bindings")
   public ArrayList<String> availableBindings;
   @SerializedName("execution_history")
   public ArrayList<String> executionHistory;
}
