package com._1c.g5.v8.dt.internal.tracing.ui.view;

import com._1c.g5.v8.dt.internal.tracing.ui.TracingUIActivator;

import java.io.BufferedWriter;
import org.eclipse.core.resources.IFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStep;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.ui.part.ViewPart;

public class TraceView extends ViewPart implements IDebugEventSetListener {

    private static final String BUILD_TAG = "20260530-006";
    private static final int MAX_STEPS = 100000;
    private static final int MAX_CONSECUTIVE_NULL = 5;

    private static final String TRACE_COL_STEP  = Messages.TraceView_StepNo;
    private static final String TRACE_COL_TIME  = Messages.TraceView_Time;
    private static final String TRACE_COL_TARGET = Messages.TraceView_Target;
    private static final String TRACE_COL_THREAD = Messages.TraceView_Thread;
    private static final String TRACE_COL_FRAME = Messages.TraceView_Frame;
    private static final String TRACE_COL_LINE  = Messages.TraceView_LineNumber;

    public static final String ID = "com._1c.g5.v8.dt.tracing.ui.TraceView";

    private TableViewer tableViewer;

    private boolean tracingActive;
    private final List<TraceStepRecord> traceRecords = new ArrayList<>();
    private final List<IDebugTarget> targets = new ArrayList<>();
    private final Set<IDebugTarget> initializedTargets = new HashSet<>();
    private final Set<IDebugTarget> suspendedByUs = new HashSet<>();
    private IDebugTarget steppedTarget;
    private IThread steppedThread;
    private int stepCount;
    private int pendingSuspends;
    private int currentTargetIndex;
    private final Map<IDebugTarget, Integer> consecutiveNullFrames = new HashMap<>();

    private void log(String msg) {
        TracingUIActivator.getDefault().getLog().log(
            new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID, msg));
    }

    @Override
    public void createPartControl(Composite parent) {
        log("TraceView.createPartControl started (build " + BUILD_TAG + ")");
        parent.setLayout(new FillLayout());

        tableViewer = new TableViewer(parent,
            SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        tableViewer.setContentProvider(new TraceViewContentProvider());
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.getTable().setHeaderVisible(true);

        createColumns();
        createToolbarActions();

        // Reset persisted toggle state — RegistryToggleState survives restarts
        ICommandService cs = (ICommandService)
            getSite().getService(ICommandService.class);
        if (cs != null) {
            Command cmd = cs.getCommand(
                "com._1c.g5.v8.dt.tracing.ui.commands.toggleTracing");
            if (cmd != null) {
                State st = cmd.getState(RegistryToggleState.STATE_ID);
                if (st != null && Boolean.TRUE.equals(st.getValue())) {
                    st.setValue(false);
                }
            }
        }

        tableViewer.addDoubleClickListener(event -> {
            IStructuredSelection sel = (IStructuredSelection) event.getSelection();
            TraceStepRecord rec = (TraceStepRecord) sel.getFirstElement();
            if (rec != null && rec.frame != null) {
                openFrameInEditor(rec.frame);
            }
        });

        log("TraceView.createPartControl finished");
    }

    private void createColumns() {
        String[] titles = {
            TRACE_COL_STEP, TRACE_COL_TIME, TRACE_COL_TARGET,
            TRACE_COL_THREAD, TRACE_COL_FRAME, TRACE_COL_LINE
        };
        int[] widths = { 50, 120, 120, 100, 320, 60 };

        for (int i = 0; i < titles.length; i++) {
            final int colIndex = i;
            TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.NONE);
            tvc.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return columnText((TraceStepRecord) element, colIndex);
                }
            });
            TableColumn col = tvc.getColumn();
            col.setText(titles[i]);
            col.setWidth(widths[i]);
            col.setResizable(true);
            col.setMoveable(true);
        }
        tableViewer.getTable().getColumn(0).setAlignment(SWT.RIGHT);
        tableViewer.getTable().getColumn(5).setAlignment(SWT.RIGHT);
    }

    private void createToolbarActions() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        Action exportCsv = new Action("Export CSV") {
            @Override
            public void run() { doExport("csv"); }
        };
        exportCsv.setToolTipText("Export trace as CSV");
        mgr.add(exportCsv);

        Action exportJsonl = new Action("Export JSONL") {
            @Override
            public void run() { doExport("jsonl"); }
        };
        exportJsonl.setToolTipText("Export trace as JSON Lines");
        mgr.add(exportJsonl);
    }

    // ==================== Tracing logic ====================

    public boolean toggleTracing() {
        if (!tracingActive) { startTracing(); return tracingActive; }
        else { stopTracing(); return false; }
    }

    private synchronized void startTracing() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        targets.clear();
        initializedTargets.clear();
        traceRecords.clear();
        steppedTarget = null;
        stepCount = 0;
        pendingSuspends = 0;
        currentTargetIndex = -1;
        suspendedByUs.clear();
        consecutiveNullFrames.clear();

        for (IDebugTarget dt : lm.getDebugTargets()) {
            if (!(dt instanceof ISuspendResume)) continue;
            ISuspendResume sr = (ISuspendResume) dt;
            if (!sr.isSuspended() && sr.canSuspend()) {
                targets.add(dt);
                pendingSuspends++;
                try {
                    sr.suspend();
                    suspendedByUs.add(dt);
                } catch (DebugException e) {
                    log("suspend failed: " + safeTargetName(dt) + " — " + e.getMessage());
                    targets.remove(dt);
                    pendingSuspends--;
                }
            } else if (sr.isSuspended()) {
                // Check if at least one thread is actually suspended;
                // target.isSuspended() may return true even when no BSL thread
                // is stopped (EDT quirk).
                boolean hasSuspendedThread = false;
                try {
                    for (IThread t : dt.getThreads()) {
                        if (t.isSuspended()) {
                            hasSuspendedThread = true;
                            break;
                        }
                    }
                } catch (DebugException e) {
                    hasSuspendedThread = true; // assume yes on error
                }
                if (hasSuspendedThread) {
                    targets.add(dt);
                    initializedTargets.add(dt);
                } else {
                    // Force a real suspend — the target is "suspended" but no
                    // thread stopped, which would block stepping.
                    targets.add(dt);
                    pendingSuspends++;
                    try {
                        sr.suspend();
                        suspendedByUs.add(dt);
                    } catch (DebugException e) {
                        log("suspend failed (stale): " + safeTargetName(dt)
                            + " — " + e.getMessage());
                        targets.remove(dt);
                        pendingSuspends--;
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            log("startTracing: no suspendable targets");
            return;
        }

        tracingActive = true;
        DebugPlugin.getDefault().addDebugEventListener(this);

        log("startTracing: " + targets.size() + " targets, "
            + pendingSuspends + " pending, "
            + initializedTargets.size() + " already suspended");

        if (pendingSuspends == 0) {
            currentTargetIndex = -1;
            stepNextTarget();
        }
    }

    private synchronized void stopTracing() {
        if (!tracingActive) return;
        tracingActive = false;
        DebugPlugin.getDefault().removeDebugEventListener(this);

        // Only resume targets that WE suspended (leave pre-suspended ones alone)
        for (IDebugTarget dt : suspendedByUs) {
            ISuspendResume sr = (ISuspendResume) dt;
            if (sr.isSuspended() && sr.canResume()) {
                try {
                    sr.resume();
                } catch (DebugException e) {
                    log("resume failed: " + safeTargetName(dt) + " — " + e.getMessage());
                }
            }
        }
        targets.clear();
        suspendedByUs.clear();
        consecutiveNullFrames.clear();
        log("stopTracing: " + stepCount + " steps recorded");
    }

    // ==================== Debug event listener ====================

    @Override
    public void handleDebugEvents(DebugEvent[] events) {
        if (!tracingActive) return;
        for (DebugEvent ev : events) {
            if (ev.getKind() == DebugEvent.SUSPEND) {
                handleSuspend(ev);
            }
        }
    }

    private synchronized void handleSuspend(DebugEvent event) {
        if (!tracingActive) return;

        IDebugTarget dt = resolveTarget(event);
        if (dt == null || !targets.contains(dt)) return;

        String targetName = safeTargetName(dt);

        // Initialization phase: process pending suspend events
        if (pendingSuspends > 0) {
            pendingSuspends--;
            initializedTargets.add(dt);
            String threadName = safeThreadName(resolveThread(event));
            log("initial suspend from " + targetName + "/" + threadName
                + " pending=" + pendingSuspends);
            if (pendingSuspends == 0) {
                // Give EDT time to propagate target-level suspend to thread level
                IThread t = resolveThread(event);
                if (t != null && !t.isSuspended()) {
                    for (int i = 0; i < 5; i++) {
                        try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                        if (t.isSuspended()) break;
                    }
                }
                log("all targets initialized, starting round-robin");
                stepNextTarget();
            }
            return;
        }

        // Ignore stale events from already-initialized targets (e.g. old breakpoint events)
        if (steppedTarget != null && dt != steppedTarget) {
            log("stale SUSPEND from " + targetName + ", waiting for "
                + safeTargetName(steppedTarget));
            return;
        }

        // Use the thread we actually stepped, not the event source
        // (EDT SUSPEND events come with source=IDebugTarget, not IThread)
        IThread thread = steppedThread;
        steppedTarget = null;
        steppedThread = null;

        if (thread == null) {
            log("step SUSPEND with no steppedThread — ignoring");
            return;
        }

        IStackFrame frame = resolveFrame(thread);

        if (frame == null) {
            int nullCount = consecutiveNullFrames.merge(dt, 1, Integer::sum);
            log("frame null for " + targetName + "/" + safeThreadName(thread)
                + " — step exited BSL (null#" + nullCount + " of "
                + MAX_CONSECUTIVE_NULL + ")");
            if (nullCount >= MAX_CONSECUTIVE_NULL) {
                log("too many consecutive null frames for " + targetName
                    + " — removing target");
                targets.remove(dt);
                consecutiveNullFrames.remove(dt);
                checkNewTargets();
                if (targets.isEmpty()) {
                    log("no targets left — stopping");
                    stopTracing();
                    return;
                }
                stepNextTarget();
                return;
            }
            // Target is no longer suspended after a null-frame step.
            // Don't re-suspend here — let stepNextTarget() handle re-suspends
            // for ALL non-suspended targets via its fallback loop.
            checkNewTargets();
            stepNextTarget();
            return;
        }
        // Successful step — reset consecutive null counter for this target
        consecutiveNullFrames.remove(dt);

        String threadName = safeThreadName(thread);
        String frameName = safeFrameName(frame);
        int line = safeLineNumber(frame);

        log("step SUSPEND from " + targetName + "/" + threadName
            + " " + frameName + ":" + line + " step=" + stepCount);

        addRecord(targetName, threadName, frameName, line, frame);
        checkNewTargets();

        if (stepCount >= MAX_STEPS) {
            log("max steps reached, stopping");
            stopTracing();
            return;
        }
        stepNextTarget();
    }

    private void addRecord(String targetName, String threadName,
                           String frameName, int lineNumber,
                           IStackFrame frame) {
        TraceStepRecord rec = new TraceStepRecord(
            ++stepCount, targetName, threadName,
            frameName, lineNumber, System.currentTimeMillis(), frame);
        traceRecords.add(rec);

        Display.getDefault().asyncExec(() -> {
            if (!tableViewer.getTable().isDisposed()) {
                tableViewer.setInput(traceRecords.toArray(new TraceStepRecord[0]));
                tableViewer.refresh();
            }
        });
    }

    private void stepNextTarget() {
        if (!tracingActive || targets.isEmpty()) return;

        log("stepNextTarget: start search, index=" + currentTargetIndex
            + " count=" + targets.size());

        int attempts = 0;
        while (attempts < targets.size()) {
            currentTargetIndex = (currentTargetIndex + 1) % targets.size();
            IDebugTarget dt = targets.get(currentTargetIndex);
            IThread[] threads;
            try {
                threads = dt.getThreads();
            } catch (DebugException e) {
                attempts++;
                continue;
            }
            for (IThread t : threads) {
                boolean suspended = t.isSuspended();
                boolean canStepOver = t instanceof IStep
                    && ((IStep) t).canStepOver();
                log("stepNextTarget: " + safeTargetName(dt) + "/"
                    + safeThreadName(t) + " suspended=" + suspended
                    + " canStepOver=" + canStepOver);
                if (suspended && canStepOver) {
                    try {
                        steppedTarget = dt;
                        steppedThread = t;
                        ((IStep) t).stepOver();
                        log("stepNextTarget: stepOver called on "
                            + safeTargetName(dt) + "/" + safeThreadName(t));
                        return;
                    } catch (DebugException e) {
                        steppedTarget = null;
                        steppedThread = null;
                        log("stepOver failed: " + e.getMessage());
                    }
                }
            }
            attempts++;
        }

        // No stepable thread found — instead of stopping, try to re-suspend
        // every target that is not suspended.  Their SUSPEND events will
        // re-enter handleSuspend → stepNextTarget.
        boolean anyReSuspended = false;
        for (IDebugTarget dt : targets) {
            ISuspendResume sr = (ISuspendResume) dt;
            if (!sr.isSuspended() && sr.canSuspend()) {
                pendingSuspends++;
                anyReSuspended = true;
                try {
                    sr.suspend();
                    log("stepNextTarget: re-suspended " + safeTargetName(dt));
                } catch (DebugException e) {
                    pendingSuspends--;
                    log("stepNextTarget: re-suspend failed "
                        + safeTargetName(dt) + " — " + e.getMessage());
                }
            }
        }
        if (anyReSuspended) return; // wait for new SUSPEND events

        log("stepNextTarget: no suspendable target — stopping");
        stopTracing();
    }

    private void checkNewTargets() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        for (IDebugTarget dt : lm.getDebugTargets()) {
            if (targets.contains(dt)) continue;
            if (dt instanceof ISuspendResume) {
                ISuspendResume sr = (ISuspendResume) dt;
                targets.add(dt);
                if (!sr.isSuspended() && sr.canSuspend()) {
                    pendingSuspends++;
                    try {
                        sr.suspend();
                        suspendedByUs.add(dt);
                    } catch (DebugException e) {
                        log("suspend new target failed: " + e.getMessage());
                        pendingSuspends--;
                    }
                }
                log("checkNewTargets: added " + safeTargetName(dt));
            }
        }
    }

    // ==================== Helpers ====================

    private static IDebugTarget resolveTarget(DebugEvent event) {
        Object src = event.getSource();
        if (src instanceof IDebugTarget) return (IDebugTarget) src;
        if (src instanceof IThread) return ((IThread) src).getDebugTarget();
        if (src instanceof IStackFrame)
            return ((IStackFrame) src).getThread().getDebugTarget();
        return null;
    }

    private static IThread resolveThread(DebugEvent event) {
        Object src = event.getSource();
        if (src instanceof IThread) return (IThread) src;
        if (src instanceof IStackFrame) return ((IStackFrame) src).getThread();
        if (src instanceof IDebugTarget) {
            try {
                IThread[] ts = ((IDebugTarget) src).getThreads();
                return ts.length > 0 ? ts[0] : null;
            } catch (DebugException e) {
                return null;
            }
        }
        return null;
    }

    private static IStackFrame resolveFrame(IThread thread) {
        if (thread == null) return null;
        // Give EDT time to populate the frame after SUSPEND
        try { Thread.sleep(50); } catch (InterruptedException e) { return null; }
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                IStackFrame f = thread.getTopStackFrame();
                if (f != null) return f;
            } catch (DebugException e) { /* retry */ }
            // Fallback: getStackFrames() works in EDT when getTopStackFrame() returns null
            try {
                IStackFrame[] frames = thread.getStackFrames();
                if (frames != null && frames.length > 0) return frames[0];
            } catch (DebugException e) { /* ignore */ }
            // Early exit: thread is no longer suspended — frame won't come
            if (!thread.isSuspended()) break;
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }
        return null;
    }

    // ==================== Safe accessors (wrap DebugException) ====================

    private static String safeTargetName(IDebugTarget dt) {
        if (dt == null) return "?";
        try { return dt.getName(); }
        catch (DebugException e) { return "?"; }
    }

    private static String safeThreadName(IThread thread) {
        if (thread == null) return "?";
        try { return thread.getName(); }
        catch (DebugException e) { return "?"; }
    }

    private static String safeFrameName(IStackFrame frame) {
        if (frame == null) return "?";
        try { return frame.getName(); }
        catch (DebugException e) { return "?"; }
    }

    private static int safeLineNumber(IStackFrame frame) {
        if (frame == null) return -1;
        try { return frame.getLineNumber(); }
        catch (DebugException e) { return -1; }
    }

    // ==================== Source lookup ====================

    private void openFrameInEditor(IStackFrame frame) {
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
            if (page == null) return;

            // 1. Try source locator from the launch
            Object sourceElement = null;
            org.eclipse.debug.core.model.ISourceLocator sl =
                frame.getLaunch().getSourceLocator();
            if (sl != null)
                sourceElement = sl.getSourceElement(frame);
            if (sourceElement instanceof IFile) {
                IDE.openEditor(page, (IFile) sourceElement, true);
                return;
            }

            // 2. Try frame.getAdapter(IFile.class)
            IFile adapted = frame.getAdapter(IFile.class);
            if (adapted != null) {
                IDE.openEditor(page, adapted, true);
                return;
            }

            // 3. Parse module name from frame name ("ModuleName.MethodName")
            String fname = safeFrameName(frame);
            int dot = fname.lastIndexOf('.');
            if (dot > 0) {
                String module = fname.substring(0, dot);
                // EDT modules are typically under src/ with .bsl/.Form/Module.bsl
                IFile[] all = ResourcesPlugin.getWorkspace().getRoot()
                    .findFilesForLocationURI(java.net.URI.create(
                        "file:///" + module.replace('.', '/')));
                // Too broad — log and let user know
                log("try opening module: " + module);
            }

            log("no source for " + fname
                + " (sourceElement="
                + (sourceElement != null ? sourceElement.getClass().getName() : "null")
                + ")");
        } catch (Exception e) {
            TracingUIActivator.getDefault().getLog().log(
                new Status(IStatus.ERROR, TracingUIActivator.PLUGIN_ID,
                    "open editor failed", e));
        }
    }

    // ==================== Column text ====================

    private static String columnText(TraceStepRecord r, int col) {
        switch (col) {
            case 0: return String.valueOf(r.stepIndex);
            case 1: return formatTime(r.timestampMillis);
            case 2: return r.targetName;
            case 3: return r.threadName;
            case 4: return r.frameName;
            case 5: return r.lineNumber >= 0 ? String.valueOf(r.lineNumber) : "";
            default: return "";
        }
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(millis));
    }

    // ==================== Export ====================

    private void doExport(String format) {
        if (traceRecords.isEmpty()) return;

        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        dialog.setFilterNames(new String[]{
            "csv".equals(format) ? "CSV files (*.csv)" : "JSON Lines (*.jsonl)"
        });
        dialog.setFilterExtensions(new String[]{
            "csv".equals(format) ? "*.csv" : "*.jsonl"
        });
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        dialog.setFileName("trace_" + ts + "." + format);
        dialog.setOverwrite(true);

        String path = dialog.open();
        if (path == null) return;

        try {
            BufferedWriter w = Files.newBufferedWriter(Paths.get(path),
                StandardCharsets.UTF_8);
            if ("csv".equals(format)) writeCsv(w);
            else writeJsonl(w);
            w.close();
        } catch (Exception e) {
            TracingUIActivator.getDefault().getLog().log(
                new Status(IStatus.ERROR, TracingUIActivator.PLUGIN_ID,
                    "Export failed", e));
        }
    }

    private void writeCsv(BufferedWriter w) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        w.write("step,time,target,thread,frame,line"); w.newLine();
        for (TraceStepRecord r : traceRecords) {
            w.write(String.valueOf(r.stepIndex)); w.write(',');
            w.write(sdf.format(new Date(r.timestampMillis))); w.write(',');
            w.write(csvEscape(r.targetName)); w.write(',');
            w.write(csvEscape(r.threadName)); w.write(',');
            w.write(csvEscape(r.frameName)); w.write(',');
            w.write(r.lineNumber >= 0 ? String.valueOf(r.lineNumber) : "");
            w.newLine();
        }
    }

    private void writeJsonl(BufferedWriter w) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        for (TraceStepRecord r : traceRecords) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"#\":").append(r.stepIndex);
            sb.append(",\"tgt\":").append(jsonEscape(r.targetName));
            sb.append(",\"thr\":").append(jsonEscape(r.threadName));
            sb.append(",\"frm\":").append(jsonEscape(r.frameName));
            sb.append(",\"l\":").append(r.lineNumber >= 0 ? r.lineNumber : "null");
            sb.append(",\"ts\":\"").append(sdf.format(new Date(r.timestampMillis))).append('"');
            sb.append('}');
            w.write(sb.toString()); w.newLine();
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

    @Override
    public void setFocus() {
        tableViewer.getControl().setFocus();
    }

    @Override
    public synchronized void dispose() {
        if (tracingActive) stopTracing();
        super.dispose();
    }
}
