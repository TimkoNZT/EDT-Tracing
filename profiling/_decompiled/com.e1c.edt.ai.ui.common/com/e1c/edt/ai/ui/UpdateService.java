package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.jobs.Job;

public class UpdateService implements IInitializable {
   private final IPluginUpdateService pluginUpdateService;
   private final ISettings settings;
   private final IDispatcher dispatcher;

   @Inject
   public UpdateService(IPluginUpdateService pluginUpdateService, ISettings settings, IDispatcher dispatcher) {
      Preconditions.checkNotNull(pluginUpdateService);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(dispatcher);
      this.pluginUpdateService = pluginUpdateService;
      this.settings = settings;
      this.dispatcher = dispatcher;
   }

   public void initialize() {
      this.scheduleUpdate(TimeUnit.SECONDS.toMillis(30L));
   }

   private void scheduleUpdate(long delayMs) {
      Job updateJob = this.dispatcher.createJob(Messages.UpdateJobMessage, (jobCtx) -> {
         if (!jobCtx.CancellationTokenSource.isCanceled()) {
            if (this.settings.isEnabled()) {
               this.pluginUpdateService.checkForUpdates(jobCtx.Monitor);
            }

            this.scheduleUpdate(TimeUnit.DAYS.toMillis(1L));
         }
      }, true, CancellationTokens.NONE);
      updateJob.setPriority(50);
      updateJob.setSystem(true);
      updateJob.schedule(delayMs);
   }
}
