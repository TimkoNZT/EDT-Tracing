package com.e1c.edt.ai.context;

import com._1c.g5.lwt.AbstractLightControl;
import com._1c.g5.lwt.ILightComposite;
import com._1c.g5.lwt.ILightControl;
import com._1c.g5.lwt.controls.LightCheckbox;
import com._1c.g5.lwt.controls.LightEditorBar;
import com._1c.g5.lwt.controls.LightLabel;
import com._1c.g5.lwt.controls.LightText;
import com._1c.g5.lwt.interop.SwtLightComposite;
import com._1c.g5.lwt.interop.SwtLightControl;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.assistent.model.VisualContext;
import com.e1c.edt.ai.assistent.model.VisualField;
import com.e1c.edt.ai.assistent.model.VisualGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.Section;

public class VisualContextProvider implements IVisualContextProvider {
   public VisualContext create(Object controlObject, ICancellationToken cancellationToken) {
      VisualContext ctx = new VisualContext();
      if (!(controlObject instanceof Control)) {
         return ctx;
      } else {
         Control control = (Control)controlObject;
         Composite root = control.getParent();
         VisualGroup rootGroup = new VisualGroup();

         for(rootGroup.fields = new ArrayList(); root != null && !cancellationToken.isCanceled(); root = root.getParent()) {
            if (root instanceof Form) {
               rootGroup.title = ((Form)root).getText();
               break;
            }

            if (root instanceof Shell) {
               rootGroup.title = ((Shell)root).getText();
               break;
            }

            if (root instanceof Section) {
               rootGroup.title = ((Section)root).getText();
            }
         }

         if (cancellationToken.isCanceled()) {
            return ctx;
         } else {
            ArrayList<VisualGroup> groups = new ArrayList();
            SwtLightControl target = SwtLightComposite.getSwtLightControl(control);
            if (target == null) {
               this.findElements(root, rootGroup, groups, cancellationToken);
            } else {
               ILightComposite parent;
               ILightComposite nextParent;
               for(parent = target.getParent(); !cancellationToken.isCanceled(); parent = nextParent) {
                  nextParent = parent.getParent();
                  if (nextParent == null) {
                     break;
                  }
               }

               this.findElements((ILightComposite)parent, rootGroup, groups, cancellationToken);
            }

            if (rootGroup.title != null && !rootGroup.title.isBlank()) {
               ctx.title = rootGroup.title;
            }

            if (!rootGroup.fields.isEmpty()) {
               ctx.fields = rootGroup.fields;
            }

            if (!groups.isEmpty()) {
               ctx.groups = groups;
            }

            return ctx;
         }
      }
   }

   private void findElements(Composite composite, VisualGroup currentGroup, ArrayList<VisualGroup> groups, ICancellationToken cancellationToken) {
      VisualField visualField = new VisualField();

      Control[] var9;
      for(Control child : var9 = composite.getChildren()) {
         if (cancellationToken.isCanceled()) {
            break;
         }

         if (child instanceof Label) {
            Label item = (Label)child;
            visualField.isFocused = this.isFocused((Control)item);
            String name = visualField.name;
            visualField.name = ((name == null ? "" : name + " ") + item.getText()).trim();
         } else if (child instanceof Text) {
            Text item = (Text)child;
            visualField.isFocused = this.isFocused((Control)item);
            visualField.isMultiline = (item.getStyle() & 2) != 0;
            visualField.value = item.getText();
            currentGroup.fields.add(visualField);
            visualField = new VisualField();
         } else if (child instanceof StyledText) {
            StyledText item = (StyledText)child;
            visualField.isFocused = this.isFocused((Control)item);
            visualField.isMultiline = (item.getStyle() & 2) != 0;
            visualField.value = item.getText();
            currentGroup.fields.add(visualField);
            visualField = new VisualField();
         } else if (child instanceof Composite) {
            Composite item = (Composite)child;
            this.findElements(item, currentGroup, groups, cancellationToken);
         }
      }

   }

   private Boolean isFocused(Control control) {
      return control.isFocusControl();
   }

   private void findElements(ILightComposite composite, VisualGroup currentGroup, List<VisualGroup> groups, ICancellationToken cancellationToken) {
      VisualField visualField = new VisualField();

      for(ILightControl child : composite.getChildren()) {
         if (cancellationToken.isCanceled()) {
            break;
         }

         if (child instanceof LightLabel) {
            LightLabel item = (LightLabel)child;
            visualField.isFocused = this.isFocused((AbstractLightControl)item);
            visualField.name = item.getText();
         } else if (child instanceof LightEditorBar) {
            LightEditorBar editorBar = (LightEditorBar)child;
            ILightControl content = editorBar.getContent();
            if (content instanceof LightText) {
               LightText item = (LightText)content;
               visualField.isFocused = this.isFocused((AbstractLightControl)item);
               visualField.isMultiline = item.isMultiline();
               visualField.value = item.getText();
               currentGroup.fields.add(visualField);
               visualField = new VisualField();
            }
         } else if (child instanceof LightText) {
            LightText item = (LightText)child;
            visualField.isFocused = this.isFocused((AbstractLightControl)item);
            visualField.isMultiline = item.isMultiline();
            visualField.value = item.getText();
            currentGroup.fields.add(visualField);
            visualField = new VisualField();
         } else if (child instanceof LightCheckbox) {
            LightCheckbox item = (LightCheckbox)child;
            visualField.isFocused = this.isFocused((AbstractLightControl)item);
            visualField.isMultiline = false;
            visualField.value = item.isChecked() ? "[X]" : "[ ]";
            currentGroup.fields.add(visualField);
            visualField = new VisualField();
         } else {
            if (child instanceof SwtLightControl) {
               SwtLightControl lightControl = (SwtLightControl)child;
               Control swtControl = lightControl.getSwtControl();
               if (swtControl instanceof Label) {
                  Label item = (Label)swtControl;
                  currentGroup = new VisualGroup();
                  currentGroup.title = item.getText();
                  currentGroup.fields = new ArrayList();
                  groups.add(currentGroup);
               }
            }

            if (child instanceof SwtLightComposite) {
               SwtLightComposite item = (SwtLightComposite)child;
               this.findElements((ILightComposite)item, currentGroup, groups, cancellationToken);
            }
         }
      }

   }

   private boolean isFocused(AbstractLightControl control) {
      return control.isFocused() ? true : (Boolean)Optional.ofNullable(control.getOverlay()).map((i) -> i.getSwtControl()).map((i) -> i.isFocusControl()).orElse(false);
   }
}
