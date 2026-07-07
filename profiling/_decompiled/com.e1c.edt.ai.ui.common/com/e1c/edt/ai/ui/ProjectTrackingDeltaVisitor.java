package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

class ProjectTrackingDeltaVisitor implements IProjectTrackingDeltaVisitor {
   private final ILog log;
   private final IGlobalContextTracker globalContextTracker;

   @Inject
   public ProjectTrackingDeltaVisitor(ILog log, IGlobalContextTracker globalContextTracker) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(globalContextTracker);
      this.log = log;
      this.globalContextTracker = globalContextTracker;
   }

   public boolean visit(IResourceDelta delta) throws CoreException {
      if ((delta.getKind() & 8194) != 0) {
         this.track(delta);
      }

      return true;
   }

   private void track(IResourceDelta delta) {
      IResource resource = delta.getResource();
      if (resource != null) {
         IProject project = resource.getProject();
         if (project != null) {
            ProjectId projectId = new ProjectId(project);
            String path = resource.getFullPath().makeRelative().toPortableString();
            this.log.trace("sync", "ResourceListener", () -> path + " was updated in project " + project.getName());
            AIContext ctx = new AIContext(projectId, path, (IDocument)null);
            this.globalContextTracker.track(ctx);
         }
      }
   }
}
