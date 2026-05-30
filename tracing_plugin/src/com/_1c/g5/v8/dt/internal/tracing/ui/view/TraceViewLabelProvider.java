package com._1c.g5.v8.dt.internal.tracing.ui.view;

import com._1c.g5.v8.dt.profiling.core.ILineProfilingResult;
import com._1c.g5.v8.dt.profiling.core.ServerCallSignal;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class TraceViewLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        if (!(element instanceof ILineProfilingResult)) {
            return "";
        }
        ILineProfilingResult r = (ILineProfilingResult) element;
        switch (columnIndex) {
            case 0:  return r.getModuleName();
            case 1:  return r.getMethodSignature();
            case 2:  return String.valueOf(r.getLineNo());
            case 3:  return r.getLine();
            case 4:  return String.valueOf(r.getFrequency());
            case 5:  return formatDurationMs(r);
            case 6:  return formatPureDurationMs(r);
            case 7:  return String.format("%.1f", r.getPercentage());
            case 8:  return r.getDebugTargetType().toString();
            case 9:  return formatServerCall(r.getServerCallSignal());
            default: return "";
        }
    }

    private static String formatDurationMs(ILineProfilingResult r) {
        long totalMicros = r.getTimeForDebugTarget(r.getDebugTargetType()).toNanos() / 1000;
        long ms = totalMicros / 1000;
        long micros = totalMicros % 1000;
        return String.format("%d.%03d", ms, micros);
    }

    private static String formatPureDurationMs(ILineProfilingResult r) {
        long totalMicros = r.getPureTimeForDebugTarget(r.getDebugTargetType()).toNanos() / 1000;
        long ms = totalMicros / 1000;
        long micros = totalMicros % 1000;
        return String.format("%d.%03d", ms, micros);
    }

    private static String formatServerCall(ServerCallSignal signal) {
        if (signal == null) return "";
        switch (signal) {
            case NO_SERVER_CALL:       return "";
            case SERVER_CALL:          return "SVR";
            case INTERNAL_SERVER_CALL: return "INT";
            default:                   return signal.toString();
        }
    }
}
