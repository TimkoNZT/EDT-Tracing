package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ValidationError;
import com.e1c.edt.ai.ValidationResult;
import com.e1c.edt.ai.WellknownError;
import java.util.TreeMap;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

class ValidatingStringFieldEditor extends StringFieldEditor {
   private static final TreeMap<WellknownError, String> Errors = new TreeMap();
   protected IValidator<String> validator;

   static {
      Errors.put(WellknownError.UnableToParse, Messages.Error_UnableToParse);
      Errors.put(WellknownError.OutOfRange, Messages.Error_OutOfRange);
      Errors.put(WellknownError.Unknown, Messages.Error_Unknown);
      Errors.put(WellknownError.InvalidToken, Messages.Error_InvalidToken);
   }

   public ValidatingStringFieldEditor(String name, String labelText, Composite parent, IValidator<String> validator) {
      super(name, labelText, parent);
      this.validator = validator;
   }

   public boolean doCheckState() {
      if (!super.doCheckState()) {
         return false;
      } else {
         ValidationResult validationResult = this.validator.validate(this.getStringValue());
         if (validationResult.getErrors().isEmpty()) {
            return true;
         } else {
            TreeMap<WellknownError, StringBuilder> stringBuilders = new TreeMap();

            for(ValidationError validationError : validationResult.getErrors()) {
               WellknownError error = validationError.getError();
               String message = (String)Errors.get(error);
               if (message != null) {
                  StringBuilder stringBuilder = (StringBuilder)stringBuilders.computeIfAbsent(error, (k) -> new StringBuilder());
                  if (stringBuilder.length() == 0) {
                     stringBuilder.append(message);
                     if (error != WellknownError.InvalidToken) {
                        stringBuilder.append(": ");
                     }
                  } else {
                     stringBuilder.append(", ");
                  }

                  if (error != WellknownError.InvalidToken) {
                     stringBuilder.append(validationError.getTarget());
                  }
               }
            }

            StringBuilder errors = new StringBuilder();

            for(StringBuilder stringBuilder : stringBuilders.values()) {
               if (errors.length() > 0) {
                  errors.append(". ");
               }

               errors.append(stringBuilder);
            }

            this.setErrorMessage(errors.toString());
            return false;
         }
      }
   }
}
