package com.nzt.edt.tracing;

import com._1c.g5.v8.dt.profiling.core.IProfilingService;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class TracingUIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.nzt.edt.tracing";
    private static TracingUIActivator plugin;
    private BundleContext bundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        bundleContext = context;
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, "TracingUIActivator started"));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        bundleContext = null;
        plugin = null;
        super.stop(context);
    }

    public static TracingUIActivator getDefault() {
        return plugin;
    }

    public static BundleContext getBundleContext() {
        return plugin != null ? plugin.bundleContext : null;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        if (plugin == null) return ImageDescriptor.getMissingImageDescriptor();
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public static IProfilingService getProfilingService() {
        BundleContext ctx = getBundleContext();
        if (ctx == null) {
            getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, "getProfilingService: bundleContext is null"));
            return null;
        }
        ServiceReference<IProfilingService> ref = ctx.getServiceReference(IProfilingService.class);
        if (ref == null) {
            getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, "getProfilingService: IProfilingService not available"));
            return null;
        }
        IProfilingService svc = ctx.getService(ref);
        getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, "getProfilingService: OK"));
        return svc;
    }
}
