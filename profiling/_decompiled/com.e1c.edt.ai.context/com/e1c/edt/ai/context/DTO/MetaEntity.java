package com.e1c.edt.ai.context.DTO;

import com.e1c.edt.ai.assistent.model.IContextEntity;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MetaEntity extends ChildEntity implements IContextEntity {
   public String namespace;
   public String fullQualifiedName;
   public String path;
   public String type;
   public List<DataType> types;
   public List<AttributeEntity> attributes;
   @SerializedName("standard_attributes")
   public List<AttributeEntity> standardAttributes;
   public List<FieldEntity> fields;
   @SerializedName("tabular_sections")
   public List<TabularSectionEntity> tabularSections;
   @SerializedName("register_resources")
   public List<RegisterResourceEntity> registerResources;
   @SerializedName("register_dimensions")
   public List<RegisterDimensionEntity> registerDimensions;
   @SerializedName("register_records")
   public List<RegisterRecordEntity> registerRecords;
   @SerializedName("enum_values")
   public List<EnumValueEntity> enumValues;
   @SerializedName("object_form")
   public List<ObjectFormEntity> objectForms;
   public List<PredefinedEntity> predefined;
   @SerializedName("based_on")
   public List<MetaEntity> basedOn;
   @SerializedName("subsystem_objects")
   public List<String> subsystemObjects;
   public List<TemplateEntity> templates;
   public List<ColumnEntity> columns;
   public String event;
   public String handler;
   public String description;
   public String key;
   @SerializedName("method_name")
   public String methodName;
}
