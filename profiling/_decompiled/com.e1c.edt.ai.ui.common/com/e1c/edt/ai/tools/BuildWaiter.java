package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectBuilder;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

public class BuildWaiter implements IBuildWaiter {
   private static final Object[] BUILD_FAMILIES;
   private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
   private final IProjectBuilder builder;

   static {
      BUILD_FAMILIES = Arrays.asList(ResourcesPlugin.FAMILY_AUTO_BUILD, ResourcesPlugin.FAMILY_MANUAL_BUILD, ResourcesPlugin.FAMILY_AUTO_REFRESH, ResourcesPlugin.FAMILY_MANUAL_REFRESH).toArray();
   }

   @Inject
   public BuildWaiter(Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IProjectBuilder builder) {
      Preconditions.checkNotNull(cancellationProgressMonitor);
      Preconditions.checkNotNull(builder);
      this.cancellationProgressMonitor = cancellationProgressMonitor;
      this.builder = builder;
   }

   public CompletableFuture<Void> waitForBuilds(IProject project, ICancellationToken cancellationToken) {
      IJobManager jobManager = Job.getJobManager();
      boolean hasActiveJobs = Arrays.stream(BUILD_FAMILIES).flatMap((family) -> Arrays.stream(jobManager.find(family))).anyMatch((job) -> job.getState() == 4 || job.getState() == 2);
      return !hasActiveJobs ? CompletableFuture.completedFuture((Object)null) : CompletableFuture.runAsync(() -> {
         try {
            this.builder.build(project, cancellationToken);
            ICancellationProgressMonitor monitor = (ICancellationProgressMonitor)this.cancellationProgressMonitor.get();
            monitor.setCancellationToken(cancellationToken);
            jobManager.join(BUILD_FAMILIES, monitor);
         } catch (OperationCanceledException e) {
            throw new CompletionException("Build waiting was cancelled", e);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Build waiting was interrupted", e);
         } catch (CoreException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Build failed", e);
         }
      });
   }
}
