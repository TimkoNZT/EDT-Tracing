package com.e1c.edt.ai.tools;

public enum MarkerType {
   UNKNOWN((String)null, "Unknown marker type.", "unknown"),
   MARKER("org.eclipse.core.resources.marker", "General marker.", "marker"),
   TASK("org.eclipse.core.resources.taskmarker", "ALWAYS use it when planning plans, schedules, proposals, tasks, TODO, etc.", "task"),
   PROBLEM("org.eclipse.core.resources.problemmarker", "Contains information about build issues.", "problem"),
   TEXT("org.eclipse.core.resources.textmarker", "Text annotation marker.", "text"),
   BOOKMARK("org.eclipse.core.resources.bookmark", "ALWAYS use it for summaries, reports.", "bookmark"),
   M1C("1c", "1C marker.", "1c"),
   AI_MARKER("com.e1c.edt.ai.AIMarker", "ALWAYS use it to show any issues, problems, errors, warnings, etc.", "ai_marker");

   public static final String AI_MARKER_BASE = "com.e1c.edt.ai.AIMarker";
   public static final String AI_MARKER_ERROR = "com.e1c.edt.ai.AIError";
   public static final String AI_MARKER_WARNING = "com.e1c.edt.ai.AIWarning";
   public static final String AI_MARKER_INFO = "com.e1c.edt.ai.AIInfo";
   public static final String M1C_MARKER_BASE = "com._1c.g5.v8.dt.bsl.ui.bsl.";
   public static final String M1C_MARKER_INFO = "1c";
   private final String typeId;
   private final String description;
   private final String displayName;

   private MarkerType(String typeId, String description, String displayName) {
      this.typeId = typeId;
      this.description = description;
      this.displayName = displayName;
   }

   public String getTypeId() {
      return this.typeId;
   }

   public String getDescription() {
      return this.description;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public static MarkerType fromTypeId(String typeId) {
      if (typeId != null) {
         if (typeId.startsWith("com.e1c.edt.ai.AIMarker")) {
            return AI_MARKER;
         }

         if (typeId.startsWith("com._1c.g5.v8.dt.bsl.ui.bsl.")) {
            return M1C;
         }
      }

      MarkerType[] var4;
      for(MarkerType type : var4 = values()) {
         if (typeId.equals(type.typeId)) {
            return type;
         }
      }

      return UNKNOWN;
   }

   public static MarkerType fromDisplayName(String displayName) {
      if (displayName == null) {
         return UNKNOWN;
      } else {
         MarkerType[] var4;
         for(MarkerType type : var4 = values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
               return type;
            }
         }

         return UNKNOWN;
      }
   }

   public static String[] getAiMarkerTypeIds() {
      return new String[]{"com.e1c.edt.ai.AIError", "com.e1c.edt.ai.AIWarning", "com.e1c.edt.ai.AIInfo"};
   }
}
