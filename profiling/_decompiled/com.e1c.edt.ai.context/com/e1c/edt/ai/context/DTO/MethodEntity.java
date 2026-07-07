package com.e1c.edt.ai.context.DTO;

import com.e1c.edt.ai.assistent.model.IContextEntity;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MethodEntity implements IContextEntity {
   public String uuid;
   public String path;
   public Integer start;
   public Integer finish;
   public String name;
   public String kind;
   public String code;
   public List<String> areas;
   public List<String> environments;
   @SerializedName("signature_str")
   public String signatureStr;
   @SerializedName("signature_structurized")
   public SignatureStructurized signatureStructurized;
   public List<String> comment;
   @SerializedName("сomment_structurized")
   public Comment structurizedComment;
}
