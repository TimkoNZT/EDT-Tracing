package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;

class GlobalContextTracker implements IGlobalContextTracker, IStateListener {
   private final ISettings settings;
   private final IDispatcher dispatcher;
   private final IStateService stateService;
   private final Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider;
   private final ConcurrentHashMap<String, IProjectTrackingWorkflow> projectWorkflows = new ConcurrentHashMap();

   @Inject
   public GlobalContextTracker(ISettings settings, IDispatcher dispatcher, IStateService stateService, Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider) {
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(stateService);
      Preconditions.checkNotNull(projectTrackingWorkflowProvider);
      this.settings = settings;
      this.dispatcher = dispatcher;
      this.stateService = stateService;
      this.projectTrackingWorkflowProvider = projectTrackingWorkflowProvider;
      stateService.addListener(this);
   }

   public Optional<IProjectTrackingWorkflow> track(IProject project) {
      if (!this.settings.isEnabled()) {
         return Optional.empty();
      } else if (project != null && project.exists()) {
         String workflowKey = project.getName();
         IProjectTrackingWorkflow existing = (IProjectTrackingWorkflow)this.projectWorkflows.get(workflowKey);
         if (existing != null) {
            return Optional.of(existing);
         } else {
            IProjectTrackingWorkflow candidate = (IProjectTrackingWorkflow)this.projectTrackingWorkflowProvider.get();
            IProjectTrackingWorkflow previous = (IProjectTrackingWorkflow)this.projectWorkflows.putIfAbsent(workflowKey, candidate);
            if (previous != null) {
               return Optional.of(previous);
            } else {
               try {
                  candidate.initialize(project);
                  this.scheduleTracking(workflowKey, candidate, 0L);
               } catch (RuntimeException error) {
                  this.projectWorkflows.remove(workflowKey, candidate);
                  throw error;
               }

               return Optional.of(candidate);
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public void track(AIContext aiCtx) {
      this.track(aiCtx.getProjectId().project).ifPresent((workflow) -> workflow.track(aiCtx));
   }

   private void scheduleTracking(String workflowKey, IProjectTrackingWorkflow workflow, long delayMs) {
      ICancellationToken cancellationToken = CancellationTokens.manual(CancellationTokens.NONE, () -> !this.settings.isEnabled());
      Job job = this.dispatcher.createJob(Messages.BackgroundJobName, (jobCtx) -> this.track(jobCtx, workflowKey, workflow), false, cancellationToken);
      job.setSystem(true);
      job.setPriority(50);
      job.schedule(delayMs);
   }

   private void track(JobContext jobCtx, String workflowKey, IProjectTrackingWorkflow workflow) {
      if (!jobCtx.CancellationTokenSource.isCanceled() && this.settings.isEnabled()) {
         if (!workflow.getProject().exists()) {
            this.projectWorkflows.remove(workflowKey);
         } else {
            Duration delay = Duration.ofSeconds(5L);

            try {
               delay = workflow.nextState(jobCtx.Monitor, jobCtx.CancellationTokenSource);
            } finally {
               this.scheduleTracking(workflowKey, workflow, delay.toMillis());
            }

         }
      } else {
         this.projectWorkflows.clear();
      }
   }

   public void onServiceStateChange(ServiceState serviceState) {
      this.projectWorkflows.clear();
   }

   public void onActionStateChange(ActionState actionState) {
   }
}
