package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class ResourceListener implements IInitializable, IResourceChangeListener {
   private final ILog log;
   private final IProjectTrackingDeltaVisitor visitor;

   @Inject
   public ResourceListener(ILog log, IProjectTrackingDeltaVisitor visitor) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(visitor);
      this.log = log;
      this.visitor = visitor;
   }

   public void initialize() {
      ResourcesPlugin.getWorkspace().addResourceChangeListener(this, 1);
   }

   public void resourceChanged(IResourceChangeEvent event) {
      if (event.getType() == 1) {
         IResourceDelta rootDelta = event.getDelta();
         if (rootDelta != null) {
            try {
               rootDelta.accept(this.visitor);
            } catch (CoreException error) {
               this.log.logError(error);
            }

         }
      }
   }
}
