package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CodeCompletionAction;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.swt.events.VerifyEvent;

class UserActions implements IUserActions {
   private static final char SEPARATOR = ' ';
   private static final Map<String, CodeCompletionAction> ACTION_MAP;
   private static final List<String> LABEL_KEYS;
   private final IHotKeys hotKeys;

   static {
      ACTION_MAP = Map.of("com.e1c.edt.ai.ui.commands.suggest.ai", CodeCompletionAction.SUGGEST, "com.e1c.edt.ai.ui.commands.stop.ai", CodeCompletionAction.FINISH, "com.e1c.edt.ai.ui.commands.rollbackpart.ai", CodeCompletionAction.ROLLBACK_PART, "com.e1c.edt.ai.ui.commands.acceptline.ai", CodeCompletionAction.ACCEPT_LINE, "com.e1c.edt.ai.ui.commands.acceptpart.ai", CodeCompletionAction.ACCEPT_PART, "com.e1c.edt.ai.ui.commands.accept.ai", CodeCompletionAction.ACCEPT);
      LABEL_KEYS = List.of("com.e1c.edt.ai.ui.commands.acceptpart.ai", "com.e1c.edt.ai.ui.commands.acceptline.ai", "com.e1c.edt.ai.ui.commands.suggest.ai", "com.e1c.edt.ai.ui.commands.stop.ai", "com.e1c.edt.ai.ui.commands.rollbackpart.ai", "com.e1c.edt.ai.ui.commands.accept.ai");
   }

   @Inject
   public UserActions(IHotKeys hotKeys) {
      Preconditions.checkNotNull(hotKeys);
      this.hotKeys = hotKeys;
   }

   public String getCodeCompletionLabels(char separator) {
      StringBuilder labels = new StringBuilder();

      for(String key : LABEL_KEYS) {
         KeyBinding binding = this.hotKeys.getBinding(key);
         if (binding != null) {
            if (labels.length() > 0) {
               labels.append(separator);
            }

            labels.append(binding.getKeySequence().format());
         }
      }

      if (labels.length() > 0) {
         labels.append(' ');
         labels.append(Messages.HintHotKey_AcceptStop);
      }

      return labels.toString();
   }

   public CodeCompletionAction getAction(VerifyEvent event) {
      Preconditions.checkNotNull(event);

      for(Map.Entry<String, CodeCompletionAction> action : ACTION_MAP.entrySet()) {
         if (this.hotKeys.isTriggered((String)action.getKey(), event)) {
            return (CodeCompletionAction)action.getValue();
         }
      }

      return CodeCompletionAction.ACCEPT_CHAR;
   }
}
