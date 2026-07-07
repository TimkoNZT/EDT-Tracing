package com.e1c.edt.ai.ui;

public enum UINotificationType {
   INFO("INFO"),
   WARNING("WARNING"),
   ERROR("ERROR");

   private final String imageId;

   private UINotificationType(String imageId) {
      this.imageId = imageId;
   }

   public String getImageId() {
      return this.imageId;
   }
}
