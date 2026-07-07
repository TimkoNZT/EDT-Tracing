package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.form.model.Addition;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.CommandBarHolder;
import com._1c.g5.v8.dt.form.model.ContextMenuHolder;
import com._1c.g5.v8.dt.form.model.Decoration;
import com._1c.g5.v8.dt.form.model.ExtendedTooltipHolder;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.FormItemContainer;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.form.model.TableHolder;
import com._1c.g5.v8.dt.mcore.Field;
import com._1c.g5.v8.dt.mcore.FieldSource;
import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Optional;
import java.util.Stack;
import org.eclipse.emf.ecore.EObject;

class FormWalker implements IFormWalker {
   public void walk(EObject root, IFormVisitor visitor, ICancellationToken cancellationToken) {
      Preconditions.checkNotNull(root);
      Preconditions.checkNotNull(visitor);
      HashSet<EObject> items = new HashSet();
      Stack<EObject> stack = new Stack();
      this.visit(stack, (EObject)null, root, visitor, items);
      stack.push(root);

      while(stack.size() > 0 && !cancellationToken.isCanceled()) {
         EObject parent = (EObject)stack.pop();
         if (parent instanceof FormItemContainer) {
            FormItemContainer container = (FormItemContainer)parent;

            for(FormItem item : container.getItems()) {
               this.visit(stack, parent, item, visitor, items);
            }
         }

         if (parent instanceof FieldSource) {
            FieldSource fieldSource = (FieldSource)parent;

            for(Field item : fieldSource.getFields()) {
               this.visit(stack, parent, item, visitor, items);
            }
         }

         if (parent instanceof CommandBarHolder) {
            CommandBarHolder holder = (CommandBarHolder)parent;
            this.visit(stack, parent, holder.getAutoCommandBar(), visitor, items);
         }

         if (parent instanceof ContextMenuHolder) {
            ContextMenuHolder holder = (ContextMenuHolder)parent;
            this.visit(stack, parent, holder.getContextMenu(), visitor, items);
         }

         if (parent instanceof ExtendedTooltipHolder) {
            ExtendedTooltipHolder holder = (ExtendedTooltipHolder)parent;
            this.visit(stack, parent, holder.getExtendedTooltip(), visitor, items);
         }

         if (parent instanceof TableHolder) {
            TableHolder holder = (TableHolder)parent;
            this.visit(stack, parent, holder.getAutoTable(), visitor, items);
         }

         if (parent instanceof Table) {
            Table table = (Table)parent;

            for(FormItem item : table.getItems()) {
               this.visit(stack, table, item, visitor, items);
            }
         }
      }

   }

   private void visit(Stack<EObject> stack, EObject parent, EObject item, IFormVisitor visitor, HashSet<EObject> items) {
      if (item != null) {
         if (items.add(item)) {
            stack.push(item);
            if (item instanceof FormField) {
               FormField field = (FormField)item;
               visitor.visitFormField(Optional.ofNullable(parent), field);
            }

            if (item instanceof Field) {
               Field field = (Field)item;
               visitor.visitField(Optional.ofNullable(parent), field);
            }

            if (item instanceof Button) {
               Button button = (Button)item;
               visitor.visitButton(Optional.ofNullable(parent), button);
            }

            if (item instanceof Table) {
               Table table = (Table)item;
               visitor.visitTable(Optional.ofNullable(parent), table);
            }

            if (item instanceof Addition) {
               Addition addition = (Addition)item;
               visitor.visitAddition(Optional.ofNullable(parent), addition);
            }

            if (item instanceof Decoration) {
               Decoration decoration = (Decoration)item;
               visitor.visitDecoration(Optional.ofNullable(parent), decoration);
            }

            if (item instanceof Form) {
               Form form = (Form)item;
               visitor.visitForm(Optional.ofNullable(parent), form);
            }

            if (item instanceof Group) {
               Group group = (Group)item;
               visitor.visitGroup(Optional.ofNullable(parent), group);
            }

            if (item instanceof FormItem) {
               FormItem formItem = (FormItem)item;
               visitor.visitFormItem(Optional.ofNullable(parent), formItem);
            }

            if (item != null) {
               visitor.visitEObject(Optional.ofNullable(parent), item);
            }

         }
      }
   }
}
