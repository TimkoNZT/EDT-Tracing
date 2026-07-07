package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class ActiveProjectTracker implements IInitializable, IResourceChangeListener, IStateListener {
   private final ILog log;
   private final IGlobalContextTracker globalContextTracker;
   private final ISettings settings;
   private final IStateService stateService;

   @Inject
   public ActiveProjectTracker(ILog log, IGlobalContextTracker globalContextTracker, ISettings settings, IStateService stateService) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(globalContextTracker);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(stateService);
      this.log = log;
      this.globalContextTracker = globalContextTracker;
      this.settings = settings;
      this.stateService = stateService;
   }

   public void initialize() {
      this.stateService.addListener(this);
      this.trackAllActiveProjects();
      ResourcesPlugin.getWorkspace().addResourceChangeListener(this, 1);
   }

   private void trackAllActiveProjects() {
      if (this.settings.isEnabled()) {
         IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
         IProject[] projects = workspaceRoot.getProjects();

         for(IProject project : projects) {
            if (project.isAccessible()) {
               this.globalContextTracker.track(project);
            }
         }

      }
   }

   public void resourceChanged(IResourceChangeEvent event) {
      if (event.getType() == 1) {
         if (this.settings.isEnabled()) {
            IResourceDelta rootDelta = event.getDelta();
            if (rootDelta != null) {
               try {
                  rootDelta.accept((delta) -> {
                     if (this.isProjectAdded(delta)) {
                        this.trackProject(delta);
                     }

                     return true;
                  });
               } catch (CoreException error) {
                  this.log.logError(error);
               }

            }
         }
      }
   }

   private boolean isProjectAdded(IResourceDelta delta) {
      int kind = delta.getKind();
      return (kind & 1) != 0;
   }

   private void trackProject(IResourceDelta delta) {
      IResource resource = delta.getResource();
      if (resource != null && resource.getType() == 4) {
         IProject project = (IProject)resource;
         if (project.isAccessible()) {
            this.globalContextTracker.track(project);
         }

      }
   }

   public void onServiceStateChange(ServiceState serviceState) {
      if (serviceState == ServiceState.SETTINGS_CHANGED) {
         this.trackAllActiveProjects();
      }

   }

   public void onActionStateChange(ActionState actionState) {
   }
}
