package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.mcore.ParamSet;
import com._1c.g5.v8.dt.mcore.Parameter;
import com._1c.g5.v8.dt.mcore.Type;
import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.nodemodel.ICompositeNode;

class IdFactory implements IIdFactory {
   private static String MAX_INT = Integer.toString(Integer.MAX_VALUE);
   private final IV8Model v8Model;

   @Inject
   public IdFactory(IV8Model v8Model) {
      Preconditions.checkNotNull(v8Model);
      this.v8Model = v8Model;
   }

   public String createNodeId(String path, ICompositeNode node) {
      try {
         URL requestPathUrl = new URL("file", "", -1, path);
         int start = node.getTotalOffset();
         int finish = node.getTotalEndOffset();
         return requestPathUrl.toString() + "?start=" + start + "&finish=" + finish;
      } catch (MalformedURLException var6) {
         return "";
      }
   }

   public String createObjectId(String path, EObject eObject, ICancellationToken cancellationToken) {
      if (eObject instanceof FeatureAccess) {
         FeatureAccess featureAccess = (FeatureAccess)eObject;
         List<Type> types = this.v8Model.getTypes(featureAccess);
         if (!types.isEmpty()) {
            StringBuilder urls = new StringBuilder();

            for(Type type : types) {
               Resource resource = type.eResource();
               if (resource != null) {
                  URI uri = resource.getURI();
                  if (uri != null) {
                     if (urls.length() != 0) {
                        urls.append(';');
                     }

                     urls.append(uri);
                  }
               }
            }
         }
      }

      ICompositeNode node = this.v8Model.getNode(eObject);
      if (eObject instanceof Invocation) {
         Invocation invocation = (Invocation)eObject;
         Optional<EObject> methodAccessFeatureOptional = this.v8Model.getMethodFeature(invocation.getMethodAccess(), cancellationToken);
         if (methodAccessFeatureOptional.isPresent()) {
            EObject methodAccessFeature = (EObject)methodAccessFeatureOptional.get();
            if (methodAccessFeature instanceof Method) {
               Method method = (Method)methodAccessFeature;
               return method.getUniqueName();
            }

            if (methodAccessFeature instanceof com._1c.g5.v8.dt.mcore.Method) {
               com._1c.g5.v8.dt.mcore.Method method = (com._1c.g5.v8.dt.mcore.Method)methodAccessFeature;
               Resource resource = method.eResource();
               if (resource != null) {
                  URI uri = resource.getURI();
                  if (uri != null) {
                     StringBuilder id = new StringBuilder();
                     id.append(uri);
                     id.append('.');
                     id.append(method.getName());
                     id.append('(');
                     boolean hasParam = false;

                     for(ParamSet paramSet : method.getParamSet()) {
                        for(Parameter param : paramSet.getParams()) {
                           if (hasParam) {
                              id.append(',');
                           } else {
                              hasParam = true;
                           }

                           id.append(param.getName());
                        }
                     }

                     id.append(')');
                     id.append(method.environments());
                     return id.toString();
                  }
               }
            }
         }
      }

      return this.createNodeId(path, node);
   }

   public Optional<SourceSpan> getNodeId(String nodeId) {
      URL url;
      try {
         url = new URL(nodeId);
      } catch (MalformedURLException var7) {
         return Optional.empty();
      }

      String path = url.getPath();
      Map<String, String> params = Splitter.on('&').trimResults().withKeyValueSeparator('=').split(url.getQuery());
      int start = Integer.parseInt((String)params.getOrDefault("start", "0"));
      int finish = Integer.parseInt((String)params.getOrDefault("finish", MAX_INT));
      return Optional.of(new SourceSpan(path, start, finish));
   }
}
