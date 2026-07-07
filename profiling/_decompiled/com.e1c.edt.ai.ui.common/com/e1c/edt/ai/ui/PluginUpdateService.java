package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.google.inject.Inject;
import java.net.URI;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class PluginUpdateService implements IPluginUpdateService {
   @Inject
   private IUINotificationService notificationService;
   @Inject
   private ILog log;
   @Inject
   private IDispatcher dispatcher;
   @Inject
   ISettings settings;
   @Inject
   private IDefaultSettings defaultSettings;

   public void checkForUpdates(IProgressMonitor monitor) {
      try {
         IProvisioningAgent agent = this.getAgent();
         if (agent == null) {
            return;
         }

         IProfileRegistry profiles = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
         IProfile profile = profiles.getProfile("_SELF_");
         if (profile == null) {
            this.log.trace("common", "Update service is not available", () -> "");
            return;
         }

         IQuery<IInstallableUnit> featureQuery = QueryUtil.createIUQuery(this.defaultSettings.getPluginFeature());
         IQueryResult<IInstallableUnit> installedResult = profile.query(featureQuery, monitor);
         if (installedResult.isEmpty()) {
            this.log.trace("common", "The plugin is missing from the Installed Software", () -> "");
            return;
         }

         Version currentVersion = ((IInstallableUnit)installedResult.iterator().next()).getVersion();
         IMetadataRepositoryManager repositoryManager = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
         IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
         URI repositoryUri = new URI(this.settings.getUpdateUrl());
         if (!repositoryManager.contains(repositoryUri)) {
            this.log.trace("common", "Adding content repository...", () -> "");
            repositoryManager.addRepository(repositoryUri);
         }

         if (!artifactManager.contains(repositoryUri)) {
            this.log.trace("common", "Adding artifacts repository...", () -> "");
            artifactManager.addRepository(repositoryUri);
         }

         IMetadataRepository repo = repositoryManager.loadRepository(repositoryUri, monitor);
         artifactManager.loadRepository(repositoryUri, monitor);
         IQueryResult<IInstallableUnit> availableResult = repo.query(featureQuery, monitor);
         IInstallableUnit latestIU = null;

         for(IInstallableUnit iu : availableResult) {
            if (latestIU == null || iu.getVersion().compareTo(latestIU.getVersion()) > 0) {
               latestIU = iu;
            }
         }

         if (latestIU == null) {
            return;
         }

         Version latestVersion = latestIU.getVersion();
         if (latestVersion.compareTo(currentVersion) > 0) {
            this.dispatcher.dispatchAsync(() -> this.notificationService.createNotificationWithAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.UpdateMessage, () -> this.installUpdate(agent, latestIU), UINotificationService.UINotificationActionType.UPDATE, UINotificationType.INFO));
         }
      } catch (Exception error) {
         this.log.logError(error);
      }

   }

   private void installUpdate(IProvisioningAgent agent, IInstallableUnit latestIU) {
      Job job = Job.create(Messages.UpdatePluginJob, (jobCtx) -> {
         try {
            ProvisioningSession session = new ProvisioningSession(agent);
            InstallOperation installOp = new InstallOperation(session, List.of(latestIU));
            IStatus resolveStatus = installOp.resolveModal(new NullProgressMonitor());
            if (resolveStatus.getSeverity() == 4) {
               this.log.trace("common", "Failed to resolve dependencies", () -> "");
               this.dispatcher.dispatch((Runnable)(() -> this.notificationService.createNotification(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.UpdateError, (String)null, (String)null, UINotificationType.ERROR)));
               return;
            }

            ProvisioningJob installJob = installOp.getProvisioningJob(new NullProgressMonitor());
            installJob.addJobChangeListener(new JobChangeAdapter() {
               public void done(IJobChangeEvent event) {
                  if (event.getResult().isOK()) {
                     PluginUpdateService.this.log.trace("common", "The update has been installed", () -> "");
                     PluginUpdateService.this.dispatcher.dispatchAsync(() -> {
                        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                        PluginUpdateService.this.notificationService.createNotificationWithAction(shell, Messages.UpdateInstalled, () -> PlatformUI.getWorkbench().restart(), UINotificationService.UINotificationActionType.RELOAD, UINotificationType.INFO);
                     });
                  } else {
                     PluginUpdateService.this.log.logError("Error during update installation");
                     PluginUpdateService.this.dispatcher.dispatch((Runnable)(() -> PluginUpdateService.this.notificationService.createNotification(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.UpdateError, (String)null, (String)null, UINotificationType.ERROR)));
                  }

               }
            });
            installJob.schedule();
         } catch (Exception e) {
            this.log.logError(e);
            this.dispatcher.dispatch((Runnable)(() -> this.notificationService.createNotification(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.UpdateError, (String)null, (String)null, UINotificationType.ERROR)));
         }

      });
      job.setSystem(true);
      job.setPriority(50);
      job.schedule();
   }

   private IProvisioningAgent getAgent() {
      BundleContext context = BaseActivator.getDefault().getBundle().getBundleContext();
      ServiceReference<IProvisioningAgentProvider> agentProviderRef = context.getServiceReference(IProvisioningAgentProvider.class);
      IProvisioningAgentProvider agentProvider = (IProvisioningAgentProvider)context.getService(agentProviderRef);
      IProvisioningAgent agent = null;

      try {
         agent = agentProvider.createAgent((URI)null);
         return agent;
      } catch (ProvisionException error) {
         this.log.logError(error);
         return null;
      }
   }
}
