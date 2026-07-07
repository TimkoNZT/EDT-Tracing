package com.e1c.edt.ai.tools;

public class JShellBindingDescription {
   private final String description;
   private final String example;
   private final Object value;
   private final Class<?> explicitType;
   private final String restriction;

   public JShellBindingDescription(String description, Object value, Class<?> explicitType) {
      this(description, (String)null, value, explicitType, (String)null);
   }

   public JShellBindingDescription(String description, String example, Object value, Class<?> explicitType) {
      this(description, example, value, explicitType, (String)null);
   }

   public JShellBindingDescription(String description, String example, Object value, Class<?> explicitType, String restriction) {
      this.description = description;
      this.example = example;
      this.value = value;
      this.explicitType = explicitType;
      this.restriction = restriction;
   }

   public String getDescription() {
      return this.description;
   }

   public String getExample() {
      return this.example;
   }

   public Object getValue() {
      return this.value;
   }

   public Class<?> getExplicitType() {
      return this.explicitType;
   }

   public String getRestriction() {
      return this.restriction;
   }
}
