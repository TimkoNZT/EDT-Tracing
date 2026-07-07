package com._1c.g5.v8.dt.internal.tracing.ui.view;

import com._1c.g5.v8.dt.internal.tracing.ui.TracingUIActivator;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;

public class ToggleTracingHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        TracingUIActivator.getDefault().getLog().log(
            new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID,
                "ToggleTracingHandler.execute called"));
        IWorkbenchPage page = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage();
        if (page == null) return null;
        TraceView view = (TraceView) page.findView(TraceView.ID);
        if (view == null) {
            try {
                view = (TraceView) page.showView(TraceView.ID);
            } catch (Exception e) {
                TracingUIActivator.getDefault().getLog().log(
                    new Status(IStatus.ERROR, TracingUIActivator.PLUGIN_ID,
                        "ToggleTracingHandler: failed to open TraceView", e));
                return null;
            }
        }
        boolean nowActive = view.toggleTracing();
        Command cmd = event.getCommand();
        if (cmd != null) {
            State state = cmd.getState(RegistryToggleState.STATE_ID);
            if (state != null) {
                state.setValue(nowActive);
            }
        }
        // Force UI elements to re-read the command state
        ICommandService cs = (ICommandService)
            page.getWorkbenchWindow().getService(ICommandService.class);
        if (cs != null) cs.refreshElements(
            "com._1c.g5.v8.dt.tracing.ui.commands.toggleTracing", null);
        return null;
    }
}
