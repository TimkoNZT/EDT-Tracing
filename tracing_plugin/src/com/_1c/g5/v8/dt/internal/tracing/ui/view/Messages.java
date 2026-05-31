package com._1c.g5.v8.dt.internal.tracing.ui.view;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "com._1c.g5.v8.dt.internal.tracing.ui.view.messages";

    public static String TraceView_StepNo;
    public static String TraceView_Target;
    public static String TraceView_Thread;
    public static String TraceView_Frame;
    public static String TraceView_LineNumber;
    public static String TraceView_Time;
    public static String TraceView_Source;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
