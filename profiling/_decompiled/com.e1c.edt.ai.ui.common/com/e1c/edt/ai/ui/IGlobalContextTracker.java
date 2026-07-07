package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import java.util.Optional;
import org.eclipse.core.resources.IProject;

public interface IGlobalContextTracker {
   Optional<IProjectTrackingWorkflow> track(IProject var1);

   void track(AIContext var1);
}
