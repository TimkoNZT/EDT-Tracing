package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.ecore.EObject;

public class FormGroupEntity extends ChildEntity {
   public String kind;
   public Map<String, String> title;
   @SerializedName("tool_tip")
   public Map<String, String> toolTip;
   public List<FormFieldEntity> fields;
   public List<FormGroupEntity> groups;
   public List<FormButtonEntity> buttons;
   public transient EObject ref;
}
