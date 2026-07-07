package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupport;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.mcore.AbstractMethod;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.context.DTO.Entity;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesRequest;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.nodemodel.ICompositeNode;

public class RelatedEntities implements IRelatedEntities {
   private final ILog log;
   private final IV8Model v8Model;
   private final IEntitiesWalker entitiesWalker;
   private final IIdFactory idFactory;
   private final IEntityFactory entityFactory;
   private final IV8ProjectManager v8ProjectManager;
   private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;
   private final IModuleProvider resourceSetProvider;

   @Inject
   public RelatedEntities(ILog log, IV8Model v8Model, IEntitiesWalker entitiesWalker, IIdFactory idFactory, IEntityFactory entityFactory, IV8ProjectManager v8ProjectManager, IProjectFileSystemSupportProvider projectFileSystemSupportProvider, IModuleProvider resourceSetProvider) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(v8Model);
      Preconditions.checkNotNull(entitiesWalker);
      Preconditions.checkNotNull(idFactory);
      Preconditions.checkNotNull(entityFactory);
      Preconditions.checkNotNull(v8ProjectManager);
      Preconditions.checkNotNull(projectFileSystemSupportProvider);
      Preconditions.checkNotNull(resourceSetProvider);
      this.log = log;
      this.v8Model = v8Model;
      this.entitiesWalker = entitiesWalker;
      this.idFactory = idFactory;
      this.entityFactory = entityFactory;
      this.v8ProjectManager = v8ProjectManager;
      this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
      this.resourceSetProvider = resourceSetProvider;
   }

   public Optional<RelatedEntitiesResponse> getRelatedEntities(final RelatedEntitiesRequest request, final ICancellationToken cancellationToken) {
      Preconditions.checkNotNull(request);
      if (request.path != null && !request.path.isBlank()) {
         final RelatedEntitiesResponse response = new RelatedEntitiesResponse();
         response.relatedObjects = new ArrayList();
         response.relatedFunctions = new ArrayList();
         response.localFunctions = new ArrayList();
         final HashSet<Entity> entities = new HashSet();
         final ArrayList<IBmObject> objects = new ArrayList();
         boolean result = this.entitiesWalker.walk((IDocument)null, request.path, request.start, request.finish, this.resourceSetProvider, new EntityVisitor() {
            public boolean visitModule(BmRoot root, Module module) {
               IV8Project project = RelatedEntities.this.v8ProjectManager.getProject(module);
               if (project == null) {
                  return false;
               } else {
                  IProjectFileSystemSupport fileSystemSupport = RelatedEntities.this.projectFileSystemSupportProvider.getProjectFileSystemSupport(project.getDtProject());
                  IFile moduleFile = fileSystemSupport.getFile(module);

                  try {
                     Throwable var6 = null;
                     Object var7 = null;

                     try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(moduleFile.getContents(), moduleFile.getCharset()));

                        try {
                           StringBuilder code = new StringBuilder();
                           CharBuffer charBuffer = CharBuffer.allocate(1024);

                           while(true) {
                              int size = reader.read(charBuffer);
                              if (size <= 0) {
                                 response.code = code.toString();
                                 break;
                              }

                              code.append(charBuffer.array(), 0, size);
                              charBuffer.clear();
                           }
                        } finally {
                           if (reader != null) {
                              reader.close();
                           }

                        }
                     } catch (Throwable var19) {
                        if (var6 == null) {
                           var6 = var19;
                        } else if (var6 != var19) {
                           var6.addSuppressed(var19);
                        }

                        throw var6;
                     }
                  } catch (Exception error) {
                     RelatedEntities.this.log.logError(error);
                  }

                  return false;
               }
            }

            public boolean visitBmObject(BmRoot root, IBmObject owner) {
               objects.add(owner);
               return false;
            }

            public boolean visitForm(BmRoot root, Form form) {
               RelatedEntities.this.entityFactory.createFormEntity(form, cancellationToken).ifPresent((i) -> response.form = i);
               return false;
            }

            public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node) {
               Entity entity = RelatedEntities.this.createEntity(request.path, nodeId, variable, node, cancellationToken);
               if (!entities.add(entity)) {
                  return false;
               } else {
                  response.relatedObjects.add(entity);
                  RelatedEntities.this.traceEntity("object", entity, variable, node);
                  return false;
               }
            }

            public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess, ICompositeNode node) {
               Entity entity = RelatedEntities.this.createEntity(request.path, nodeId, featureAccess, node, cancellationToken);
               if (!entities.add(entity)) {
                  return false;
               } else {
                  for(FeatureEntry featureEntry : RelatedEntities.this.v8Model.getFeatureEntries(featureAccess)) {
                     if (cancellationToken.isCanceled()) {
                        break;
                     }

                     EObject feature = featureEntry.getFeature();
                     if (feature instanceof AbstractMethod) {
                        return false;
                     }

                     if (feature instanceof Method) {
                        return false;
                     }
                  }

                  response.relatedObjects.add(entity);
                  RelatedEntities.this.traceEntity("object", entity, featureAccess, node);
                  return false;
               }
            }

            public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation, ICompositeNode node) {
               Entity entity = RelatedEntities.this.createEntity(request.path, nodeId, invocation, node, cancellationToken);
               if (!entities.add(entity)) {
                  return false;
               } else {
                  response.relatedFunctions.add(entity);
                  RelatedEntities.this.traceEntity("function", entity, invocation, node);
                  return false;
               }
            }
         }, IStatistics.Empty, cancellationToken);
         if (!objects.isEmpty()) {
            response.meta = this.entityFactory.createMetaEntity((IBmObject)objects.get(objects.size() - 1), cancellationToken);
         } else {
            response.meta = null;
         }

         this.entitiesWalker.walk((IDocument)null, request.path, 0, Integer.MAX_VALUE, this.resourceSetProvider, new EntityVisitor() {
            public boolean visitMethod(BmRoot root, String nodeId, Method method, ICompositeNode node) {
               RelatedEntities.this.entityFactory.createMethodEntity(method, node, true, cancellationToken).ifPresent((i) -> response.localFunctions.add(i));
               return false;
            }
         }, IStatistics.Empty, cancellationToken);
         return !result ? Optional.empty() : Optional.of(response);
      } else {
         return Optional.empty();
      }
   }

   private Entity createEntity(String path, String nodeId, EObject eObject, ICompositeNode node, ICancellationToken cancellationToken) {
      Entity entity = new Entity();
      entity.uuid = this.idFactory.createObjectId(path, eObject, cancellationToken);
      entity.ref = nodeId;
      entity.start = node.getTotalOffset();
      entity.finish = node.getTotalEndOffset();
      return entity;
   }

   private void traceEntity(String type, Entity entity, EObject eObject, ICompositeNode node) {
      this.log.trace("sync", type + ": " + entity, () -> {
         StringBuilder sb = new StringBuilder();
         sb.append("Node type:");
         sb.append(eObject.getClass().getName());
         sb.append(System.lineSeparator());
         sb.append("Code:");
         sb.append(System.lineSeparator());
         sb.append(node.getText());
         return sb.toString();
      });
   }
}
