package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.ListenerList;

class StateService implements IStateService {
   private static final ListenerList<IStateListener> listeners = new ListenerList(1);
   private final ILog log;
   private final ISettings settings;
   private ServiceState serviceState;
   private ActionState actionState;
   private AtomicInteger _busy;

   @Inject
   public StateService(ILog log, ISettings settings) {
      this.serviceState = ServiceState.NONE;
      this.actionState = ActionState.INACTIVE;
      this._busy = new AtomicInteger(0);
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settings);
      this.log = log;
      this.settings = settings;
   }

   public void addListener(IStateListener newListener) {
      listeners.add(newListener);
      this.refresh();
   }

   public void removeListener(IStateListener listener) {
      listeners.remove(listener);
   }

   public AIState getState() {
      ServiceState curState = this.serviceState;
      if (!this.settings.hasClientToken()) {
         curState = ServiceState.MISSING_TOKEN;
      }

      return new AIState(curState, this.actionState);
   }

   public void setState(ServiceState serviceState) {
      Preconditions.checkNotNull(serviceState);
      if (!this.settings.hasClientToken()) {
         serviceState = ServiceState.MISSING_TOKEN;
      }

      if (this.serviceState != serviceState || serviceState.isAllowDuplicates()) {
         this.serviceState = serviceState;
         this.notifyServiceStateChanged();
      }

   }

   public void refresh() {
      AIState state = this.getState();
      this.log.trace("api_calls", "StateService", () -> state.toString());
      this.notifyServiceStateChanged();
      this.notifyActionStateChanged();
   }

   public AutoCloseable busy() {
      if (this._busy.incrementAndGet() == 1) {
         this.setState(ActionState.BUSY);
      }

      return Closeables.create(() -> {
         if (this._busy.decrementAndGet() == 0) {
            this.setState(ActionState.INACTIVE);
         }

      });
   }

   private void setState(ActionState actionState) {
      Preconditions.checkNotNull(actionState);
      if (this.actionState != actionState) {
         this.actionState = actionState;
         this.notifyActionStateChanged();
      }

   }

   private void notifyServiceStateChanged() {
      ServiceState serviceState = this.getState().getServiceState();

      for(IStateListener listener : listeners) {
         try {
            listener.onServiceStateChange(serviceState);
         } catch (Throwable error) {
            this.log.logError(error);
         }
      }

   }

   private void notifyActionStateChanged() {
      ActionState actionState = this.getState().getActionState();

      for(IStateListener listener : listeners) {
         try {
            listener.onActionStateChange(actionState);
         } catch (Throwable error) {
            this.log.logError(error);
         }
      }

   }
}
