package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmNamespace;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.Procedure;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.bsl.util.BslUtil;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.AbstractDataPath;
import com._1c.g5.v8.dt.form.model.AccountTypeValue;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.DynamicListExtInfo;
import com._1c.g5.v8.dt.form.model.DynamicListKeyType;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormAttributeExtInfo;
import com._1c.g5.v8.dt.form.model.FormChoiceListDesTimeValue;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.FormParameter;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.ManagedFormFieldType;
import com._1c.g5.v8.dt.form.model.MultiLanguageDataPath;
import com._1c.g5.v8.dt.form.model.PropertyInfo;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.form.model.ValueListExtInfo;
import com._1c.g5.v8.dt.form.model.PropertyInfo.PropertyInfoType;
import com._1c.g5.v8.dt.form.service.datasourceinfo.IDataSourceInfoAssociationService;
import com._1c.g5.v8.dt.mcore.BinaryValue;
import com._1c.g5.v8.dt.mcore.BooleanValue;
import com._1c.g5.v8.dt.mcore.Border;
import com._1c.g5.v8.dt.mcore.BorderValue;
import com._1c.g5.v8.dt.mcore.Color;
import com._1c.g5.v8.dt.mcore.ColorValue;
import com._1c.g5.v8.dt.mcore.ContextDef;
import com._1c.g5.v8.dt.mcore.DateValue;
import com._1c.g5.v8.dt.mcore.Field;
import com._1c.g5.v8.dt.mcore.FieldSource;
import com._1c.g5.v8.dt.mcore.FixedArrayValue;
import com._1c.g5.v8.dt.mcore.Font;
import com._1c.g5.v8.dt.mcore.FontValue;
import com._1c.g5.v8.dt.mcore.IrresolvableReferenceValue;
import com._1c.g5.v8.dt.mcore.NullValue;
import com._1c.g5.v8.dt.mcore.NumberValue;
import com._1c.g5.v8.dt.mcore.ParamSet;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.ReferenceValue;
import com._1c.g5.v8.dt.mcore.StandardPeriod;
import com._1c.g5.v8.dt.mcore.StandardPeriodValue;
import com._1c.g5.v8.dt.mcore.StringValue;
import com._1c.g5.v8.dt.mcore.SysEnumValue;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeDescriptionValue;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.UndefinedValue;
import com._1c.g5.v8.dt.mcore.ValueList;
import com._1c.g5.v8.dt.mcore.util.Environment;
import com._1c.g5.v8.dt.metadata.common.AccountType;
import com._1c.g5.v8.dt.metadata.common.ChartLineType;
import com._1c.g5.v8.dt.metadata.common.ChartLineTypeValue;
import com._1c.g5.v8.dt.metadata.dbview.DbViewDef;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicForm;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.BusinessProcess;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccountsPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypes;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypesPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypes;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypesPredefinedItem;
import com._1c.g5.v8.dt.metadata.mdclass.Column;
import com._1c.g5.v8.dt.metadata.mdclass.Constant;
import com._1c.g5.v8.dt.metadata.mdclass.DataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentJournal;
import com._1c.g5.v8.dt.metadata.mdclass.Enum;
import com._1c.g5.v8.dt.metadata.mdclass.EnumValue;
import com._1c.g5.v8.dt.metadata.mdclass.EventSubscription;
import com._1c.g5.v8.dt.metadata.mdclass.ExchangePlan;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalDataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalReport;
import com._1c.g5.v8.dt.metadata.mdclass.FilterCriterion;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.Report;
import com._1c.g5.v8.dt.metadata.mdclass.ReportTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.ScheduledJob;
import com._1c.g5.v8.dt.metadata.mdclass.SessionParameter;
import com._1c.g5.v8.dt.metadata.mdclass.StandardAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com._1c.g5.v8.dt.metadata.mdclass.Task;
import com._1c.g5.v8.dt.metadata.mdclass.Template;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.e1c.edt.ai.assistent.model.IContextEntity;
import com.e1c.edt.ai.context.DTO.AccountTypeEntity;
import com.e1c.edt.ai.context.DTO.AttributeEntity;
import com.e1c.edt.ai.context.DTO.BorderEntity;
import com.e1c.edt.ai.context.DTO.ChartLineTypeEntity;
import com.e1c.edt.ai.context.DTO.ChildEntity;
import com.e1c.edt.ai.context.DTO.ColorEntity;
import com.e1c.edt.ai.context.DTO.ColumnEntity;
import com.e1c.edt.ai.context.DTO.DataType;
import com.e1c.edt.ai.context.DTO.DynamicListEntity;
import com.e1c.edt.ai.context.DTO.EnumValueEntity;
import com.e1c.edt.ai.context.DTO.FieldEntity;
import com.e1c.edt.ai.context.DTO.FontEntity;
import com.e1c.edt.ai.context.DTO.FormButtonEntity;
import com.e1c.edt.ai.context.DTO.FormEntity;
import com.e1c.edt.ai.context.DTO.FormFieldEntity;
import com.e1c.edt.ai.context.DTO.FormGroupEntity;
import com.e1c.edt.ai.context.DTO.FormParameterEntity;
import com.e1c.edt.ai.context.DTO.FormTableEntity;
import com.e1c.edt.ai.context.DTO.MetaEntity;
import com.e1c.edt.ai.context.DTO.MethodEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntityField;
import com.e1c.edt.ai.context.DTO.ObjectFormEntity;
import com.e1c.edt.ai.context.DTO.Parameter;
import com.e1c.edt.ai.context.DTO.PredefinedEntity;
import com.e1c.edt.ai.context.DTO.PropertyEntity;
import com.e1c.edt.ai.context.DTO.RegisterDimensionEntity;
import com.e1c.edt.ai.context.DTO.RegisterRecordEntity;
import com.e1c.edt.ai.context.DTO.RegisterResourceEntity;
import com.e1c.edt.ai.context.DTO.SignatureStructurized;
import com.e1c.edt.ai.context.DTO.StandardPeriodEntity;
import com.e1c.edt.ai.context.DTO.TabularSectionEntity;
import com.e1c.edt.ai.context.DTO.TemplateEntity;
import com.e1c.edt.ai.context.DTO.ValueEntity;
import com.e1c.edt.ai.context.DTO.ValueListEntity;
import com.e1c.edt.ai.context.DTO.ValueType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.Pair;

class EntityFactory implements IEntityFactory {
   private final IV8Model v8Model;
   private final IIdFactory idFactory;
   private final ICommentFactory commentFactory;
   private final IFormWalker formWalker;
   private final ICodePartsProvider codePartsProvider;
   private final IDataSourceInfoAssociationService dataSourceInfoAssociationService;
   private final IV8ProjectManager v8ProjectManager;
   private final IModuleProvider moduleProvider;
   private final IQualifiedNameFilePathConverter qualifiedNameFilePathConverter;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$_1c$g5$v8$dt$form$model$PropertyInfo$PropertyInfoType;

   @Inject
   public EntityFactory(IV8Model v8Model, IIdFactory idFactory, ICommentFactory commentFactory, IFormWalker formWalker, ICodePartsProvider codePartsProvider, IDataSourceInfoAssociationService dataSourceInfoAssociationService, IV8ProjectManager v8ProjectManager, IModuleProvider moduleProvider, IQualifiedNameFilePathConverter qualifiedNameFilePathConverter) {
      Preconditions.checkNotNull(v8Model);
      Preconditions.checkNotNull(idFactory);
      Preconditions.checkNotNull(commentFactory);
      Preconditions.checkNotNull(formWalker);
      Preconditions.checkNotNull(codePartsProvider);
      Preconditions.checkNotNull(dataSourceInfoAssociationService);
      Preconditions.checkNotNull(v8ProjectManager);
      Preconditions.checkNotNull(moduleProvider);
      Preconditions.checkNotNull(qualifiedNameFilePathConverter);
      this.v8Model = v8Model;
      this.idFactory = idFactory;
      this.commentFactory = commentFactory;
      this.formWalker = formWalker;
      this.codePartsProvider = codePartsProvider;
      this.dataSourceInfoAssociationService = dataSourceInfoAssociationService;
      this.v8ProjectManager = v8ProjectManager;
      this.moduleProvider = moduleProvider;
      this.qualifiedNameFilePathConverter = qualifiedNameFilePathConverter;
   }

   public Optional<FormEntity> createFormEntity(Form form, ICancellationToken cancellationToken) {
      FormEntity formEntity = new FormEntity();
      formEntity.id = this.getId(form);
      formEntity.title = this.createMap(form.getTitle());
      formEntity.parameters = this.createFormParameters(form.getParameters());
      EList<FormAttribute> attributes = form.getAttributes();
      if (attributes != null && !attributes.isEmpty()) {
         if (formEntity.attributes == null) {
            formEntity.attributes = new ArrayList();
         }

         boolean hasMainAttribute = false;

         for(FormAttribute attribute : attributes) {
            hasMainAttribute |= attribute.isMain();
         }

         for(FormAttribute attribute : attributes) {
            formEntity.attributes.add(this.createAttribute(form, attribute, hasMainAttribute));
         }
      }

      final HashMap<EObject, FormGroupEntity> groups = new HashMap();
      groups.put(form, formEntity);
      this.formWalker.walk(form, new FormVisitor() {
         public void visitFormField(Optional<EObject> parent, FormField field) {
            parent.map((p) -> (FormGroupEntity)groups.get(p)).ifPresent((group) -> EntityFactory.this.addField(group, EntityFactory.this.createField(field)));
         }

         public void visitButton(Optional<EObject> parent, Button button) {
            parent.map((p) -> (FormGroupEntity)groups.get(p)).ifPresent((group) -> EntityFactory.this.addButton(group, EntityFactory.this.createButton(button)));
         }

         public void visitGroup(Optional<EObject> parent, Group group) {
            parent.map((p) -> (FormGroupEntity)groups.get(p)).ifPresent((parentNode) -> {
               FormGroupEntity node = EntityFactory.this.createGroup(group);
               if (parentNode.groups == null) {
                  parentNode.groups = new ArrayList();
               }

               parentNode.groups.add(node);
               groups.put(group, node);
            });
         }

         public void visitTable(Optional<EObject> parent, Table table) {
            parent.map((p) -> (FormGroupEntity)groups.get(p)).ifPresent((parentNode) -> {
               FormTableEntity node = EntityFactory.this.createTable(table);
               if (parentNode.groups == null) {
                  parentNode.groups = new ArrayList();
               }

               parentNode.groups.add(node);
               groups.put(table, node);
            });
         }
      }, cancellationToken);
      formEntity.ref = form;
      this.fillParent(formEntity.parameters, form, "parameters");
      this.fillParent(formEntity.attributes, form, "attributes");
      LinkedList<FormGroupEntity> grps = new LinkedList();
      grps.addLast(formEntity);

      while(!grps.isEmpty()) {
         FormGroupEntity grp = (FormGroupEntity)grps.removeFirst();
         if (grp.groups != null) {
            for(FormGroupEntity nestedGrp : grp.groups) {
               grps.addLast(nestedGrp);
            }
         }

         EObject parent = grp.ref;
         if (grp.ref != null) {
            this.fillParent(grp.fields, parent, "fields");
            this.fillParent(grp.groups, parent, "groups");
            this.fillParent(grp.buttons, parent, "buttons");
         }
      }

      return Optional.of(formEntity);
   }

   private List<FormParameterEntity> createFormParameters(List<FormParameter> parameters) {
      return parameters == null ? null : (List)parameters.stream().map(this::createFormParameter).collect(Collectors.toList());
   }

   private FormParameterEntity createFormParameter(FormParameter parameter) {
      FormParameterEntity entity = new FormParameterEntity();
      entity.name = parameter.getName();
      entity.comment = parameter.getComment();
      entity.types = this.createTypes(parameter.getValueType());
      return entity;
   }

   private void addField(FormGroupEntity group, FormFieldEntity field) {
      if (group.fields == null) {
         group.fields = new ArrayList();
      }

      group.fields.add(field);
   }

   private void addButton(FormGroupEntity group, FormButtonEntity button) {
      if (group.buttons == null) {
         group.buttons = new ArrayList();
      }

      group.buttons.add(button);
   }

   private AttributeEntity createAttribute(Form form, FormAttribute attribute, boolean hasMainAttribute) {
      AttributeEntity attr = new AttributeEntity();
      attr.id = this.getId(attribute);
      attr.name = attribute.getName();
      if (attribute.isMain()) {
         attr.isMain = true;
      }

      attr.title = this.createMap(attribute.getTitle());
      attr.types = this.createTypes(attribute.getValueType());
      if (!attribute.isMain()) {
         try {
            PropertyInfo proprtyInfo = this.dataSourceInfoAssociationService.findPropertyInfo(form, attribute);
            if (proprtyInfo != null) {
               this.fillProperty(attr, proprtyInfo, hasMainAttribute ? 1 : 2);
            }
         } catch (Exception var11) {
         }

         FormAttributeExtInfo extInfo = attribute.getExtInfo();
         if (extInfo != null) {
            if (extInfo instanceof DynamicListExtInfo) {
               DynamicListExtInfo info = (DynamicListExtInfo)extInfo;
               DynamicListEntity dynamicList = new DynamicListEntity();
               attr.dynamicList = dynamicList;
               dynamicList.query = info.getQueryText();
               dynamicList.keyField = info.getKeyField();
               DynamicListKeyType keyType = info.getKeyType();
               if (keyType != null) {
                  dynamicList.keyTypeName = keyType.getName();
               }

               try {
                  DbViewDef mainTable = info.getMainTable();
                  if (mainTable != null) {
                     dynamicList.mainTableName = mainTable.getName();
                     dynamicList.mainTableNameRu = mainTable.getNameRu();
                  }
               } catch (AssertionError var10) {
               }
            }

            if (extInfo instanceof ValueListExtInfo) {
               ValueListExtInfo info = (ValueListExtInfo)extInfo;
               ValueListEntity valueList = new ValueListEntity();
               attr.valueList = valueList;
               valueList.itemTypes = this.createTypes(info.getItemValueType());
            }
         }
      }

      return attr;
   }

   private List<String> getDataPaths(MultiLanguageDataPath dataPath) {
      if (dataPath == null) {
         return null;
      } else {
         EList<AbstractDataPath> paths = dataPath.getPaths();
         return paths != null && !paths.isEmpty() ? (List)paths.stream().map((path) -> path.toString()).collect(Collectors.toList()) : null;
      }
   }

   private void fillProperty(PropertyEntity propery, PropertyInfo propertyInfo, int dept) {
      propery.name = propertyInfo.getName();
      propery.nameRu = propertyInfo.getNameRu();
      propery.description = propertyInfo.getStaticDescription();
      propery.dataPaths = this.getDataPaths(propertyInfo.getMultyLanguageDataPath());
      propery.types = this.createTypes(propertyInfo.getValueType());
      if (dept > 0 && !"Ref".equals(propery.name)) {
         switch (propertyInfo.getType()) {
            case COMMON_TABLE_TYPE_PROPERTY:
            case COLUMN_TABLE_TYPE_PROPERTY:
               dept = 0;
               break;
            case COMMON_DUAL_TYPE_PROPERTY:
            case COLUMN_PROPERTY:
            case COLUMN_NON_VISUAL_PROPERTY:
            default:
               --dept;
         }

         List<PropertyInfo> propInfos = propertyInfo.getPropertyInfos();
         if (propInfos != null && !propInfos.isEmpty()) {
            propery.properties = new ArrayList();

            for(PropertyInfo propInfo : propInfos) {
               PropertyEntity prop = new PropertyEntity();
               this.fillProperty(prop, propInfo, dept);
               propery.properties.add(prop);
            }
         }

      }
   }

   private FormFieldEntity createField(FormField field) {
      FormFieldEntity entity = new FormFieldEntity();
      entity.id = this.getId(field);
      entity.name = field.getName();
      entity.toolTip = this.createMap(field.getToolTip());
      ManagedFormFieldType fiedType = field.getType();
      AbstractDataPath dataPath = field.getDataPath();
      if (dataPath != null) {
         entity.dataPath = dataPath.toString();
      }

      if (fiedType != null) {
         entity.fieldType = fiedType.getName();
      }

      return entity;
   }

   private FormGroupEntity createGroup(Group group) {
      FormGroupEntity entity = new FormGroupEntity();
      entity.id = this.getId(group);
      entity.name = group.getName();
      entity.kind = group.getClass().getSimpleName();
      entity.title = this.createMap(group.getTitle());
      entity.toolTip = this.createMap(group.getToolTip());
      if (group instanceof MdObject) {
         entity.ref = group;
      }

      return entity;
   }

   private FormButtonEntity createButton(Button button) {
      FormButtonEntity entity = new FormButtonEntity();
      entity.id = this.getId(button);
      entity.name = button.getName();
      entity.title = this.createMap(button.getTitle());
      AbstractDataPath dataPath = button.getDataPath();
      if (dataPath != null) {
         entity.dataPath = dataPath.toString();
      }

      return entity;
   }

   private FormTableEntity createTable(Table table) {
      FormTableEntity entity = new FormTableEntity();
      entity.id = this.getId(table);
      entity.name = table.getName();
      entity.kind = table.getClass().getSimpleName();
      entity.title = this.createMap(table.getTitle());
      entity.toolTip = this.createMap(table.getToolTip());
      AbstractDataPath dataPath = table.getDataPath();
      if (dataPath != null) {
         entity.dataPath = dataPath.toString();
      }

      entity.tableFields = this.createFields(table, (field) -> this.isPublishedField(field));
      return entity;
   }

   public Optional<ObjectEntity> crateObjectEntity(Variable variable, ICompositeNode node, boolean detailed, ICancellationToken cancellationToken) {
      ObjectEntity entity = new ObjectEntity();
      entity.name = variable.getName();
      if (detailed) {
         entity.start = node.getTotalOffset();
         entity.finish = node.getTotalEndOffset();
         entity.code = node.getText();
      }

      List<String> comment = this.v8Model.getComment(variable);
      if (comment != null && !comment.isEmpty()) {
         entity.comment = comment;
      }

      List<Type> types = this.v8Model.getTypes(variable.getTypeStateProvider(), node);
      this.fillType(variable, entity, types, cancellationToken);
      return Optional.of(entity);
   }

   public Optional<ObjectEntity> crateObjectEntity(FeatureAccess featureAccess, ICompositeNode node, boolean detailed, ICancellationToken cancellationToken) {
      ObjectEntity objectEntity = new ObjectEntity();
      objectEntity.name = featureAccess.getName();
      if (detailed) {
         objectEntity.start = node.getTotalOffset();
         objectEntity.finish = node.getTotalEndOffset();
         objectEntity.code = node.getText();
      }

      List<String> comment = this.v8Model.getComment(featureAccess);
      if (comment != null && !comment.isEmpty()) {
         objectEntity.comment = comment;
      }

      List<Type> types = this.v8Model.getTypes(featureAccess);
      this.fillType(featureAccess, objectEntity, types, cancellationToken);
      return Optional.of(objectEntity);
   }

   public Optional<MethodEntity> createMethodEntity(Invocation invocation, ICompositeNode node, boolean detailed, ICancellationToken cancellationToken) {
      FeatureAccess methodAccess = invocation.getMethodAccess();
      Optional<EObject> methodAccessFeatureOptional = this.v8Model.getMethodFeature(methodAccess, cancellationToken);
      MethodEntity methodEntity = new MethodEntity();
      this.v8Model.getPath(methodAccess).ifPresent((path) -> methodEntity.path = path);
      boolean hasData = false;
      boolean hasSignatureStructurized = false;
      if (methodAccessFeatureOptional.isPresent()) {
         EObject methodAccessFeature = (EObject)methodAccessFeatureOptional.get();
         SignatureStructurized signatureStructurized = new SignatureStructurized();
         methodEntity.signatureStructurized = signatureStructurized;
         signatureStructurized.preprocess = new ArrayList();
         ArrayList<Parameter> parameters = new ArrayList();
         signatureStructurized.parameters = parameters;
         signatureStructurized.attributes = new ArrayList();
         if (methodAccessFeature instanceof Method) {
            Method method = (Method)methodAccessFeature;
            ICompositeNode methodNode = NodeModelUtils.getNode(methodAccessFeature);
            this.fillMethod(methodEntity, method, detailed, methodNode, node, cancellationToken);
            List<TypeItem> returnTypes = this.v8Model.getTypesComputer().compute(invocation, this.v8Model.getEnvironments(invocation));
            signatureStructurized.returnTypes = this.createDataTypesFromTypeItemsSafety(returnTypes, true);
            hasSignatureStructurized = true;
            hasData = true;
         }

         if (methodAccessFeature instanceof com._1c.g5.v8.dt.mcore.Method) {
            com._1c.g5.v8.dt.mcore.Method method = (com._1c.g5.v8.dt.mcore.Method)methodAccessFeature;
            methodEntity.name = method.getName();
            EList<ParamSet> paramsSet = method.getParamSet();
            if (paramsSet != null && !paramsSet.isEmpty()) {
               ParamSet paramSet = (ParamSet)paramsSet.get(paramsSet.size() - 1);

               for(com._1c.g5.v8.dt.mcore.Parameter param : paramSet.getParams()) {
                  Parameter parameter = new Parameter();
                  parameters.add(parameter);
                  parameter.name = param.getName();
                  parameter.types = this.createDataTypesFromTypeItemsSafety(param.getType(), true);
               }
            }

            this.getAreas(method, cancellationToken).ifPresent((areas) -> methodEntity.areas = areas);
            List<TypeItem> returnTypes = this.v8Model.getTypesComputer().compute(invocation, this.v8Model.getEnvironments(invocation));
            signatureStructurized.returnTypes = this.createDataTypesFromTypeItemsSafety(returnTypes, true);
            hasSignatureStructurized = true;
            if (method instanceof BslContextDefMethod) {
               BslContextDefMethod defMethod = (BslContextDefMethod)method;
               methodEntity.comment = defMethod.getCommentLines();
               methodEntity.structurizedComment = this.commentFactory.create(this.v8Model.getComment(defMethod, true));
            }

            hasData = true;
         }
      }

      if (methodEntity.signatureStructurized == null || !hasSignatureStructurized) {
         SimpleStatement simpleStatement = (SimpleStatement)EcoreUtil2.getContainerOfType(invocation, SimpleStatement.class);
         if (simpleStatement != null) {
            Expression target = simpleStatement.getLeft();
            if (target != null) {
               List<DataType> types = this.createDataTypesFromTypeItemsSafety(this.v8Model.getTypes((EObject)target), true);
               if (types != null) {
                  SignatureStructurized signatureStructurized = new SignatureStructurized();
                  methodEntity.signatureStructurized = signatureStructurized;
                  signatureStructurized.returnTypes = types;
                  hasData = true;
               }
            }
         }
      }

      return !hasData ? Optional.empty() : Optional.of(methodEntity);
   }

   public Optional<MethodEntity> createMethodEntity(Method method, ICompositeNode node, boolean detailed, ICancellationToken cancellationToken) {
      MethodEntity methodEntity = new MethodEntity();
      SignatureStructurized signatureStructurized = new SignatureStructurized();
      methodEntity.signatureStructurized = signatureStructurized;
      signatureStructurized.preprocess = new ArrayList();
      signatureStructurized.parameters = new ArrayList();
      signatureStructurized.attributes = new ArrayList();
      this.fillMethod(methodEntity, method, detailed, node, node, cancellationToken);
      List<TypeItem> returnTypes = this.v8Model.getTypesComputer().compute(method, this.v8Model.getEnvironments(method));
      signatureStructurized.returnTypes = this.createDataTypesFromTypeItemsSafety(returnTypes, true);
      return Optional.of(methodEntity);
   }

   public MetaEntity createMetaEntity(IBmObject bmObject, ICancellationToken cancellationToken) {
      return this.createAndFillMetaEntity(bmObject, false);
   }

   private MetaEntity createAndFillMetaEntity(IBmObject bmObject, boolean brief) {
      MetaEntity entity = brief ? new MetaEntity() : this.createMetaEntity(bmObject, brief);
      entity.type = this.getEntityType(bmObject);
      entity.id = this.getId(bmObject);
      IBmNamespace namespace = bmObject.bmGetNamespace();
      if (namespace != null) {
         entity.namespace = namespace.getName();
         IBmObject top = bmObject.bmIsTop() ? bmObject : bmObject.bmGetTopObject();
         entity.fullQualifiedName = top.bmGetFqn();
         if (entity.fullQualifiedName != null) {
            entity.path = this.qualifiedNameFilePathConverter.getFilePath(entity.fullQualifiedName);
         }
      }

      if (bmObject instanceof MdObject) {
         MdObject mdObject = (MdObject)bmObject;
         entity.name = mdObject.getName();
         entity.comment = mdObject.getComment();
         entity.synonym = this.createMap(mdObject.getSynonym());
      }

      return entity;
   }

   private String getEntityType(EObject eObject) {
      Class[] var5;
      for(Class<?> metadataInterface : var5 = eObject.getClass().getInterfaces()) {
         if (metadataInterface.getName().startsWith("com._1c.g5.v8.dt.metadata.mdclass.")) {
            return metadataInterface.getSimpleName();
         }
      }

      return null;
   }

   private String getId(EObject eObject) {
      if (eObject instanceof MdObject) {
         MdObject mdObject = (MdObject)eObject;
         return mdObject.getUuid() != null ? mdObject.getUuid().toString() : null;
      } else if (eObject instanceof FormItem) {
         FormItem formItem = (FormItem)eObject;
         return Integer.toString(formItem.getId());
      } else {
         return null;
      }
   }

   private MetaEntity createMetaEntity(IBmObject bmObject, boolean brief) {
      if (bmObject instanceof AccountingRegister) {
         return this.createAccountingRegister((AccountingRegister)bmObject);
      } else if (bmObject instanceof AccumulationRegister) {
         return this.createAccumulationRegister((AccumulationRegister)bmObject);
      } else if (bmObject instanceof BusinessProcess) {
         return this.createBusinessProcess((BusinessProcess)bmObject);
      } else if (bmObject instanceof CalculationRegister) {
         return this.createCalculationRegister((CalculationRegister)bmObject);
      } else if (bmObject instanceof Catalog) {
         return this.createCatalog((Catalog)bmObject);
      } else if (bmObject instanceof ChartOfAccounts) {
         return this.createChartOfAccounts((ChartOfAccounts)bmObject);
      } else if (bmObject instanceof ChartOfCalculationTypes) {
         return this.createChartOfCalculationTypes((ChartOfCalculationTypes)bmObject);
      } else if (bmObject instanceof ChartOfCharacteristicTypes) {
         return this.createChartOfCharacteristicTypes((ChartOfCharacteristicTypes)bmObject);
      } else if (bmObject instanceof DataProcessor) {
         return this.createDataProcessor((DataProcessor)bmObject);
      } else if (bmObject instanceof Document) {
         MetaEntity meta = this.createDocument((Document)bmObject);
         return meta;
      } else if (bmObject instanceof ExchangePlan) {
         return this.createExchangePlan((ExchangePlan)bmObject);
      } else if (bmObject instanceof ExternalDataProcessor) {
         return this.createExternalDataProcessor((ExternalDataProcessor)bmObject);
      } else if (bmObject instanceof ExternalReport) {
         return this.createExternalReport((ExternalReport)bmObject);
      } else if (bmObject instanceof InformationRegister) {
         return this.createInformationRegister((InformationRegister)bmObject);
      } else if (bmObject instanceof Report) {
         return this.createReport((Report)bmObject);
      } else if (bmObject instanceof ReportTabularSection) {
         return this.createReportTabularSection((ReportTabularSection)bmObject);
      } else if (bmObject instanceof Task) {
         return this.createTask((Task)bmObject);
      } else if (bmObject instanceof Enum) {
         return this.createEnum((Enum)bmObject);
      } else if (bmObject instanceof Subsystem) {
         return this.createSubsystem((Subsystem)bmObject);
      } else if (bmObject instanceof SessionParameter) {
         return this.createSessionParameter((SessionParameter)bmObject);
      } else if (bmObject instanceof Constant) {
         return this.createConstant((Constant)bmObject);
      } else if (bmObject instanceof DocumentJournal) {
         return this.createDocumentJournal((DocumentJournal)bmObject);
      } else if (bmObject instanceof FilterCriterion) {
         return this.createFilterCriterion((FilterCriterion)bmObject);
      } else if (bmObject instanceof EventSubscription) {
         return this.createEventSubscription((EventSubscription)bmObject);
      } else {
         return bmObject instanceof ScheduledJob ? this.createScheduledJob((ScheduledJob)bmObject) : new MetaEntity();
      }
   }

   private <T extends ChildEntity> List<T> fillParent(List<T> children, EObject eObject, String targetField) {
      if (eObject != null && children != null && !children.isEmpty()) {
         String parenId = this.getId(eObject);

         for(T child : children) {
            child.container = targetField;
            child.parentId = parenId;
         }

         return children;
      } else {
         return children;
      }
   }

   private MetaEntity createScheduledJob(ScheduledJob bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.description = bmObject.getDescription();
      meta.key = bmObject.getKey();
      meta.methodName = bmObject.getMethodName();
      return meta;
   }

   private MetaEntity createEventSubscription(EventSubscription bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.event = bmObject.getEvent();
      meta.handler = bmObject.getHandler();
      meta.types = this.createTypes(bmObject.getSource());
      return meta;
   }

   private MetaEntity createFilterCriterion(FilterCriterion bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.types = this.createTypes(bmObject.getType());
      return meta;
   }

   private MetaEntity createDocumentJournal(DocumentJournal bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.fields = this.<FieldEntity>fillParent(this.createFields(bmObject, (field) -> this.isPublishedField(field)), bmObject, "fields");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.columns = this.<ColumnEntity>fillParent(this.createColumns(bmObject.getColumns()), bmObject, "columns");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createConstant(Constant bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.types = this.createTypes(bmObject.getTypeDescription());
      return meta;
   }

   private MetaEntity createSessionParameter(SessionParameter bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.types = this.createTypes(bmObject.getTypeDescription());
      return meta;
   }

   private MetaEntity createSubsystem(Subsystem bmObject) {
      MetaEntity meta = new MetaEntity();
      ArrayList<String> subsystemObjects = new ArrayList();

      for(MdObject mdObject : bmObject.getContent()) {
         String type = this.getEntityType(mdObject);
         StringBuilder name = new StringBuilder();
         if (type != null) {
            name.append(type);
            name.append('.');
         }

         name.append(mdObject.getName());
         subsystemObjects.add(name.toString());
      }

      if (subsystemObjects != null) {
         meta.subsystemObjects = subsystemObjects;
      }

      return meta;
   }

   private MetaEntity createEnum(Enum bmObject) {
      MetaEntity meta = new MetaEntity();
      EList<EnumValue> enumValues = bmObject.getEnumValues();
      if (enumValues != null) {
         meta.enumValues = this.<EnumValueEntity>fillParent((List)enumValues.stream().map(this::createEnumValue).collect(Collectors.toList()), bmObject, "enum_values");
      }

      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      return meta;
   }

   private MetaEntity createTask(Task bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createReportTabularSection(ReportTabularSection bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      return meta;
   }

   private MetaEntity createReport(Report bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createInformationRegister(InformationRegister bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.registerResources = this.<RegisterResourceEntity>fillParent(this.createRegisterResources(bmObject.getResources()), bmObject, "register_resources");
      meta.registerDimensions = this.<RegisterDimensionEntity>fillParent(this.createRegisterDimensions(bmObject.getDimensions()), bmObject, "register_dimensions");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createExternalReport(ExternalReport bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createExternalDataProcessor(ExternalDataProcessor bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createExchangePlan(ExchangePlan bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createDocument(Document bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.fields = this.<FieldEntity>fillParent(this.createFields(bmObject, (field) -> this.isPublishedField(field)), bmObject, "fields");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.registerRecords = this.<RegisterRecordEntity>fillParent(this.createRegisterRecords(bmObject.getRegisterRecords()), bmObject, "register_records");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createDataProcessor(DataProcessor bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createChartOfCharacteristicTypes(ChartOfCharacteristicTypes bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.predefined = this.<PredefinedEntity>fillParent(this.createChartOfCharacteristicTypesPredefinedItems((List)Optional.ofNullable(bmObject.getPredefined()).map((i) -> i.getItems()).orElse((Object)null)), bmObject, "predefined");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createChartOfCalculationTypes(ChartOfCalculationTypes bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.predefined = this.<PredefinedEntity>fillParent(this.createChartOfCalculationTypesPredefinedItems((List)Optional.ofNullable(bmObject.getPredefined()).map((i) -> i.getItems()).orElse((Object)null)), bmObject, "predefined");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createChartOfAccounts(ChartOfAccounts bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.predefined = this.<PredefinedEntity>fillParent(this.createChartOfAccountsPredefinedItems((List)Optional.ofNullable(bmObject.getPredefined()).map((i) -> i.getItems()).orElse((Object)null)), bmObject, "predefined");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createCatalog(Catalog bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.predefined = this.<PredefinedEntity>fillParent(this.createCatalogPredefinedItems((List)Optional.ofNullable(bmObject.getPredefined()).map((i) -> i.getItems()).orElse((Object)null)), bmObject, "predefined");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createCalculationRegister(CalculationRegister bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.registerResources = this.<RegisterResourceEntity>fillParent(this.createRegisterResources(bmObject.getResources()), bmObject, "register_resources");
      meta.registerDimensions = this.<RegisterDimensionEntity>fillParent(this.createRegisterDimensions(bmObject.getDimensions()), bmObject, "register_dimensions");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createBusinessProcess(BusinessProcess bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.tabularSections = this.<TabularSectionEntity>fillParent(this.createTabularSections(bmObject.getTabularSections()), bmObject, "tabular_sections");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.basedOn = this.<MetaEntity>fillParent(this.createBasedOn(bmObject.getBasedOn()), bmObject, "based_on");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createAccumulationRegister(AccumulationRegister bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.registerResources = this.<RegisterResourceEntity>fillParent(this.createRegisterResources(bmObject.getResources()), bmObject, "register_resources");
      meta.registerDimensions = this.<RegisterDimensionEntity>fillParent(this.createRegisterDimensions(bmObject.getDimensions()), bmObject, "register_dimensions");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private MetaEntity createAccountingRegister(AccountingRegister bmObject) {
      MetaEntity meta = new MetaEntity();
      meta.attributes = this.<AttributeEntity>fillParent(this.createAttributes(bmObject.getAttributes()), bmObject, "attributes");
      meta.standardAttributes = this.<AttributeEntity>fillParent(this.createStandardAttributes(bmObject.getStandardAttributes()), bmObject, "standard_attributes");
      meta.registerResources = this.<RegisterResourceEntity>fillParent(this.createRegisterResources(bmObject.getResources()), bmObject, "register_resources");
      meta.registerDimensions = this.<RegisterDimensionEntity>fillParent(this.createRegisterDimensions(bmObject.getDimensions()), bmObject, "register_dimensions");
      meta.objectForms = this.<ObjectFormEntity>fillParent(this.createForms(bmObject.getForms()), bmObject, "object_form");
      meta.templates = this.<TemplateEntity>fillParent(this.createTemplates(bmObject.getTemplates()), bmObject, "templates");
      return meta;
   }

   private List<MetaEntity> createBasedOn(List<MdObject> basedOn) {
      return basedOn != null && !basedOn.isEmpty() ? (List)basedOn.stream().filter((i) -> i instanceof IBmObject).map((i) -> (IBmObject)i).map(this::createBasedOn).collect(Collectors.toList()) : null;
   }

   private <T extends BasicFeature> List<AttributeEntity> createAttributes(List<T> attributes) {
      return attributes != null && !attributes.isEmpty() ? (List)attributes.stream().map(this::createAttribute).collect(Collectors.toList()) : null;
   }

   private <T extends StandardAttribute> List<AttributeEntity> createStandardAttributes(List<T> attributes) {
      return attributes != null && !attributes.isEmpty() ? (List)attributes.stream().map(this::createStandardAttribute).collect(Collectors.toList()) : null;
   }

   private <T extends BasicForm> List<ObjectFormEntity> createForms(List<T> forms) {
      return forms != null && !forms.isEmpty() ? (List)forms.stream().map(this::createForm).collect(Collectors.toList()) : null;
   }

   private <T extends RegisterResource> List<RegisterResourceEntity> createRegisterResources(List<T> resources) {
      return resources != null && !resources.isEmpty() ? (List)resources.stream().map(this::createRegisterResource).collect(Collectors.toList()) : null;
   }

   private <T extends RegisterDimension> List<RegisterDimensionEntity> createRegisterDimensions(List<T> dimensions) {
      return dimensions != null && !dimensions.isEmpty() ? (List)dimensions.stream().map(this::createRegisterDimension).collect(Collectors.toList()) : null;
   }

   private <T extends DbObjectTabularSection> List<TabularSectionEntity> createTabularSections(List<T> tabularSections) {
      return tabularSections != null && !tabularSections.isEmpty() ? (List)tabularSections.stream().map(this::createTabularSection).collect(Collectors.toList()) : null;
   }

   private <T extends BasicRegister> List<RegisterRecordEntity> createRegisterRecords(List<T> registerRecords) {
      return registerRecords != null && !registerRecords.isEmpty() ? (List)registerRecords.stream().map(this::createBasicRegister).collect(Collectors.toList()) : null;
   }

   private List<PredefinedEntity> createCatalogPredefinedItems(List<CatalogPredefinedItem> predefinedItems) {
      return predefinedItems != null && !predefinedItems.isEmpty() ? (List)predefinedItems.stream().map(this::createCatalogPredefined).collect(Collectors.toList()) : null;
   }

   private List<PredefinedEntity> createChartOfCharacteristicTypesPredefinedItems(List<ChartOfCharacteristicTypesPredefinedItem> predefinedItems) {
      return predefinedItems != null && !predefinedItems.isEmpty() ? (List)predefinedItems.stream().map(this::createChartOfCharacteristicTypesPredefined).collect(Collectors.toList()) : null;
   }

   private List<PredefinedEntity> createChartOfCalculationTypesPredefinedItems(List<ChartOfCalculationTypesPredefinedItem> predefinedItems) {
      return predefinedItems != null && !predefinedItems.isEmpty() ? (List)predefinedItems.stream().map(this::createChartOfCalculationTypesPredefined).collect(Collectors.toList()) : null;
   }

   private List<PredefinedEntity> createChartOfAccountsPredefinedItems(List<ChartOfAccountsPredefinedItem> predefinedItems) {
      return predefinedItems != null && !predefinedItems.isEmpty() ? (List)predefinedItems.stream().map(this::createChartOfAccountsPredefined).collect(Collectors.toList()) : null;
   }

   private <T extends Template> List<TemplateEntity> createTemplates(List<T> templates) {
      return templates != null && !templates.isEmpty() ? (List)templates.stream().map(this::createTemplate).collect(Collectors.toList()) : null;
   }

   private <T extends Field> List<FieldEntity> createFields(FieldSource fieldSource, Predicate<Field> filter) {
      if (fieldSource == null) {
         return null;
      } else {
         ArrayList<FieldEntity> result = new ArrayList();
         Stack<FieldSource> sources = new Stack();
         sources.push(fieldSource);

         FieldSource source;
         for(; !sources.isEmpty(); sources.addAll(source.getRefFieldSources())) {
            source = (FieldSource)sources.pop();
            EList<Field> fields = source.getFields();
            if (fields != null) {
               for(Field field : fields) {
                  if (filter.test(field)) {
                     result.add(this.createField(field));
                  }
               }
            }
         }

         if (result.isEmpty()) {
            return null;
         } else {
            return result;
         }
      }
   }

   private MetaEntity createBasedOn(IBmObject basedOn) {
      return this.createAndFillMetaEntity(basedOn, true);
   }

   private AttributeEntity createAttribute(BasicFeature attribute) {
      AttributeEntity entity = new AttributeEntity();
      entity.id = this.getId(attribute);
      entity.name = attribute.getName();
      entity.comment = attribute.getComment();
      entity.toolTip = this.createMap(attribute.getToolTip());
      entity.synonym = this.createMap(attribute.getSynonym());
      entity.types = this.createTypes(attribute.getTypeDescription());
      entity.minValue = this.createValue((EObject)attribute.getMinValue());
      entity.maxValue = this.createValue((EObject)attribute.getMaxValue());
      return entity;
   }

   private AttributeEntity createStandardAttribute(StandardAttribute attribute) {
      AttributeEntity entity = new AttributeEntity();
      entity.id = this.getId(attribute);
      entity.name = attribute.getName();
      entity.toolTip = this.createMap(attribute.getToolTip());
      entity.synonym = this.createMap(attribute.getSynonym());
      return entity;
   }

   private ObjectFormEntity createForm(BasicForm form) {
      ObjectFormEntity entity = new ObjectFormEntity();
      entity.id = this.getId(form);
      entity.name = form.getName();
      entity.synonym = this.createMap(form.getSynonym());
      return entity;
   }

   private TabularSectionEntity createTabularSection(DbObjectTabularSection tabularSection) {
      TabularSectionEntity entity = new TabularSectionEntity();
      entity.id = this.getId(tabularSection);
      entity.name = tabularSection.getName();
      entity.comment = tabularSection.getComment();
      entity.toolTip = this.createMap(tabularSection.getToolTip());
      entity.attributes = this.<AttributeEntity>fillParent(this.createAttributes(tabularSection.getAttributes()), tabularSection, "attributes");
      entity.fields = this.<FieldEntity>fillParent(this.createFields(tabularSection, (field) -> this.isPublishedField(field)), tabularSection, "fields");
      return entity;
   }

   private RegisterResourceEntity createRegisterResource(RegisterResource registerResource) {
      RegisterResourceEntity entity = new RegisterResourceEntity();
      entity.id = this.getId(registerResource);
      entity.name = registerResource.getName();
      entity.comment = registerResource.getComment();
      entity.toolTip = this.createMap(registerResource.getToolTip());
      entity.synonym = this.createMap(registerResource.getSynonym());
      entity.types = this.createTypes(registerResource.getType());
      return entity;
   }

   private RegisterDimensionEntity createRegisterDimension(RegisterDimension registerDimension) {
      RegisterDimensionEntity entity = new RegisterDimensionEntity();
      entity.id = this.getId(registerDimension);
      entity.name = registerDimension.getName();
      entity.comment = registerDimension.getComment();
      entity.toolTip = this.createMap(registerDimension.getToolTip());
      entity.synonym = this.createMap(registerDimension.getSynonym());
      entity.types = this.createTypes(registerDimension.getType());
      return entity;
   }

   private RegisterRecordEntity createBasicRegister(BasicRegister registerRecord) {
      RegisterRecordEntity entity = new RegisterRecordEntity();
      entity.id = this.getId(registerRecord);
      entity.name = registerRecord.getName();
      entity.comment = registerRecord.getComment();
      entity.synonym = this.createMap(registerRecord.getSynonym());
      return entity;
   }

   private EnumValueEntity createEnumValue(EnumValue enumValue) {
      EnumValueEntity entity = new EnumValueEntity();
      entity.id = this.getId(enumValue);
      entity.name = enumValue.getName();
      entity.synonym = this.createMap(enumValue.getSynonym());
      return entity;
   }

   private PredefinedEntity createCatalogPredefined(CatalogPredefinedItem predefined) {
      PredefinedEntity entity = new PredefinedEntity();
      entity.id = this.getId(predefined);
      entity.name = predefined.getName();
      entity.description = predefined.getDescription();
      entity.value = this.createValue((EObject)predefined.getCode());
      entity.predefined = this.createCatalogPredefinedItems(predefined.getContent());
      return entity;
   }

   private PredefinedEntity createChartOfCharacteristicTypesPredefined(ChartOfCharacteristicTypesPredefinedItem predefined) {
      PredefinedEntity entity = new PredefinedEntity();
      entity.id = this.getId(predefined);
      entity.name = predefined.getName();
      entity.description = predefined.getDescription();
      entity.value = this.createValue(predefined.getCode());
      entity.predefined = this.createChartOfCharacteristicTypesPredefinedItems(predefined.getContent());
      entity.types = this.createTypes(predefined.getType());
      return entity;
   }

   private PredefinedEntity createChartOfCalculationTypesPredefined(ChartOfCalculationTypesPredefinedItem predefined) {
      PredefinedEntity entity = new PredefinedEntity();
      entity.id = this.getId(predefined);
      entity.name = predefined.getName();
      entity.description = predefined.getDescription();
      entity.value = this.createValue((EObject)predefined.getCode());
      entity.displaced = this.createChartOfCalculationTypesPredefinedItems(predefined.getDisplaced());
      return entity;
   }

   private PredefinedEntity createChartOfAccountsPredefined(ChartOfAccountsPredefinedItem predefined) {
      PredefinedEntity entity = new PredefinedEntity();
      entity.id = this.getId(predefined);
      entity.name = predefined.getName();
      entity.description = predefined.getDescription();
      entity.value = this.createValue(predefined.getCode());
      entity.child = this.createChartOfAccountsPredefinedItems(predefined.getChildItems());
      return entity;
   }

   private TemplateEntity createTemplate(Template template) {
      TemplateEntity entity = new TemplateEntity();
      entity.id = this.getId(template);
      entity.name = template.getName();
      entity.comment = template.getComment();
      entity.synonym = this.createMap(template.getSynonym());
      return entity;
   }

   private ValueEntity createValue(EObject valueObject) {
      ValueEntity entity = new ValueEntity();
      entity.id = this.getId(valueObject);
      if (valueObject == null) {
         entity.type = ValueType.NULL;
         return entity;
      } else if (valueObject instanceof UndefinedValue) {
         entity.type = ValueType.UNDEFINED;
         return entity;
      } else if (valueObject instanceof NullValue) {
         entity.type = ValueType.NULL;
         return entity;
      } else if (valueObject instanceof BooleanValue) {
         entity.type = ValueType.BOOLEAN;
         entity.value = ((BooleanValue)valueObject).isValue();
         return entity;
      } else if (valueObject instanceof NumberValue) {
         entity.type = ValueType.DECIMAL;
         entity.value = ((NumberValue)valueObject).getValue();
         return entity;
      } else if (valueObject instanceof StringValue) {
         entity.type = ValueType.STRING;
         entity.value = ((StringValue)valueObject).getValue();
         return entity;
      } else if (valueObject instanceof DateValue) {
         entity.type = ValueType.DATETIME;
         entity.value = ((DateValue)valueObject).getValue();
         return entity;
      } else if (valueObject instanceof BinaryValue) {
         entity.type = ValueType.BINARY;
         entity.value = ((BinaryValue)valueObject).getValue();
         return entity;
      } else {
         if (valueObject instanceof ReferenceValue) {
            entity.type = ValueType.REFERENCE;
            ReferenceValue refValueObject = (ReferenceValue)valueObject;
            entity.value = this.createValue(refValueObject.getValue());
         }

         if (valueObject instanceof IrresolvableReferenceValue) {
            entity.type = ValueType.IRRESORVABLE_REFERENCE;
            IrresolvableReferenceValue referenceValue = (IrresolvableReferenceValue)valueObject;
            entity.value = String.format("%s.%s", referenceValue.getRefTypeId().toString(), referenceValue.getInstanceId().toString());
            return entity;
         } else if (valueObject instanceof ValueList) {
            entity.type = ValueType.LIST;
            entity.value = ((ValueList)valueObject).getValues().stream().map(this::createValue).collect(Collectors.toList());
            return entity;
         } else if (valueObject instanceof FixedArrayValue) {
            entity.type = ValueType.ARRAY;
            entity.value = ((FixedArrayValue)valueObject).getValues().stream().map(this::createValue).collect(Collectors.toList());
            return entity;
         } else if (valueObject instanceof TypeDescriptionValue) {
            entity.type = ValueType.TYPE;
            TypeDescription value = ((TypeDescriptionValue)valueObject).getValue();
            entity.value = this.createTypes(value);
            return entity;
         } else if (valueObject instanceof StandardPeriodValue) {
            entity.type = ValueType.STANDARD_PERIOD;
            entity.value = this.createStandardPeriod(((StandardPeriodValue)valueObject).getValue());
            return entity;
         } else if (valueObject instanceof FormChoiceListDesTimeValue) {
            entity.type = ValueType.FORM_CHOICE_LIST_DES_TIME;
            return entity;
         } else if (valueObject instanceof BorderValue) {
            entity.type = ValueType.BORDER;
            entity.value = this.createBorder(((BorderValue)valueObject).getValue());
            return entity;
         } else if (valueObject instanceof ColorValue) {
            entity.type = ValueType.COLOR;
            entity.value = this.createColor(((ColorValue)valueObject).getValue());
            return entity;
         } else if (valueObject instanceof FontValue) {
            entity.type = ValueType.FONT;
            entity.value = this.createFont(((FontValue)valueObject).getValue());
            return entity;
         } else if (valueObject instanceof AccountTypeValue) {
            entity.type = ValueType.ACCOUNT_TYPE;
            entity.value = this.createAccountType(((AccountTypeValue)valueObject).getValue());
            return entity;
         } else if (valueObject instanceof ChartLineTypeValue) {
            entity.type = ValueType.CHART_LINE_TYPE;
            entity.value = this.createChartLineType(((ChartLineTypeValue)valueObject).getValue());
            return entity;
         } else if (valueObject instanceof EnumValue) {
            entity.type = ValueType.ENUM;
            entity.value = this.createEnumValue((EnumValue)valueObject);
            return entity;
         } else {
            if (valueObject instanceof SysEnumValue) {
               entity.type = ValueType.SYS_ENUM;
               SysEnumValue value = (SysEnumValue)valueObject;
               if (value.getValue() != null && value.getValue().indexOf(46) != -1) {
                  String[] segments = value.getValue().split("\\.");
                  entity.value = new String[]{segments[0], segments[1]};
               }
            }

            entity.type = ValueType.UNKNOWN;
            return entity;
         }
      }
   }

   private ValueEntity createValue(String code) {
      ValueEntity entity = new ValueEntity();
      entity.value = code;
      entity.type = ValueType.STRING;
      return entity;
   }

   private ChartLineTypeEntity createChartLineType(ChartLineType value) {
      ChartLineTypeEntity entity = new ChartLineTypeEntity();
      entity.name = value.getName();
      entity.literal = value.getLiteral();
      entity.value = value.getValue();
      return null;
   }

   private AccountTypeEntity createAccountType(AccountType value) {
      AccountTypeEntity entity = new AccountTypeEntity();
      entity.name = value.getName();
      entity.literal = value.getLiteral();
      entity.value = value.getValue();
      return entity;
   }

   private IContextEntity createFont(Font value) {
      FontEntity entity = new FontEntity();
      entity.bold = value.bold();
      entity.italic = value.italic();
      entity.underline = value.underline();
      entity.strikeout = value.strikeout();
      entity.faceName = value.faceName();
      entity.scale = value.scale();
      entity.height = value.height();
      return entity;
   }

   private IContextEntity createColor(Color value) {
      ColorEntity entity = new ColorEntity();
      entity.red = value.red();
      entity.green = value.green();
      entity.blue = value.blue();
      return entity;
   }

   private IContextEntity createBorder(Border value) {
      BorderEntity entity = new BorderEntity();
      entity.style = value.style().getName();
      entity.width = value.width();
      return entity;
   }

   private IContextEntity createStandardPeriod(StandardPeriod value) {
      StandardPeriodEntity entity = new StandardPeriodEntity();
      entity.startDate = value.getStartDate();
      entity.endDate = value.getEndDate();
      return entity;
   }

   private boolean isPublishedField(Field field) {
      String name = field.getName();
      return name != null && !name.equals("Ref") && !name.equals("LineNumber");
   }

   private Map<String, String> createMap(EMap<String, String> map) {
      return map != null && !map.isEmpty() ? map.map() : null;
   }

   private List<DataType> createTypes(TypeDescription typeDescription) {
      if (typeDescription == null) {
         return null;
      } else {
         EList<TypeItem> types = typeDescription.getTypes();
         return types != null && !types.isEmpty() ? (List)types.stream().map(this::createType).collect(Collectors.toList()) : null;
      }
   }

   private List<ColumnEntity> createColumns(EList<Column> columns) {
      return columns != null && !columns.isEmpty() ? (List)columns.stream().map(this::createColumn).collect(Collectors.toList()) : null;
   }

   private DataType createType(TypeItem type) {
      DataType dataType = new DataType();
      dataType.type = type.getName();
      dataType.typeRu = type.getNameRu();
      return dataType;
   }

   private ColumnEntity createColumn(Column column) {
      ColumnEntity entity = new ColumnEntity();
      entity.id = this.getId(column);
      entity.name = column.getName();
      entity.comment = column.getComment();
      entity.synonym = this.createMap(column.getSynonym());
      return entity;
   }

   private FieldEntity createField(Field field) {
      FieldEntity entity = new FieldEntity();
      entity.id = this.getId(field);
      entity.name = field.getName();
      entity.nameRu = field.getNameRu();
      entity.types = this.createTypes(field.getType());
      return entity;
   }

   private void fillMethod(MethodEntity methodEntity, Method method, boolean detailed, ICompositeNode methodNode, ICompositeNode node, ICancellationToken cancellationToken) {
      methodEntity.name = method.getName();
      if (detailed) {
         methodEntity.start = methodNode.getTotalOffset();
         methodEntity.finish = methodNode.getTotalEndOffset();
         String code = methodNode.getText();
         int length = code.length();
         code = code.stripLeading();
         methodEntity.start = methodEntity.start + (length - code.length());
         code = code.stripTrailing();
         methodEntity.finish = methodEntity.finish - (length - code.length());
         methodEntity.code = code;
      }

      if (method instanceof Function) {
         methodEntity.kind = BslUtil.isRussian(method, this.v8ProjectManager) ? "Функция" : "Function";
      } else if (method instanceof Procedure) {
         methodEntity.kind = BslUtil.isRussian(method, this.v8ProjectManager) ? "Процедура" : "Procedure";
      }

      List<String> signatureParts = (List)this.codePartsProvider.getParts(methodNode).filter((i) -> !cancellationToken.isCanceled()).filter((i) -> i.getLocation() == CursorLocation.FunctionName || i.getLocation() == CursorLocation.FunctionArguments).map((i) -> i.getText()).collect(Collectors.toList());
      StringBuilder signature = new StringBuilder();

      for(String signaturePart : signatureParts) {
         signature.append(signaturePart);
      }

      String signatureStr = signature.toString().trim();
      if (!signatureStr.isBlank()) {
         methodEntity.signatureStr = signatureStr;
      }

      if (methodEntity.path != null) {
         methodEntity.uuid = this.idFactory.createNodeId(methodEntity.path, methodNode);
      }

      if (method.isAsync()) {
         methodEntity.signatureStructurized.attributes.add(BslUtil.isRussian(method, this.v8ProjectManager) ? "Аcинх" : "Async");
      }

      if (method.isExport()) {
         methodEntity.signatureStructurized.attributes.add(BslUtil.isRussian(method, this.v8ProjectManager) ? "Экспорт" : "Export");
      }

      for(FormalParam param : method.getFormalParams()) {
         Parameter parameter = new Parameter();
         methodEntity.signatureStructurized.parameters.add(parameter);
         parameter.name = param.getName();
         parameter.required = param.getDefaultValue() == null;
         parameter.types = this.createDataTypes(this.v8Model.getTypes(param.getTypeStateProvider(), node));
      }

      for(Pragma pragma : method.getPragmas()) {
         methodEntity.signatureStructurized.preprocess.add(pragma.getSymbol());
      }

      this.getEnvironments(method, cancellationToken).ifPresent((areas) -> methodEntity.environments = areas);
      this.getAreas(method, cancellationToken).ifPresent((areas) -> methodEntity.areas = areas);
      methodEntity.comment = this.v8Model.getComment(method);
      methodEntity.structurizedComment = this.commentFactory.create(this.v8Model.getComment(method, true));
   }

   public Optional<List<String>> getEnvironments(EObject obj, ICancellationToken cancellationToken) {
      Environment[] environments = this.v8Model.getEnvironments(obj).toArray();
      if (environments.length == 0) {
         return Optional.empty();
      } else {
         ArrayList<String> result = new ArrayList();

         for(Environment environment : environments) {
            if (cancellationToken.isCanceled()) {
               return Optional.empty();
            }

            result.add(environment.name());
         }

         return Optional.of(result);
      }
   }

   public Optional<List<String>> getAreas(EObject obj, ICancellationToken cancellationToken) {
      EObject object = obj;
      ArrayList<String> areas = new ArrayList();

      do {
         RegionPreprocessor region = (RegionPreprocessor)EcoreUtil2.getContainerOfType(object, RegionPreprocessor.class);
         if (region == null) {
            break;
         }

         TreeIterator<EObject> it = region.eAllContents();
         boolean started = false;

         while(it.hasNext()) {
            EObject item = (EObject)it.next();
            if (!started) {
               if (region.getItem() == item) {
                  started = true;
               }
            } else if (region.getItemAfter() == item) {
               break;
            }

            if (started && item == obj) {
               areas.add(0, region.getName());
               break;
            }
         }

         object = region.eContainer();
      } while(object != null && !cancellationToken.isCanceled());

      return !cancellationToken.isCanceled() && areas.size() != 0 ? Optional.of(areas) : Optional.empty();
   }

   private void fillType(EObject eObject, ObjectEntity objectEntity, List<Type> types, ICancellationToken cancellationToken) {
      ArrayList<ObjectEntityField> fields = new ArrayList();
      objectEntity.fields = fields;
      objectEntity.types = this.createDataTypes(types);

      for(Type type : types) {
         ContextDef contexDef = type.getContextDef();
         if (contexDef != null) {
            for(Property prop : contexDef.getProperties()) {
               ObjectEntityField field = this.createField(prop, cancellationToken);
               objectEntity.fields.add(field);
            }
         }
      }

      Resource resouce = eObject.eResource();
      if (resouce != null) {
         List<TypeItem> typeItems = this.v8Model.getTypes(eObject);

         for(Pair<Collection<Property>, TypeItem> pair : this.v8Model.getProperties(typeItems, resouce)) {
            for(Property dynamicProp : (Collection)pair.getFirst()) {
               ObjectEntityField field = this.createField(dynamicProp, cancellationToken);
               objectEntity.fields.add(field);
            }
         }
      }

   }

   private List<DataType> createDataTypes(List<Type> types) {
      if (types != null && !types.isEmpty()) {
         ArrayList<DataType> dataTypes = new ArrayList();

         for(Type type : types) {
            DataType dataType = new DataType();
            dataTypes.add(dataType);
            dataType.type = type.getName();
            dataType.typeRu = type.getNameRu();
         }

         return distinct(dataTypes);
      } else {
         return null;
      }
   }

   private List<DataType> createDataTypesFromTypeItemsSafety(List<TypeItem> types, boolean distinct) {
      if (types != null && !types.isEmpty()) {
         ArrayList<DataType> dataTypes = new ArrayList();

         for(TypeItem type : types) {
            try {
               ;
            } catch (Exception var7) {
               continue;
            }

            DataType dataType = new DataType();
            dataTypes.add(dataType);
            dataType.type = type.getName();
            dataType.typeRu = type.getNameRu();
         }

         return (List<DataType>)(!distinct ? dataTypes : distinct(dataTypes));
      } else {
         return null;
      }
   }

   private static <T> List<T> distinct(List<T> source) {
      return source != null && source.size() != 0 ? (List)source.stream().distinct().collect(Collectors.toList()) : source;
   }

   private ObjectEntityField createField(Property prop, ICancellationToken cancellationToken) {
      ObjectEntityField field = new ObjectEntityField();
      field.name = prop.getName();
      EList<TypeItem> types = prop.getTypes();
      field.types = this.createDataTypesFromTypeItemsSafety(types, false);
      if (types != null && !types.isEmpty()) {
         for(int i = 0; i < types.size(); ++i) {
            TypeItem propType = (TypeItem)types.get(i);
            DataType propDataType = (DataType)field.types.get(i);
            FeatureAccess featureAccess = (FeatureAccess)EcoreUtil2.getContainerOfType(propType, FeatureAccess.class);
            if (featureAccess != null) {
               this.v8Model.getPath(featureAccess).ifPresent((path) -> {
                  this.moduleProvider.getModule((IDocument)null, path, cancellationToken);
                  ICompositeNode fieldNode = NodeModelUtils.getNode(featureAccess);
                  propDataType.uuid = this.idFactory.createNodeId(path, fieldNode);
               });
            }

            List<String> comment = this.v8Model.getComment(featureAccess);
            if (comment != null && !comment.isEmpty()) {
               propDataType.comment = comment;
            }
         }
      }

      field.types = distinct(field.types);
      return field;
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$_1c$g5$v8$dt$form$model$PropertyInfo$PropertyInfoType() {
      int[] var10000 = $SWITCH_TABLE$com$_1c$g5$v8$dt$form$model$PropertyInfo$PropertyInfoType;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[PropertyInfoType.values().length];

         try {
            var0[PropertyInfoType.COLUMN_DUAL_TYPE_PROPERTY.ordinal()] = 8;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[PropertyInfoType.COLUMN_NON_VISUAL_PROPERTY.ordinal()] = 6;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[PropertyInfoType.COLUMN_PROPERTY.ordinal()] = 5;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[PropertyInfoType.COLUMN_TABLE_TYPE_PROPERTY.ordinal()] = 7;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[PropertyInfoType.COMMON_DUAL_TYPE_PROPERTY.ordinal()] = 4;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[PropertyInfoType.COMMON_NON_VISUAL_PROPERTY.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[PropertyInfoType.COMMON_PROPERTY.ordinal()] = 1;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[PropertyInfoType.COMMON_TABLE_TYPE_PROPERTY.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$_1c$g5$v8$dt$form$model$PropertyInfo$PropertyInfoType = var0;
         return var0;
      }
   }
}
