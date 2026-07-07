package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.form.model.Addition;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.Decoration;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.mcore.Field;
import java.util.Optional;
import org.eclipse.emf.ecore.EObject;

interface IFormVisitor {
   void visitFormField(Optional<EObject> var1, FormField var2);

   void visitField(Optional<EObject> var1, Field var2);

   void visitButton(Optional<EObject> var1, Button var2);

   void visitTable(Optional<EObject> var1, Table var2);

   void visitAddition(Optional<EObject> var1, Addition var2);

   void visitDecoration(Optional<EObject> var1, Decoration var2);

   void visitForm(Optional<EObject> var1, Form var2);

   void visitGroup(Optional<EObject> var1, Group var2);

   void visitFormItem(Optional<EObject> var1, FormItem var2);

   void visitEObject(Optional<EObject> var1, EObject var2);
}
