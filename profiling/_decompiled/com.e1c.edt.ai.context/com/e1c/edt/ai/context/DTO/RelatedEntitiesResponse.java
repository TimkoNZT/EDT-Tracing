package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RelatedEntitiesResponse {
   public String code;
   @SerializedName("related_objects")
   public List<Entity> relatedObjects;
   @SerializedName("related_functions")
   public List<Entity> relatedFunctions;
   @SerializedName("local_functions")
   public List<MethodEntity> localFunctions;
   public FormEntity form;
   public MetaEntity meta;
}
