package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Completion;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;

class GlobalContextManager implements IGlobalContextManager {
   private final ILog log;
   private final ISettings settings;
   private final IDispatcher dispatcher;
   private final IGlobalContextSync globalContextSync;
   private final IGlobalContextTracker globalContextTracker;
   private Job currentJob;

   @Inject
   public GlobalContextManager(ILog log, ISettings settings, IDispatcher dispatcher, IGlobalContextSync globalContextSync, IGlobalContextTracker globalContextTracker) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(globalContextSync);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(globalContextTracker);
      this.log = log;
      this.settings = settings;
      this.dispatcher = dispatcher;
      this.globalContextSync = globalContextSync;
      this.globalContextTracker = globalContextTracker;
   }

   public void update(AIContext aiCtx, ICancellationToken cancellationToken) {
      if (this.settings.isEnabled()) {
         Job job = this.dispatcher.createJob(Messages.BackgroundJobName, (jobCtx) -> {
            try {
               CompletableFuture<Boolean> syncTask = this.globalContextSync.sync(aiCtx, 5, jobCtx.CancellationTokenSource);
               CancellationTokenSource.attach(jobCtx.CancellationTokenSource, () -> syncTask.cancel(true));
               syncTask.get();
            } catch (ExecutionException error) {
               this.log.trace("sync", "GlobalContextManager", () -> "Error updating global context: " + error);
            } catch (Exception error) {
               this.log.logError(error);
            } finally {
               this.globalContextTracker.track(aiCtx);
            }

         }, false, cancellationToken);
         this.runJob(job);
      }
   }

   public void update(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken) {
      if (this.settings.isEnabled() && completion.unknownValues != null && !completion.unknownValues.isEmpty()) {
         Job job = this.dispatcher.createJob(Messages.CodeCompletionJobName, (jobCtx) -> {
            try {
               CompletableFuture<Boolean> syncTask = this.globalContextSync.syncUnknown(aiCtx, completion.unknownValues, 5, jobCtx.CancellationTokenSource);
               CancellationTokenSource.attach(jobCtx.CancellationTokenSource, () -> syncTask.cancel(true));
               syncTask.get();
            } catch (Exception error) {
               this.log.logError(error);
            }

         }, false, cancellationToken);
         this.runJob(job);
      }
   }

   private synchronized void runJob(Job job) {
      if (this.currentJob != null) {
         this.currentJob.cancel();
         this.currentJob = null;
      }

      this.currentJob = job;
      this.currentJob.setSystem(true);
      this.currentJob.setPriority(50);
      job.schedule();
   }
}
