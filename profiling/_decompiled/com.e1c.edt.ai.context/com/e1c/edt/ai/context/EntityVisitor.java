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

class EntityVisitor implements IEntityVisitor {
   public boolean visitModule(BmRoot root, Module module) {
      return false;
   }

   public boolean visitNode(BmRoot root, EObject eObject, ICompositeNode node) {
      return false;
   }

   public boolean visitBmObject(BmRoot root, IBmObject owner) {
      return false;
   }

   public boolean visitForm(BmRoot root, Form form) {
      return false;
   }

   public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation, ICompositeNode node) {
      return false;
   }

   public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess, ICompositeNode node) {
      return false;
   }

   public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node) {
      return false;
   }

   public boolean visitMethod(BmRoot root, String nodeId, Method method, ICompositeNode node) {
      return false;
   }
}
