package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ValueListEntity {
   @SerializedName("item_types")
   public List<DataType> itemTypes;
}
