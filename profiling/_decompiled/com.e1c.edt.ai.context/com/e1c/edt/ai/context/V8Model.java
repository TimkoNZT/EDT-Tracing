package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslCommentUtils;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.BslContextDefPackage;
import com._1c.g5.v8.dt.bsl.model.BslPackage;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.SourceObjectLinkProvider;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.typesytem.TypeSystemMode;
import com._1c.g5.v8.dt.bsl.model.typesytem.VariableTypeState;
import com._1c.g5.v8.dt.bsl.model.typesytem.VariableTypeStateProvider;
import com._1c.g5.v8.dt.bsl.model.typesytem.VariableTypeStateProviderCollector;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.Environments;
import com._1c.g5.v8.dt.md.IExternalPropertyManagerRegistry;
import com._1c.g5.v8.dt.metadata.IExternalPropertyManager;
import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider.Registry;
import org.eclipse.xtext.util.Pair;

class V8Model implements IV8Model {
   private static final String RESOURCE_PREFIX = "/resource";
   private final BslMultiLineCommentDocumentationProvider commentDocumentationProvider;
   private final IExternalPropertyManagerRegistry externalPropertyManagerRegistry;
   private final IModuleProvider moduleProvider;

   @Inject
   public V8Model(BslMultiLineCommentDocumentationProvider commentDocumentationProvider, IExternalPropertyManagerRegistry externalPropertyManagerRegistry, IModuleProvider moduleProvider) {
      Preconditions.checkNotNull(commentDocumentationProvider);
      Preconditions.checkNotNull(externalPropertyManagerRegistry);
      Preconditions.checkNotNull(moduleProvider);
      this.commentDocumentationProvider = commentDocumentationProvider;
      this.externalPropertyManagerRegistry = externalPropertyManagerRegistry;
      this.moduleProvider = moduleProvider;
   }

   public IBmObject getBmObjectOwner(IBmModel bmModel, EObject object) {
      Preconditions.checkNotNull(bmModel);
      Preconditions.checkNotNull(object);
      IExternalPropertyManager externalPropertyManager = this.externalPropertyManagerRegistry.getExternalPropertyManager(bmModel);
      return (IBmObject)externalPropertyManager.getOwner(object, IBmObject.class);
   }

   public List<Type> getTypes(VariableTypeStateProviderCollector typeStateProviders, ICompositeNode node) {
      ArrayList<Type> result = new ArrayList();
      if (typeStateProviders == null) {
         return result;
      } else {
         VariableTypeStateProvider typeStateProvider = typeStateProviders.get(TypeSystemMode.NORMAL);
         if (typeStateProvider == null) {
            return result;
         } else {
            for(VariableTypeState state : typeStateProvider.getAll()) {
               int offset = state.getOffset();
               if (offset >= node.getTotalOffset() && offset <= node.getTotalEndOffset()) {
                  for(TypeItem type : state.getTypes()) {
                     if (type instanceof Type) {
                        result.add((Type)type);
                     }
                  }
               }
            }

            return result;
         }
      }
   }

   public List<TypeItem> getTypes(EObject eObject) {
      return ((TypesComputer)this.getResourceService(TypesComputer.class)).computeTypes(eObject, this.getEnvironments(eObject));
   }

   public Collection<Pair<Collection<Property>, TypeItem>> getProperties(Collection<TypeItem> types, Resource resource) {
      DynamicFeatureAccessComputer dynamicComputer = (DynamicFeatureAccessComputer)this.getResourceService(DynamicFeatureAccessComputer.class);
      return dynamicComputer.getAllProperties(types, resource);
   }

   public List<FeatureEntry> getFeatureEntries(FeatureAccess featureAccess) {
      if (featureAccess != null) {
         if (featureAccess instanceof DynamicFeatureAccess) {
            DynamicFeatureAccess dynamicFeatureAccess = (DynamicFeatureAccess)featureAccess;
            Environments envs = ((Environmental)EcoreUtil2.getContainerOfType(dynamicFeatureAccess, Environmental.class)).environments();
            DynamicFeatureAccessComputer dynamicComputer = (DynamicFeatureAccessComputer)this.getResourceService(DynamicFeatureAccessComputer.class);
            return dynamicComputer.getLastObject(dynamicFeatureAccess, envs, true);
         }

         if (featureAccess instanceof StaticFeatureAccess) {
            return ((StaticFeatureAccess)featureAccess).getFeatureEntries();
         }
      }

      return new ArrayList();
   }

   public Optional<String> getPath(FeatureAccess featureAccess) {
      if (featureAccess == null) {
         return Optional.empty();
      } else if (featureAccess instanceof DynamicFeatureAccess) {
         DynamicFeatureAccess dynamicFeatureAccess = (DynamicFeatureAccess)featureAccess;
         Environments envs = ((Environmental)EcoreUtil2.getContainerOfType(dynamicFeatureAccess, Environmental.class)).environments();
         DynamicFeatureAccessComputer dynamicComputer = (DynamicFeatureAccessComputer)this.getResourceService(DynamicFeatureAccessComputer.class);
         List<FeatureEntry> features = dynamicComputer.getLastObject(dynamicFeatureAccess, envs, true);
         return this.getPath(features);
      } else {
         return featureAccess instanceof StaticFeatureAccess ? this.getPath((List)((StaticFeatureAccess)featureAccess).getFeatureEntries()) : Optional.empty();
      }
   }

   public TypesComputer getTypesComputer() {
      return (TypesComputer)this.getResourceService(TypesComputer.class);
   }

   public List<String> getComment(EObject eObject) {
      return this.commentDocumentationProvider.getCommentLines(eObject);
   }

   public BslDocumentationComment getComment(Method method, boolean oldFormat) {
      return BslCommentUtils.parseTemplateComment(method, oldFormat, this.commentDocumentationProvider);
   }

   public BslDocumentationComment getComment(BslContextDefMethod method, boolean oldFormat) {
      return BslCommentUtils.parseTemplateComment(method, oldFormat);
   }

   private Optional<String> getPath(List<FeatureEntry> features) {
      for(FeatureEntry uniqueFeature : features) {
         EObject feature = uniqueFeature.getFeature();
         if (feature != null) {
            EPackage ePackage = feature.eClass().getEPackage();
            if (ePackage == BslPackage.eINSTANCE && !feature.eIsProxy()) {
               return this.getPath(EcoreUtil.getURI(feature));
            }

            if (ePackage == BslContextDefPackage.eINSTANCE && feature instanceof SourceObjectLinkProvider) {
               return this.getPath(((SourceObjectLinkProvider)feature).getSourceUri());
            }
         }
      }

      return Optional.empty();
   }

   private Optional<String> getPath(URI baseUri) {
      if (baseUri == null) {
         return Optional.empty();
      } else {
         String path = baseUri.path();
         if (path != null && !path.isBlank()) {
            if (path.startsWith("/resource")) {
               path = path.substring("/resource".length());
            }

            return Optional.ofNullable(path);
         } else {
            return Optional.empty();
         }
      }
   }

   public <T> T getResourceService(Class<T> type) {
      IResourceServiceProvider resourceServiceProvider = Registry.INSTANCE.getResourceServiceProvider(URI.createFileURI("*.bsl"));
      return (T)resourceServiceProvider.get(type);
   }

   public Environments getEnvironments(EObject eObject) {
      Environmental environmental = (Environmental)EcoreUtil2.getContainerOfType(eObject, Environmental.class);
      return environmental != null ? environmental.environments() : Environments.EMPTY;
   }

   public ICompositeNode getNode(EObject eObject) {
      for(EObject obj = eObject; obj != null; obj = obj.eContainer()) {
         ICompositeNode node = NodeModelUtils.getNode(obj);
         if (node != null) {
            return node;
         }
      }

      return null;
   }

   public List<Type> getTypes(FeatureAccess featureAccess) {
      return (List<Type>)(featureAccess == null ? new ArrayList() : this.getTypes((EObject)featureAccess, (List)this.getTypesComputer().compute(featureAccess, this.getEnvironments(featureAccess))));
   }

   private List<Type> getTypes(EObject contextObject, List<TypeItem> typeItems) {
      HashSet<Type> types = new HashSet();

      for(TypeItem typeItem : typeItems) {
         this.fillType(contextObject, typeItem, types);
      }

      return new ArrayList(types);
   }

   private void fillType(EObject contextObject, TypeItem typeItem, HashSet<Type> types) {
      if (typeItem instanceof Type) {
         Type type = (Type)typeItem;
         if (type.eIsProxy()) {
            TypeItem proxy = (TypeItem)EcoreUtil.resolve(type, contextObject);
            if (types.add(type)) {
               this.fillType(type, proxy, types);
            }
         }
      } else {
         for(EObject ref : typeItem.eCrossReferences()) {
            if (ref instanceof Type) {
               types.add((Type)ref);
            }
         }
      }

   }

   public Optional<EObject> getMethodFeature(FeatureAccess methodAccess, ICancellationToken cancellationToken) {
      if (methodAccess == null) {
         return Optional.empty();
      } else {
         ArrayList<Module> modules = new ArrayList();
         this.getPath(methodAccess).ifPresent((path) -> this.moduleProvider.getModule((IDocument)null, path, cancellationToken).ifPresent((moduleInfo) -> modules.add(moduleInfo.getModule())));

         for(FeatureEntry featureEntry : this.getFeatureEntries(methodAccess)) {
            EObject feature = featureEntry.getFeature();
            if (feature instanceof SourceObjectLinkProvider) {
               SourceObjectLinkProvider sourceLinkProvider = (SourceObjectLinkProvider)feature;
               if (!modules.isEmpty()) {
                  String methodUri = sourceLinkProvider.getSourceUri().toString();
                  Module module = (Module)modules.get(0);

                  for(Method method : module.allMethods()) {
                     if (method.getUniqueName().equals(methodUri)) {
                        feature = method;
                        break;
                     }
                  }
               }
            }

            if (feature instanceof Method || feature instanceof com._1c.g5.v8.dt.mcore.Method) {
               return Optional.of(feature);
            }
         }

         return Optional.empty();
      }
   }
}
