package com.nzt.edt.tracing.view;

import org.eclipse.debug.core.model.IStackFrame;

public class TraceStepRecord {

    public final int stepIndex;
    public final String targetName;
    public final String threadName;
    public final String frameName;
    public final int lineNumber;
    public final long timestampMillis;
    public final IStackFrame frame;
    public final String sourceCode;
    public final String sourceUri;

    public TraceStepRecord(int stepIndex, String targetName, String threadName,
                           String frameName, int lineNumber, long timestampMillis,
                           IStackFrame frame, String sourceCode, String sourceUri) {
        this.stepIndex = stepIndex;
        this.targetName = targetName;
        this.threadName = threadName;
        this.frameName = frameName;
        this.lineNumber = lineNumber;
        this.timestampMillis = timestampMillis;
        this.frame = frame;
        this.sourceCode = sourceCode;
        this.sourceUri = sourceUri;
    }
}
