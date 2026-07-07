package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

interface IEntityVisitor {
   boolean visitModule(BmRoot var1, Module var2);

   boolean visitNode(BmRoot var1, EObject var2, ICompositeNode var3);

   boolean visitBmObject(BmRoot var1, IBmObject var2);

   boolean visitForm(BmRoot var1, Form var2);

   boolean visitInvocation(BmRoot var1, String var2, Invocation var3, ICompositeNode var4);

   boolean visitFeatureAccess(BmRoot var1, String var2, FeatureAccess var3, ICompositeNode var4);

   boolean visitVariable(BmRoot var1, String var2, Variable var3, ICompositeNode var4);

   boolean visitMethod(BmRoot var1, String var2, Method var3, ICompositeNode var4);
}
