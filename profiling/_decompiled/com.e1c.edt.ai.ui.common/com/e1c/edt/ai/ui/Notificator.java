package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.jobs.Job;

public class Notificator implements IStateListener, IInitializable {
   private static final int REPEAT_INTERVAL_DAYS = 1;
   private final IStateService stateService;
   private final INotifications notifications;
   private final IDispatcher dispatcher;
   private final IClock clock;
   private final Object lock = new Object();
   private ServiceState lastServiceState;
   public ServiceState lastShownServiceState;
   public LocalDateTime lastShownTime;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$ServiceState;

   @Inject
   public Notificator(IStateService stateService, INotifications notifications, IDispatcher dispatcher, IClock clock) {
      Preconditions.checkNotNull(stateService);
      Preconditions.checkNotNull(notifications);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(clock);
      this.stateService = stateService;
      this.notifications = notifications;
      this.dispatcher = dispatcher;
      this.clock = clock;
      stateService.addListener(this);
   }

   public void initialize() {
      AIState state = this.stateService.getState();
      this.onServiceStateChange(state.getServiceState());
      this.onActionStateChange(state.getActionState());
      this.scheduleUpdate(TimeUnit.SECONDS.toMillis(15L));
   }

   public void onServiceStateChange(ServiceState serviceState) {
      synchronized(this.lock) {
         if (serviceState != this.lastServiceState) {
            this.lastServiceState = serviceState;
         }
      }
   }

   public void onActionStateChange(ActionState actionState) {
   }

   private void scheduleUpdate(long delayMs) {
      Job updateJob = this.dispatcher.createJob(Messages.UpdateJobMessage, (jobCtx) -> {
         if (!jobCtx.CancellationTokenSource.isCanceled()) {
            ServiceState serviceState;
            synchronized(this.lock) {
               serviceState = this.lastServiceState;
            }

            if (serviceState != null && this.shouldShowNotification(serviceState) && this.showState(serviceState)) {
               synchronized(this.lock) {
                  this.lastShownServiceState = serviceState;
                  this.lastShownTime = this.clock.now();
               }
            }

            this.scheduleUpdate(TimeUnit.SECONDS.toMillis(5L));
         }
      }, true, CancellationTokens.NONE);
      updateJob.setPriority(50);
      updateJob.setSystem(true);
      updateJob.schedule(delayMs);
   }

   public boolean shouldShowNotification(ServiceState serviceState) {
      synchronized(this.lock) {
         if (serviceState == null) {
            return false;
         } else if (serviceState == ServiceState.MISSING_TOKEN) {
            return this.lastShownServiceState != serviceState;
         } else {
            return this.lastShownServiceState == serviceState && this.lastShownTime != null ? this.clock.now().isAfter(this.lastShownTime.plusDays(1L)) : true;
         }
      }
   }

   private boolean showState(ServiceState serviceState) {
      switch (serviceState) {
         case MISSING_TOKEN:
            return this.notifications.showMissingTokenInfo();
         case TOKEN_ERROR:
            return this.notifications.showTokenError();
         case SERVER_ERROR:
         case SETTINGS_CHANGED:
         default:
            return true;
         case SSL_ERROR:
            return this.notifications.showSSLError();
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$ServiceState() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$ServiceState;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[ServiceState.values().length];

         try {
            var0[ServiceState.MISSING_TOKEN.ordinal()] = 2;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[ServiceState.NONE.ordinal()] = 1;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[ServiceState.OFFLINE.ordinal()] = 8;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[ServiceState.ONLINE.ordinal()] = 9;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[ServiceState.SERVER_ERROR.ordinal()] = 4;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[ServiceState.SESSION_EXPIRED.ordinal()] = 7;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[ServiceState.SETTINGS_CHANGED.ordinal()] = 5;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[ServiceState.SSL_ERROR.ordinal()] = 6;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[ServiceState.TOKEN_ERROR.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$ServiceState = var0;
         return var0;
      }
   }
}
