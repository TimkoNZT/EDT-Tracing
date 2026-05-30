package com._1c.g5.v8.dt.internal.tracing.ui.view;

import com._1c.g5.v8.dt.internal.tracing.ui.TracingUIActivator;
import com._1c.g5.v8.dt.profiling.core.ILineProfilingResult;
import com._1c.g5.v8.dt.profiling.core.IProfilingResult;
import com._1c.g5.v8.dt.profiling.core.IProfilingResultListener;
import com._1c.g5.v8.dt.profiling.core.IProfilingService;
import com._1c.g5.v8.dt.profiling.core.ServerCallSignal;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.ui.progress.UIJob;

import org.eclipse.ui.part.ViewPart;

public class TraceView extends ViewPart {

    private static final String TRACE_COL_MODULE = Messages.TraceView_Module;
    private static final String TRACE_COL_METHOD = Messages.TraceView_Method;
    private static final String TRACE_COL_LINE_NUM = Messages.TraceView_LineNumber;
    private static final String TRACE_COL_LINE = Messages.TraceView_Line;
    private static final String TRACE_COL_CALLS = Messages.TraceView_Calls;
    private static final String TRACE_COL_DURATION = Messages.TraceView_Duration;
    private static final String TRACE_COL_PURE_TIME = Messages.TraceView_PureTime;
    private static final String TRACE_COL_PERCENT = Messages.TraceView_Percent;
    private static final String TRACE_COL_TARGET = Messages.TraceView_Target;
    private static final String TRACE_COL_SERVER_CALL = Messages.TraceView_ServerCall;

    public static final String ID = "com._1c.g5.v8.dt.tracing.ui.TraceView";

    private TableViewer tableViewer;
    private IProfilingService service;
    private IProfilingResult currentResult;

    private final IProfilingResultListener listener = new IProfilingResultListener() {
        @Override
        public void resultsUpdated(IProfilingResult result) {
            new UIJob("Refresh trace") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    currentResult = result;
                    loadResult(result);
                    return Status.OK_STATUS;
                }
            }.schedule();
        }

        @Override
        public void resultsCleared() {
            new UIJob("Clear trace") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    currentResult = null;
                    tableViewer.setInput(null);
                    return Status.OK_STATUS;
                }
            }.schedule();
        }

        @Override
        public void resultRenamed(IProfilingResult result, String newName) {
        }
    };

    @Override
    public void createPartControl(Composite parent) {
        TracingUIActivator.getDefault().getLog().log(
            new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID, "TraceView.createPartControl started"));
        parent.setLayout(new FillLayout());

        tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        tableViewer.setContentProvider(new TraceViewContentProvider());
        tableViewer.setLabelProvider(new TraceViewLabelProvider());
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.getTable().setHeaderVisible(true);

        createColumns();
        createToolbarActions();

        service = TracingUIActivator.getProfilingService();
        if (service != null) {
            TracingUIActivator.getDefault().getLog().log(
                new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID, "TraceView: service OK, registering listener"));
            service.addProfilingResultsListener(listener);
            List<IProfilingResult> results = service.getResults();
            if (results != null && !results.isEmpty()) {
                currentResult = results.get(results.size() - 1);
                loadResult(currentResult);
            }
        } else {
            TracingUIActivator.getDefault().getLog().log(
                new Status(IStatus.WARNING, TracingUIActivator.PLUGIN_ID, "TraceView: IProfilingService not available"));
        }
        TracingUIActivator.getDefault().getLog().log(
            new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID, "TraceView.createPartControl finished"));
    }

    private void createColumns() {
        String[] titles = {
            TRACE_COL_MODULE, TRACE_COL_METHOD, TRACE_COL_LINE_NUM, TRACE_COL_LINE,
            TRACE_COL_CALLS, TRACE_COL_DURATION, TRACE_COL_PURE_TIME, TRACE_COL_PERCENT,
            TRACE_COL_TARGET, TRACE_COL_SERVER_CALL
        };
        int[] widths = { 150, 200, 60, 200, 60, 90, 80, 50, 80, 80 };

        for (int i = 0; i < titles.length; i++) {
            TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.NONE);
            TableColumn col = tvc.getColumn();
            col.setText(titles[i]);
            col.setWidth(widths[i]);
            col.setResizable(true);
            col.setMoveable(true);
        }

        tableViewer.getTable().getColumn(2).setAlignment(SWT.RIGHT);
        tableViewer.getTable().getColumn(4).setAlignment(SWT.RIGHT);
        tableViewer.getTable().getColumn(5).setAlignment(SWT.RIGHT);
        tableViewer.getTable().getColumn(6).setAlignment(SWT.RIGHT);
        tableViewer.getTable().getColumn(7).setAlignment(SWT.RIGHT);
    }

    private void createToolbarActions() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        Action exportCsv = new Action("Export CSV") {
            @Override
            public void run() {
                doExport("csv");
            }
        };
        exportCsv.setToolTipText("Export trace as compact CSV");
        mgr.add(exportCsv);

        Action exportJsonl = new Action("Export JSONL") {
            @Override
            public void run() {
                doExport("jsonl");
            }
        };
        exportJsonl.setToolTipText("Export trace as JSON Lines (AI-friendly)");
        mgr.add(exportJsonl);
    }

    private void loadResult(IProfilingResult result) {
        if (result == null) {
            tableViewer.setInput(null);
            return;
        }
        List<ILineProfilingResult> data = result.getProfilingResults();
        tableViewer.setInput(data);
    }

    private void doExport(String format) {
        List<?> input = (List<?>) tableViewer.getInput();
        if (input == null || input.isEmpty()) return;

        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        dialog.setFilterNames(new String[]{
            format.equals("csv") ? "CSV files (*.csv)" : "JSON Lines (*.jsonl)"
        });
        dialog.setFilterExtensions(new String[]{
            format.equals("csv") ? "*.csv" : "*.jsonl"
        });
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        dialog.setFileName("trace_" + ts + "." + format);
        dialog.setOverwrite(true);

        String path = dialog.open();
        if (path == null) return;

        try {
            BufferedWriter w = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8);
            if ("csv".equals(format)) {
                writeCsv(w, input);
            } else {
                writeJsonl(w, input);
            }
            w.close();
        } catch (Exception e) {
            TracingUIActivator.getDefault().getLog().log(
                new Status(IStatus.ERROR, TracingUIActivator.PLUGIN_ID,
                    "Export failed", e));
        }
    }

    @SuppressWarnings("unchecked")
    private void writeCsv(BufferedWriter w, List<?> input) throws Exception {
        w.write("mod,method,line,calls,dur_ms,pure_ms,pct,target,svr");
        w.newLine();

        for (Object obj : input) {
            ILineProfilingResult r = (ILineProfilingResult) obj;
            w.write(csvEscape(r.getModuleName()));
            w.write(',');
            w.write(csvEscape(r.getMethodSignature()));
            w.write(',');
            w.write(String.valueOf(r.getLineNo()));
            w.write(',');
            w.write(String.valueOf(r.getFrequency()));
            w.write(',');
            w.write(formatMs(r));
            w.write(',');
            w.write(formatPureMs(r));
            w.write(',');
            w.write(String.format("%.1f", r.getPercentage()));
            w.write(',');
            w.write(r.getDebugTargetType().toString());
            w.write(',');
            w.write(serverCallStr(r));
            w.newLine();
        }
    }

    private void writeJsonl(BufferedWriter w, List<?> input) throws Exception {
        for (Object obj : input) {
            ILineProfilingResult r = (ILineProfilingResult) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{\"m\":");
            sb.append(jsonEscape(r.getModuleName()));
            sb.append(",\"f\":");
            sb.append(jsonEscape(r.getMethodSignature()));
            sb.append(",\"l\":").append(r.getLineNo());
            sb.append(",\"n\":").append(r.getFrequency());
            sb.append(",\"d\":").append(formatMs(r));
            sb.append(",\"p\":").append(String.format("%.1f", r.getPercentage()));
            sb.append(",\"t\":");
            sb.append(jsonEscape(r.getDebugTargetType().toString()));
            sb.append('}');
            w.write(sb.toString());
            w.newLine();
        }
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String formatMs(ILineProfilingResult r) {
        long totalMicros = r.getTimeForDebugTarget(r.getDebugTargetType()).toNanos() / 1000;
        long ms = totalMicros / 1000;
        long micros = totalMicros % 1000;
        return String.format("%d.%03d", ms, micros);
    }

    private static String formatPureMs(ILineProfilingResult r) {
        long totalMicros = r.getPureTimeForDebugTarget(r.getDebugTargetType()).toNanos() / 1000;
        long ms = totalMicros / 1000;
        long micros = totalMicros % 1000;
        return String.format("%d.%03d", ms, micros);
    }

    private static String serverCallStr(ILineProfilingResult r) {
        if (r.getServerCallSignal() == null) return "";
        switch (r.getServerCallSignal()) {
            case NO_SERVER_CALL:       return "";
            case SERVER_CALL:          return "SVR";
            case INTERNAL_SERVER_CALL: return "INT";
            default:                   return r.getServerCallSignal().toString();
        }
    }

    @Override
    public void setFocus() {
        tableViewer.getControl().setFocus();
    }

    @Override
    public void dispose() {
        if (service != null) {
            service.removeProfilingResultsListener(listener);
        }
        super.dispose();
    }
}
