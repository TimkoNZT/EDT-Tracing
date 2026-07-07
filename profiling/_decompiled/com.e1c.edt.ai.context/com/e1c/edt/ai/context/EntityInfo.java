package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.DataType;
import com.e1c.edt.ai.FillAction;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IHashTools;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProgramingLanguage;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.StatisticsType;
import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.HashedValue;
import com.e1c.edt.ai.assistent.model.LocalContext;
import com.e1c.edt.ai.context.DTO.EntityInfoRequest;
import com.e1c.edt.ai.context.DTO.EntityInfoResponse;
import com.e1c.edt.ai.context.DTO.MethodEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntity;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.nodemodel.ICompositeNode;

class EntityInfo implements IEntityInfo, IContextEntities {
   private static final String MethodNamePrefix = "#/_method/";
   private static final HashSet<CursorLocation> methodHashingParts = new HashSet();
   private final ILog log;
   private final IEntitiesWalker entitiesWalker;
   private final IIdFactory idFactory;
   private final IEntityFactory entityFactory;
   private final ISettings settings;
   private final IDispatcher dispatcher;
   private final IV8ProjectManager v8ProjectManager;
   private final IProgramingLanguage programingLanguage;
   private final Provider<MessageDigest> messageDigestProvider;
   private final IHashTools hashTools;
   private final ICodePartsProvider codePartsProvider;
   private final IModuleProvider activeEditorResourceSetProvider;
   private final IModuleProvider baseResourceSetProvider;

   static {
      methodHashingParts.add(CursorLocation.Comment);
      methodHashingParts.add(CursorLocation.FunctionName);
      methodHashingParts.add(CursorLocation.FunctionArguments);
   }

   @Inject
   public EntityInfo(ILog log, IEntitiesWalker entitiesWalker, IIdFactory idFactory, IEntityFactory entityFactory, ISettings settings, IDispatcher dispatcher, IV8ProjectManager v8ProjectManager, IProgramingLanguage programingLanguage, Provider<MessageDigest> messageDigestProvider, IHashTools hashTools, ICodePartsProvider codePartsProvider, IModuleProvider activeEditorResourceSetProvider, @Named("BaseModuleProvider") IModuleProvider baseResourceSetProvider) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(entitiesWalker);
      Preconditions.checkNotNull(idFactory);
      Preconditions.checkNotNull(entityFactory);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(v8ProjectManager);
      Preconditions.checkNotNull(programingLanguage);
      Preconditions.checkNotNull(messageDigestProvider);
      Preconditions.checkNotNull(hashTools);
      Preconditions.checkNotNull(codePartsProvider);
      Preconditions.checkNotNull(activeEditorResourceSetProvider);
      Preconditions.checkNotNull(baseResourceSetProvider);
      this.log = log;
      this.entitiesWalker = entitiesWalker;
      this.idFactory = idFactory;
      this.entityFactory = entityFactory;
      this.settings = settings;
      this.dispatcher = dispatcher;
      this.v8ProjectManager = v8ProjectManager;
      this.programingLanguage = programingLanguage;
      this.messageDigestProvider = messageDigestProvider;
      this.hashTools = hashTools;
      this.codePartsProvider = codePartsProvider;
      this.activeEditorResourceSetProvider = activeEditorResourceSetProvider;
      this.baseResourceSetProvider = baseResourceSetProvider;
   }

   public Optional<EntityInfoResponse> getInfo(final EntityInfoRequest request, final ICancellationToken cancellationToken) {
      Preconditions.checkNotNull(request);
      if (request.ref != null && !request.ref.isBlank()) {
         Optional<SourceSpan> nodeIdOptional = this.idFactory.getNodeId(request.ref);
         if (nodeIdOptional.isEmpty()) {
            return Optional.empty();
         } else {
            SourceSpan nodeId = (SourceSpan)nodeIdOptional.get();
            final EntityInfoResponse response = new EntityInfoResponse();
            response.ref = request.ref;
            boolean result = this.entitiesWalker.walk((IDocument)null, nodeId.getPath(), nodeId.getStart(), nodeId.getFinish(), this.activeEditorResourceSetProvider, new EntityVisitor() {
               public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node) {
                  if (request.ref != null && request.ref.equals(nodeId)) {
                     Optional<ObjectEntity> objectEntity = EntityInfo.this.entityFactory.crateObjectEntity(variable, node, true, cancellationToken);
                     response.object = (ObjectEntity)objectEntity.orElse((Object)null);
                     return objectEntity.isPresent();
                  } else {
                     return false;
                  }
               }

               public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess, ICompositeNode node) {
                  if (request.ref != null && request.ref.equals(nodeId)) {
                     Optional<ObjectEntity> objectEntity = EntityInfo.this.entityFactory.crateObjectEntity(featureAccess, node, true, cancellationToken);
                     response.object = (ObjectEntity)objectEntity.orElse((Object)null);
                     return objectEntity.isPresent();
                  } else {
                     return false;
                  }
               }

               public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation, ICompositeNode node) {
                  if (request.ref != null && request.ref.equals(nodeId)) {
                     Optional<MethodEntity> methodEntity = EntityInfo.this.entityFactory.createMethodEntity(invocation, node, true, cancellationToken);
                     response.method = (MethodEntity)methodEntity.orElse((Object)null);
                     return methodEntity.isPresent();
                  } else {
                     return false;
                  }
               }
            }, IStatistics.Empty, cancellationToken);
            if (!result) {
               this.log.warning("Entity not found", () -> request.ref);
               return Optional.empty();
            } else {
               return Optional.of(response);
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public Duration fill(AIContext aiContext, LocalContext localContext, GlobalContext globalContext, Predicate<FillAction> actionFilter, IStatistics statistics, ICancellationToken cancellationToken) {
      try {
         this.log.trace("sync", "EntityInfo fill", () -> aiContext.toString());
         IModuleProvider curResourceSetProvider = aiContext.getDocument() != null ? this.activeEditorResourceSetProvider : this.baseResourceSetProvider;
         return this.fillInternal(aiContext, localContext, globalContext, curResourceSetProvider, statistics, actionFilter, cancellationToken);
      } catch (Exception error) {
         this.log.logError(error);
         return Duration.ofMillis(0L);
      }
   }

   private Duration fillInternal(AIContext aiContext, final LocalContext localContext, final GlobalContext globalContext, IModuleProvider resourceSetProvider, final IStatistics statistics, final Predicate<FillAction> actionFilter, final ICancellationToken cancellationToken) {
      final CharBuffer buffer = CharBuffer.allocate(1024);
      Stopwatch stopwatch = Stopwatch.createStarted();
      final String filePath = aiContext.getPath();
      int start = aiContext.getStart();
      final int offset = aiContext.getTextOffset();
      int finish = aiContext.getFinish();
      final int sourceOffset = aiContext.getSourceOffset();
      localContext.relatedObjects = new ArrayList();
      localContext.relatedFunctions = new ArrayList();
      globalContext.localFunctions = new HashMap();
      globalContext.localFunctionsEntities = new HashMap();
      final HashSet<String> uuids = new HashSet();
      final EObject[] cursorObjects = new EObject[1];
      final ArrayList<BmObject> bmObjects = new ArrayList();
      final IDocument document = aiContext.getDocument();
      this.programingLanguage.getFromPath(filePath).ifPresent((lang) -> localContext.programingLanguage = lang);
      this.entitiesWalker.walk(document, filePath, start, finish, resourceSetProvider, new EntityVisitor() {
         public boolean visitModule(BmRoot root, Module module) {
            if (!actionFilter.test(new FillAction(DataType.HASH, "local_functions", (String)null))) {
               return false;
            } else {
               IFile file = (IFile)root.getFile(module).orElse((Object)null);
               globalContext.moduleHash = (String)EntityInfo.this.hashTools.hashOf(document, file).map((hash) -> EntityInfo.this.hashTools.format(hash, true)).orElse((Object)null);
               return false;
            }
         }

         public boolean visitNode(BmRoot root, EObject eObject, ICompositeNode node) {
            int nodeStart = node.getTotalOffset();
            int nodeFinish = node.getTotalEndOffset();
            if (nodeStart <= offset && offset <= nodeFinish) {
               cursorObjects[0] = eObject;
            }

            return false;
         }

         public boolean visitBmObject(BmRoot root, IBmObject owner) {
            bmObjects.add(new BmObject(root, owner));
            return false;
         }

         public boolean visitForm(BmRoot root, Form form) {
            try {
               Throwable var3 = null;
               Object var4 = null;

               try {
                  AutoCloseable measurement = statistics.measureDuration(StatisticsType.FORM_DURATUION);

                  try {
                     if (actionFilter.test(new FillAction(DataType.HASH, "form", (String)null))) {
                        root.getFile(form).map((file) -> {
                           globalContext.formPath = file.getFullPath().makeRelative().toPortableString();

                           try {
                              return EntityInfo.this.hashTools.compute(file, buffer);
                           } catch (Exception error) {
                              EntityInfo.this.log.logError(error);
                              return null;
                           }
                        }).ifPresent((hash) -> globalContext.formHash = EntityInfo.this.hashTools.format(hash, true));
                        if (actionFilter.test(new FillAction(DataType.DATA, "form", globalContext.formHash))) {
                           EntityInfo.this.entityFactory.createFormEntity(form, cancellationToken).ifPresent((enity) -> globalContext.formEntity = enity);
                        }

                        return false;
                     }
                  } finally {
                     if (measurement != null) {
                        measurement.close();
                     }

                  }

                  return false;
               } catch (Throwable var13) {
                  if (var3 == null) {
                     var3 = var13;
                  } else if (var3 != var13) {
                     var3.addSuppressed(var13);
                  }

                  throw var3;
               }
            } catch (Exception error) {
               EntityInfo.this.log.logError(error);
               return false;
            }
         }

         public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node) {
            if (!actionFilter.test(new FillAction(DataType.DATA, "related_objects", (String)null))) {
               return false;
            } else if (!uuids.add(EntityInfo.this.idFactory.createObjectId(filePath, variable, cancellationToken))) {
               return false;
            } else {
               EntityInfo.this.entityFactory.crateObjectEntity(variable, node, false, cancellationToken).ifPresent((object) -> localContext.relatedObjects.add(object));
               return false;
            }
         }

         public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess, ICompositeNode node) {
            if (!actionFilter.test(new FillAction(DataType.DATA, "related_objects", (String)null))) {
               return false;
            } else {
               EntityInfo.this.entityFactory.crateObjectEntity(featureAccess, node, false, cancellationToken).ifPresent((object) -> localContext.relatedObjects.add(object));
               return false;
            }
         }

         public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation, ICompositeNode node) {
            if (!actionFilter.test(new FillAction(DataType.DATA, "related_functions", (String)null))) {
               return false;
            } else {
               EntityInfo.this.entityFactory.createMethodEntity(invocation, node, false, cancellationToken).ifPresent((method) -> localContext.relatedFunctions.add(method));
               return false;
            }
         }

         public boolean visitMethod(BmRoot root, String nodeId, Method method, ICompositeNode node) {
            if (document == null && !method.isExport()) {
               return false;
            } else {
               String uniqueName = method.getUniqueName();
               int prefixIndex = uniqueName.indexOf("#/_method/");
               if (prefixIndex >= 0) {
                  uniqueName = uniqueName.substring(prefixIndex + "#/_method/".length());
               }

               String field = "local_functions." + uniqueName;
               if (document != null && sourceOffset >= node.getTotalOffset() && sourceOffset <= node.getTotalEndOffset()) {
                  localContext.currenMethodName = uniqueName;
               }

               if (!actionFilter.test(new FillAction(DataType.HASH, "local_functions", (String)null)) && !actionFilter.test(new FillAction(DataType.HASH, field, (String)null))) {
                  return false;
               } else {
                  MessageDigest hash = (MessageDigest)EntityInfo.this.messageDigestProvider.get();
                  EntityInfo.this.codePartsProvider.getParts(node).filter((part) -> EntityInfo.methodHashingParts.contains(part.getLocation())).flatMapToInt((i) -> i.getText().codePoints()).filter((ch) -> !Character.isWhitespace(ch)).forEach((ch) -> hash.update((byte)ch));
                  String hashStr = EntityInfo.this.hashTools.format(hash, true);
                  globalContext.localFunctions.put(uniqueName, hashStr);
                  if (actionFilter.test(new FillAction(DataType.DATA, field, hashStr))) {
                     EntityInfo.this.entityFactory.createMethodEntity(method, node, false, cancellationToken).ifPresent((entity) -> globalContext.localFunctionsEntities.put(uniqueName, new HashedValue(entity, hashStr)));
                  }

                  return false;
               }
            }
         }
      }, statistics, cancellationToken);
      EObject cursorObject = cursorObjects[0];
      if (cursorObject != null) {
         Class<? extends EObject> type = cursorObject.getClass();

         Class[] var24;
         for(Class<?> modelInterface : var24 = type.getInterfaces()) {
            if (modelInterface.getName().startsWith("com._1c.g5.v8.dt.bsl.model.")) {
               localContext.cursorObject = modelInterface.getSimpleName();
               break;
            }
         }

         this.entityFactory.getEnvironments(cursorObject, cancellationToken).ifPresent((areas) -> localContext.cursorEnvironments = areas);
         this.entityFactory.getAreas(cursorObject, cancellationToken).ifPresent((areas) -> localContext.cursorAreas = areas);
      }

      globalContext.metaEntity = null;
      if (!bmObjects.isEmpty()) {
         try {
            Throwable error = null;
            Object var35 = null;

            try {
               AutoCloseable measurement = statistics.measureDuration(StatisticsType.META_DURATUION);

               try {
                  BmObject bmObject = (BmObject)bmObjects.get(bmObjects.size() - 1);
                  if (actionFilter.test(new FillAction(DataType.HASH, "meta", (String)null))) {
                     bmObject.root.getFile(bmObject.owner).map((file) -> {
                        globalContext.metaPath = file.getFullPath().makeRelative().toPortableString();

                        try {
                           return this.hashTools.compute(file, buffer);
                        } catch (Exception error) {
                           this.log.logError(error);
                           return null;
                        }
                     }).ifPresent((hash) -> globalContext.metaHash = this.hashTools.format(hash, true));
                  }

                  if (actionFilter.test(new FillAction(DataType.DATA, "meta", (String)null))) {
                     globalContext.metaEntity = this.entityFactory.createMetaEntity(bmObject.owner, cancellationToken);
                  }
               } finally {
                  if (measurement != null) {
                     measurement.close();
                  }

               }
            } catch (Throwable var32) {
               if (error == null) {
                  error = var32;
               } else if (error != var32) {
                  error.addSuppressed(var32);
               }

               throw error;
            }
         } catch (Exception error) {
            this.log.logError(error);
         }
      }

      return stopwatch.elapsed();
   }

   public void fill(AIContext aiContext, ChatContext context, IStatistics statistics, ICancellationToken cancellationToken) {
      Duration timeout = this.settings.getTimeout();
      this.dispatcher.dispatch(() -> this.fillInternal(aiContext, context, statistics, cancellationToken), timeout);
   }

   private Boolean fillInternal(AIContext aiContext, final ChatContext context, IStatistics statistics, ICancellationToken cancellationToken) {
      String filePath = aiContext.getPath();
      int start = aiContext.getStart();
      int finish = aiContext.getFinish();
      this.programingLanguage.getFromPath(filePath).ifPresent((lang) -> context.programingLanguage = lang);
      this.entitiesWalker.walk(aiContext.getDocument(), filePath, start, finish, this.activeEditorResourceSetProvider, new EntityVisitor() {
         public boolean visitModule(BmRoot root, Module module) {
            IV8Project project = EntityInfo.this.v8ProjectManager.getProject(module);
            if (project != null) {
               context.scriptLanguage = project.getScriptVariant().getName();
            }

            return false;
         }
      }, statistics, cancellationToken);
      return null;
   }

   // $FF: synthetic method
   static IHashTools access$0(EntityInfo var0) {
      return var0.hashTools;
   }

   // $FF: synthetic method
   static ILog access$1(EntityInfo var0) {
      return var0.log;
   }

   private static class BmObject {
      public final BmRoot root;
      public final IBmObject owner;

      public BmObject(BmRoot root, IBmObject owner) {
         this.root = root;
         this.owner = owner;
      }
   }
}
