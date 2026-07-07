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

class FormVisitor implements IFormVisitor {
   public void visitFormField(Optional<EObject> parent, FormField field) {
   }

   public void visitField(Optional<EObject> parent, Field field) {
   }

   public void visitButton(Optional<EObject> parent, Button button) {
   }

   public void visitTable(Optional<EObject> parent, Table table) {
   }

   public void visitAddition(Optional<EObject> parent, Addition addition) {
   }

   public void visitDecoration(Optional<EObject> parent, Decoration decoration) {
   }

   public void visitForm(Optional<EObject> parent, Form form) {
   }

   public void visitGroup(Optional<EObject> parent, Group group) {
   }

   public void visitFormItem(Optional<EObject> parent, FormItem formItem) {
   }

   public void visitEObject(Optional<EObject> parent, EObject eObject) {
   }
}
