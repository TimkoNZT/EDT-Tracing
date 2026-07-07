package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import java.time.Duration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

interface IProjectTrackingWorkflow {
   IProjectTrackingWorkflow initialize(IProject var1);

   IProject getProject();

   Duration nextState(IProgressMonitor var1, ICancellationToken var2);

   void track(AIContext var1);
}
