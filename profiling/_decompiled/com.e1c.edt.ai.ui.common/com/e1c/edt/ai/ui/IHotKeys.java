package com.e1c.edt.ai.ui;

import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.swt.events.KeyEvent;

interface IHotKeys {
   String PREFIX = "com.e1c.edt.ai.ui.commands.";
   String SUGGEST = "com.e1c.edt.ai.ui.commands.suggest.ai";
   String ACCEPT = "com.e1c.edt.ai.ui.commands.accept.ai";
   String ACCEPT_PART = "com.e1c.edt.ai.ui.commands.acceptpart.ai";
   String ACCEPT_LINE = "com.e1c.edt.ai.ui.commands.acceptline.ai";
   String ROLLBACK_PART = "com.e1c.edt.ai.ui.commands.rollbackpart.ai";
   String FINISH = "com.e1c.edt.ai.ui.commands.stop.ai";

   boolean isTriggered(String var1, KeyEvent var2);

   boolean isTriggered(KeyEvent var1);

   KeyBinding getBinding(String var1);
}
