package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.BindingManagerEvent;
import org.eclipse.jface.bindings.IBindingManagerListener;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

class HotKeys implements IHotKeys, IBindingManagerListener {
   private final HashMap<String, KeyBinding> _keyBindigs = new HashMap();

   @Inject
   public HotKeys() {
      IBindingService bindingService = (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
      bindingService.addBindingManagerListener(this);
   }

   public synchronized boolean isTriggered(String bindingId, KeyEvent event) {
      Preconditions.checkNotNull(bindingId);
      Preconditions.checkNotNull(event);
      this.ensureBindingsExists();
      KeyBinding binding = (KeyBinding)this._keyBindigs.get(bindingId);
      return this.isTriggered(bindingId, event, binding);
   }

   public synchronized boolean isTriggered(KeyEvent event) {
      Preconditions.checkNotNull(event);

      for(Map.Entry<String, KeyBinding> entry : this._keyBindigs.entrySet()) {
         if (this.isTriggered((String)entry.getKey(), event, (KeyBinding)entry.getValue())) {
            return true;
         }
      }

      return false;
   }

   private boolean isTriggered(String bindingId, KeyEvent event, KeyBinding binding) {
      if (binding == null) {
         return false;
      } else {
         KeyStroke[] bindingKeyStrokes = binding.getKeySequence().getKeyStrokes();
         if (bindingKeyStrokes == null) {
            return false;
         } else {
            List<KeyStroke> eventKeyStrokes = generatePossibleKeyStrokes(event);
            return eventKeyStrokes == null ? false : Arrays.asList(bindingKeyStrokes).equals(eventKeyStrokes);
         }
      }
   }

   public synchronized KeyBinding getBinding(String bindingId) {
      return (KeyBinding)this._keyBindigs.get(bindingId);
   }

   private static List<KeyStroke> generatePossibleKeyStrokes(KeyEvent event) {
      ArrayList<KeyStroke> keyStrokes = new ArrayList(3);
      if (event.stateMask == 0 && event.keyCode == 0 && event.character == 0) {
         return keyStrokes;
      } else {
         int firstAccelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(event);
         keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(firstAccelerator));
         return keyStrokes;
      }
   }

   private synchronized void ensureBindingsExists() {
      if (this._keyBindigs.size() <= 0) {
         IBindingService bindingService = this.getBindingManager();

         Binding[] var5;
         for(Binding binding : var5 = bindingService.getBindings()) {
            if (binding instanceof KeyBinding) {
               ParameterizedCommand command = binding.getParameterizedCommand();
               if (command != null) {
                  String id = command.getId();
                  if (id != null && id.startsWith("com.e1c.edt.ai.ui.commands.")) {
                     this._keyBindigs.put(id, (KeyBinding)binding);
                  }
               }
            }
         }

      }
   }

   private IBindingService getBindingManager() {
      return (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
   }

   public synchronized void bindingManagerChanged(BindingManagerEvent event) {
      if (event.isSchemeChanged()) {
         this._keyBindigs.clear();
      }

   }
}
