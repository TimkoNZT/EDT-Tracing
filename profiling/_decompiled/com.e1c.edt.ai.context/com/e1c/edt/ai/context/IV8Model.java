package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.typesytem.VariableTypeStateProviderCollector;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.Environments;
import com.e1c.edt.ai.ICancellationToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.util.Pair;

public interface IV8Model {
   IBmObject getBmObjectOwner(IBmModel var1, EObject var2);

   List<Type> getTypes(VariableTypeStateProviderCollector var1, ICompositeNode var2);

   List<TypeItem> getTypes(EObject var1);

   Collection<Pair<Collection<Property>, TypeItem>> getProperties(Collection<TypeItem> var1, Resource var2);

   List<FeatureEntry> getFeatureEntries(FeatureAccess var1);

   Optional<String> getPath(FeatureAccess var1);

   TypesComputer getTypesComputer();

   Environments getEnvironments(EObject var1);

   List<String> getComment(EObject var1);

   BslDocumentationComment getComment(Method var1, boolean var2);

   BslDocumentationComment getComment(BslContextDefMethod var1, boolean var2);

   ICompositeNode getNode(EObject var1);

   List<Type> getTypes(FeatureAccess var1);

   Optional<EObject> getMethodFeature(FeatureAccess var1, ICancellationToken var2);

   <T> T getResourceService(Class<T> var1);
}
