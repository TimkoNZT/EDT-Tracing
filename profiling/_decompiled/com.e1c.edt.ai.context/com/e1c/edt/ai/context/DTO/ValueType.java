package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;

public enum ValueType {
   @SerializedName("null")
   NULL,
   @SerializedName("undefined")
   UNDEFINED,
   @SerializedName("unknown")
   UNKNOWN,
   @SerializedName("boolean")
   BOOLEAN,
   @SerializedName("integer")
   INTEGER,
   @SerializedName("decimal")
   DECIMAL,
   @SerializedName("string")
   STRING,
   @SerializedName("datetime")
   DATETIME,
   @SerializedName("binary")
   BINARY,
   @SerializedName("reference")
   REFERENCE,
   @SerializedName("irresolvable_reference")
   IRRESORVABLE_REFERENCE,
   @SerializedName("list")
   LIST,
   @SerializedName("array")
   ARRAY,
   @SerializedName("type")
   TYPE,
   @SerializedName("standard_period")
   STANDARD_PERIOD,
   @SerializedName("border")
   BORDER,
   @SerializedName("color")
   COLOR,
   @SerializedName("font")
   FONT,
   @SerializedName("account_type")
   ACCOUNT_TYPE,
   @SerializedName("chart_line_type")
   CHART_LINE_TYPE,
   @SerializedName("enum")
   ENUM,
   @SerializedName("sys_enum")
   SYS_ENUM,
   @SerializedName("form_choice_list_des_time")
   FORM_CHOICE_LIST_DES_TIME;
}
