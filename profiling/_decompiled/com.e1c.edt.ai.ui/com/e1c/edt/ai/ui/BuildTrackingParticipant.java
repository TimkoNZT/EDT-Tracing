package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.builder.IXtextBuilderParticipant;

public class BuildTrackingParticipant implements IXtextBuilderParticipant {
   @Inject
   public ILog log;
   @Inject
   public IGlobalContextTracker globalContextTracker;

   public BuildTrackingParticipant() {
      Activator.injectMembers(this);
   }

   public void build(IXtextBuilderParticipant.IBuildContext context, IProgressMonitor monitor) throws CoreException {
      IProject project = context.getBuiltProject();
      if (project != null) {
         this.log.trace("common", "Building", () -> "The building was registered for project " + project.getName());
         this.globalContextTracker.track(project);
      }
   }
}
