package com._1c.g5.v8.dt.internal.tracing.ui.view;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "com._1c.g5.v8.dt.internal.tracing.ui.view.messages";

    public static String TraceView_Module;
    public static String TraceView_Method;
    public static String TraceView_LineNumber;
    public static String TraceView_Line;
    public static String TraceView_Calls;
    public static String TraceView_Duration;
    public static String TraceView_PureTime;
    public static String TraceView_Percent;
    public static String TraceView_Target;
    public static String TraceView_ServerCall;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
