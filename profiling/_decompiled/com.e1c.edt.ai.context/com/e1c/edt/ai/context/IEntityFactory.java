package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.DTO.FormEntity;
import com.e1c.edt.ai.context.DTO.MetaEntity;
import com.e1c.edt.ai.context.DTO.MethodEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntity;
import java.util.List;
import java.util.Optional;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

public interface IEntityFactory {
   Optional<FormEntity> createFormEntity(Form var1, ICancellationToken var2);

   Optional<ObjectEntity> crateObjectEntity(Variable var1, ICompositeNode var2, boolean var3, ICancellationToken var4);

   Optional<ObjectEntity> crateObjectEntity(FeatureAccess var1, ICompositeNode var2, boolean var3, ICancellationToken var4);

   Optional<MethodEntity> createMethodEntity(Invocation var1, ICompositeNode var2, boolean var3, ICancellationToken var4);

   Optional<MethodEntity> createMethodEntity(Method var1, ICompositeNode var2, boolean var3, ICancellationToken var4);

   MetaEntity createMetaEntity(IBmObject var1, ICancellationToken var2);

   Optional<List<String>> getEnvironments(EObject var1, ICancellationToken var2);

   Optional<List<String>> getAreas(EObject var1, ICancellationToken var2);
}
