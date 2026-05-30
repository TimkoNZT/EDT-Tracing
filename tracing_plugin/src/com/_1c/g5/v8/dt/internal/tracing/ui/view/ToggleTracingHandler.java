package com._1c.g5.v8.dt.internal.tracing.ui.view;

import com._1c.g5.v8.dt.internal.tracing.ui.TracingUIActivator;
import com._1c.g5.v8.dt.profiling.core.IProfileTarget;
import com._1c.g5.v8.dt.profiling.core.IProfilingService;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ui.handlers.HandlerUtil;

public class ToggleTracingHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        TracingUIActivator.getDefault().getLog().log(
            new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID, "ToggleTracingHandler.execute called"));
        IProfilingService service = TracingUIActivator.getProfilingService();
        if (service == null) {
            TracingUIActivator.getDefault().getLog().log(
                new Status(IStatus.WARNING, TracingUIActivator.PLUGIN_ID, "ToggleTracingHandler: IProfilingService null"));
            return null;
        }

        Command command = event.getCommand();
        boolean oldState = HandlerUtil.toggleCommandState(command);
        service.toggleTargetWaitingState(!oldState);

        for (IProfileTarget target : getTargets()) {
            if (target != null && target.canProfile()) {
                service.toggleProfiling(target);
            }
        }
        return null;
    }

    private Set<IProfileTarget> getTargets() {
        Set<IProfileTarget> targets = new HashSet<>();

        Object context = DebugUITools.getDebugContext();
        if (context instanceof IProfileTarget) {
            targets.add((IProfileTarget) context);
        } else if (context != null) {
            IProfileTarget target = (IProfileTarget) ((IAdaptable) context).getAdapter(IProfileTarget.class);
            if (target != null) targets.add(target);
        }

        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        for (IDebugTarget dt : launchManager.getDebugTargets()) {
            if (dt instanceof IProfileTarget) {
                targets.add((IProfileTarget) dt);
            } else {
                IProfileTarget target = (IProfileTarget) dt.getAdapter(IProfileTarget.class);
                if (target != null) targets.add(target);
            }
        }

        return targets;
    }
}
