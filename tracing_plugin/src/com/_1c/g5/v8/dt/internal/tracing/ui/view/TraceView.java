package com._1c.g5.v8.dt.internal.tracing.ui.view;

import com._1c.g5.v8.dt.internal.tracing.ui.TracingUIActivator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.eclipse.emf.common.util.URI;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;

import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.common.ui.editors.TextEditorPositioner;
import com._1c.g5.v8.dt.debug.util.CrossReferenceFinder;
import com._1c.g5.v8.dt.ui.util.OpenHelper;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.ui.part.ViewPart;

public class TraceView extends ViewPart implements IDebugEventSetListener {

    private static final String BUILD_TAG = "20260531-027";
    private static final int MAX_STEPS = 100000;
    private static final int FRAME_POLL_ATTEMPTS = 600;
    private static final int FRAME_POLL_DELAY_MS = 100;
    private static final int STEP_TIMEOUT_MS = 5000;

    private static final String TRACE_COL_STEP  = Messages.TraceView_StepNo;
    private static final String TRACE_COL_TIME  = Messages.TraceView_Time;
    private static final String TRACE_COL_TARGET = Messages.TraceView_Target;
    private static final String TRACE_COL_THREAD = Messages.TraceView_Thread;
    private static final String TRACE_COL_FRAME = Messages.TraceView_Frame;
    private static final String TRACE_COL_LINE  = Messages.TraceView_LineNumber;
    private static final String TRACE_COL_SOURCE = Messages.TraceView_Source;

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
    private int expectedStepSuspend;
    private int currentTargetIndex;
    private final Set<IDebugTarget> stepOverMode = new HashSet<>();
    private final Map<String, String[]> sourceLineCache = new HashMap<>();
    private OpenHelper openHelper;

    private static void log(String msg) {
        TracingUIActivator.getDefault().getLog().log(
            new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID, msg));
    }

    @Override
    public void createPartControl(Composite parent) {
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
                openFrameInEditor(rec);
            }
        });

        // OpenHelper has public constructor OpenHelper(IWorkbenchPage)
        // — no DI needed, we create it on-demand per click in openFrameInEditor
        openHelper = null;

    }

    private static OpenHelper createOpenHelper() {
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
            if (page != null) {
                return new OpenHelper(page);
            }
        } catch (Exception e) {
            log("createOpenHelper failed: " + e.getMessage());
        }
        return null;
    }

    private void createColumns() {
        String[] titles = {
            TRACE_COL_STEP, TRACE_COL_TIME, TRACE_COL_TARGET,
            TRACE_COL_THREAD, TRACE_COL_FRAME, TRACE_COL_LINE,
            TRACE_COL_SOURCE
        };
        int[] widths = { 50, 120, 120, 100, 320, 60, 400 };

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
        tableViewer.getTable().getColumn(6).setAlignment(SWT.LEFT);
    }

    private void createToolbarActions() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        Action clearAction = new Action("Clear") {
            @Override
            public void run() { clearTrace(); }
        };
        clearAction.setToolTipText("Clear trace list");
        clearAction.setImageDescriptor(PlatformUI.getWorkbench()
            .getSharedImages().getImageDescriptor(
                org.eclipse.ui.ISharedImages.IMG_ELCL_REMOVE));
        mgr.add(clearAction);

        mgr.add(new org.eclipse.jface.action.Separator());

        addToolbarButton(mgr, "Экспорт CSV",
            "Экспорт трассировки в CSV",
            TracingUIActivator.getImageDescriptor("icons/export.png"),
            () -> doExport("csv"));
        addToolbarButton(mgr, "Экспорт JSON",
            "Экспорт трассировки в JSON",
            TracingUIActivator.getImageDescriptor("icons/export.png"),
            () -> doExport("jsonl"));
    }

    private static void addToolbarButton(IToolBarManager mgr,
            String text, String tooltip,
            org.eclipse.jface.resource.ImageDescriptor icon,
            Runnable action) {
        mgr.add(new ContributionItem() {
            @Override
            public void fill(ToolBar parent, int index) {
                ToolItem item = new ToolItem(parent, SWT.PUSH, index);
                item.setText(text);
                item.setToolTipText(tooltip);
                if (icon != null) {
                    Image img = icon.createImage();
                    item.setImage(img);
                    item.addDisposeListener(e -> {
                        if (img != null && !img.isDisposed()) img.dispose();
                    });
                }
                item.addListener(SWT.Selection,
                    e -> action.run());
            }
        });
    }

    private synchronized void clearTrace() {
        traceRecords.clear();
        stepCount = 0;
        sourceLineCache.clear();
        Display.getDefault().asyncExec(() -> {
            if (!tableViewer.getTable().isDisposed()) {
                tableViewer.setInput(new TraceStepRecord[0]);
                tableViewer.refresh();
            }
        });
    }

    // ==================== Tracing logic ====================

    public boolean toggleTracing() {
        if (!tracingActive) { startTracing(); return tracingActive; }
        else { stopTracing(false); return false; } // pause, keep targets suspended
    }

    private synchronized void startTracing() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        targets.clear();
        initializedTargets.clear();
        steppedTarget = null;
        pendingSuspends = 0;
        currentTargetIndex = -1;
        suspendedByUs.clear();
        stepOverMode.clear();

        for (IDebugTarget dt : lm.getDebugTargets()) {
            if (!(dt instanceof ISuspendResume)) continue;
            ISuspendResume sr = (ISuspendResume) dt;
            if (!sr.isSuspended() && sr.canSuspend()) {
                targets.add(dt);
                pendingSuspends++;
                asyncSuspendTarget(dt);
            } else if (sr.isSuspended()) {
                boolean hasSuspendedThread = false;
                try {
                    for (IThread t : dt.getThreads()) {
                        if (t.isSuspended()) {
                            hasSuspendedThread = true;
                            break;
                        }
                    }
                } catch (DebugException e) {
                    hasSuspendedThread = true;
                }
                if (hasSuspendedThread) {
                    targets.add(dt);
                    initializedTargets.add(dt);
                } else {
                    targets.add(dt);
                    pendingSuspends++;
                    asyncSuspendTarget(dt);
                }
            }
        }

        if (targets.isEmpty()) {
            log("startTracing: no targets yet, waiting for new targets");
        }

        tracingActive = true;
        DebugPlugin.getDefault().addDebugEventListener(this);

        log("startTracing (build " + BUILD_TAG + "): " + targets.size()
            + " targets, " + pendingSuspends + " pending, "
            + initializedTargets.size() + " already suspended");

        if (pendingSuspends == 0 && !targets.isEmpty()) {
            recordCurrentFrames();
            currentTargetIndex = -1;
            stepNextTarget();
        }
    }

    private synchronized void stopTracing(boolean resumeTargets) {
        if (!tracingActive) return;
        tracingActive = false;
        DebugPlugin.getDefault().removeDebugEventListener(this);

        if (resumeTargets) {
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
            stepOverMode.clear();
        }
        resetToggleState();
        log("stopTracing: " + stepCount + " steps recorded");
    }

    private void resetToggleState() {
        Display display = Display.getDefault();
        if (display == null || display.isDisposed()) return;
        Runnable r = () -> {
            try {
                ICommandService cs = (ICommandService)
                    PlatformUI.getWorkbench().getService(ICommandService.class);
                if (cs != null) {
                    Command cmd = cs.getCommand(
                        "com._1c.g5.v8.dt.tracing.ui.commands.toggleTracing");
                    if (cmd != null) {
                        State st = cmd.getState(RegistryToggleState.STATE_ID);
                        if (st != null) st.setValue(false);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        };
        if (display.getThread() == Thread.currentThread()) {
            r.run();
        } else {
            display.syncExec(r);
        }
    }

    // ==================== Debug event listener ====================

    @Override
    public void handleDebugEvents(DebugEvent[] events) {
        if (!tracingActive) return;
        for (DebugEvent ev : events) {
            if (ev.getKind() == DebugEvent.SUSPEND) {
                handleSuspend(ev);
            } else if (ev.getKind() == DebugEvent.CREATE) {
                handleCreate(ev);
            } else if (ev.getKind() == DebugEvent.TERMINATE) {
                Object src = ev.getSource();
                if (src instanceof IDebugTarget) {
                    IDebugTarget dt = (IDebugTarget) src;
                    if (targets.remove(dt)) {
                        log("target terminated: " + safeTargetName(dt));
                        suspendedByUs.remove(dt);
                        initializedTargets.remove(dt);
                        stepOverMode.remove(dt);
                    }
                    if (targets.isEmpty()) {
                        // ROOT target may terminate just before the real
                        // server/client target appears — check for new ones.
                        checkNewTargets();
                        if (targets.isEmpty() && tracingActive) {
                            log("all targets terminated, but wait for new targets");
                        }
                    }
                }
            }
        }
    }

    private synchronized void handleSuspend(DebugEvent event) {
        if (!tracingActive) return;

        // 1C debugger may create server/client targets without CREATE events —
        // scan for new targets on every SUSPEND.
        checkNewTargets();

        IDebugTarget dt = resolveTarget(event);
        if (dt == null) return;
        if (!targets.contains(dt)) {
            // Auto-discover targets from SUSPEND events (1C may create
            // child server/client targets without CREATE events).
            if (dt instanceof ISuspendResume) {
                targets.add(dt);
                initializedTargets.add(dt);
                String info = safeTargetName(dt);
                try {
                    IThread[] threads = dt.getThreads();
                    info += " threads=" + threads.length;
                    for (IThread t : threads) {
                        info += " [" + safeThreadName(t)
                            + " suspended=" + t.isSuspended()
                            + " canStep=" + (t instanceof IStep)
                            + "]";
                    }
                } catch (DebugException e) {
                    info += " threadErr=" + e.getMessage();
                }
                log("auto-discovered from SUSPEND: " + info);
                recordCurrentFrames();
                stepNextTarget();
                return;
            }
            log("SUSPEND from non-suspendable target: " + safeTargetName(dt));
            return;
        }

        String targetName = safeTargetName(dt);

        // Initialization phase: any target in targets but not yet in
        // initializedTargets must be processed (either its async suspend
        // completed, or it was already suspended before we discovered it
        // via checkNewTargets without a CREATE event).
        if (!initializedTargets.contains(dt)) {
            initializedTargets.add(dt);
            if (pendingSuspends > 0) {
                pendingSuspends--;
            }
            String threadName = safeThreadName(resolveThread(event));
            log("initial suspend from " + targetName + "/" + threadName
                + " pending=" + pendingSuspends);
            if (pendingSuspends == 0) {
                IThread t = resolveThread(event);
                if (t != null && !t.isSuspended()) {
                    for (int i = 0; i < 5; i++) {
                        try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                        if (t.isSuspended()) break;
                    }
                }
                log("all targets initialized, starting round-robin");
                recordFrame(dt);
                stepNextTarget();
            }
            return;
        }

        // Unexpected SUSPEND (breakpoint hit, new BSL thread appeared, or user
        // resume) — record the current position before stepping away.
        if (expectedStepSuspend <= 0) {
            log("unexpected SUSPEND from " + targetName + " — recording frame");
            IThread t = resolveThread(event);
            if (t != null && t.isSuspended()) {
                IStackFrame frame = resolveFrame(t);
                if (frame != null) {
                    String threadName = safeThreadName(t);
                    String frameName = safeFrameName(frame);
                    int line = safeLineNumber(frame);
                    String sourceCode = resolveSourceLine(frame, line);
                    String sourceUri = frame instanceof IBslStackFrame
                        ? String.valueOf(((IBslStackFrame) frame).getSource()) : "";
                    addRecord(targetName, threadName, frameName, line, frame,
                        sourceCode, sourceUri);
                    log("recorded frame at " + targetName + "/" + threadName
                        + " " + frameName + ":" + line + " step=" + stepCount
                        + " src=" + sourceCode);
                }
            }
            stepNextTarget();
            return;
        }
        expectedStepSuspend = 0;

        // Find the suspended thread.  For cross-target jumps the source
        // target differs from steppedTarget — resolveThread handles that.
        IThread thread = resolveThread(event);
        if (thread == null) {
            log("step SUSPEND: no thread found — trying steppedThread");
            thread = steppedThread;
        }
        steppedTarget = null;
        steppedThread = null;

        if (thread == null) {
            log("step SUSPEND with null thread — ignoring");
            return;
        }

        IStackFrame frame = resolveFrame(thread);

        if (frame == null) {
            log("frame null for " + targetName + "/" + safeThreadName(thread)
                + " — step exited BSL, switching to stepOver for this target");
            stepOverMode.add(dt);
            stepNextTarget();
            return;
        }

        String threadName = safeThreadName(thread);
        String frameName = safeFrameName(frame);
        int line = safeLineNumber(frame);
        String sourceCode = resolveSourceLine(frame, line);

        log("step SUSPEND from " + targetName + "/" + threadName
            + " " + frameName + ":" + line + " step=" + stepCount
            + " src=" + sourceCode);

        String sourceUri = frame instanceof IBslStackFrame
            ? String.valueOf(((IBslStackFrame) frame).getSource()) : "";
        addRecord(targetName, threadName, frameName, line, frame, sourceCode, sourceUri);
        checkNewTargets();

        if (stepCount >= MAX_STEPS) {
            log("max steps reached, stopping");
            stopTracing(true);
            return;
        }
        stepNextTarget();
    }

    private void addRecord(String targetName, String threadName,
                           String frameName, int lineNumber,
                           IStackFrame frame, String sourceCode,
                           String sourceUri) {
        TraceStepRecord rec = new TraceStepRecord(
            ++stepCount, targetName, threadName,
            frameName, lineNumber, System.currentTimeMillis(), frame, sourceCode,
            sourceUri);
        traceRecords.add(0, rec);

        Display.getDefault().asyncExec(() -> {
            if (!tableViewer.getTable().isDisposed()) {
                tableViewer.setInput(traceRecords.toArray(new TraceStepRecord[0]));
                tableViewer.refresh();
            }
        });
    }

    private void stepNextTarget() {
        stepNextTargetWithRetry(0);
    }

    private void stepNextTargetWithRetry(int retryDepth) {
        if (!tracingActive || targets.isEmpty()) return;

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
                boolean canStep = t instanceof IStep && ((IStep) t).canStepInto();
                if (suspended && canStep) {
                    boolean useStepOver = stepOverMode.contains(dt);
                    try {
                        steppedTarget = dt;
                        steppedThread = t;
                        expectedStepSuspend = 1;
                        if (useStepOver) {
                            ((IStep) t).stepOver();
                        } else {
                            ((IStep) t).stepInto();
                        }

                        // Non-blocking safety timeout: if SUSPEND doesn't arrive
                        // (BSL-exit, cross-target miss), poll and re-suspend.
                        scheduleStepTimeout(dt, t, useStepOver);
                        return;
                    } catch (DebugException e) {
                        steppedTarget = null;
                        steppedThread = null;
                        expectedStepSuspend = 0;
                        log((useStepOver ? "stepOver" : "stepInto")
                            + " failed: " + e.getMessage());
                    }
                }
            }
            attempts++;
        }

        // No stepable thread found — re-suspend running targets asynchronously.
        // sr.suspend() is an HTTP call to dbgs.exe that can block for 30+ seconds
        // on client targets — must not hold the synchronized monitor.
        List<IDebugTarget> toReSuspend = new ArrayList<>();
        for (IDebugTarget dt : targets) {
            ISuspendResume sr = (ISuspendResume) dt;
            if (!sr.isSuspended() && sr.canSuspend()) {
                toReSuspend.add(dt);
            }
        }
        if (!toReSuspend.isEmpty()) {
            for (IDebugTarget dt : toReSuspend) {
                pendingSuspends++;
                log("re-suspend " + safeTargetName(dt) + " — queued");
                asyncSuspendTarget(dt);
            }
            return;
        }

        // Scan for new targets that may have appeared without a CREATE event.
        // Max one retry to avoid infinite recursion.
        if (retryDepth == 0) {
            checkNewTargets();
            if (!targets.isEmpty()) {
                currentTargetIndex = -1;
                stepNextTargetWithRetry(retryDepth + 1);
                return;
            }
        }
        log("stepNextTarget: no suspendable target — will retry on next SUSPEND/CREATE");
    }

    private void recordCurrentFrames() {
        for (IDebugTarget dt : initializedTargets) {
            recordFrame(dt);
        }
    }

    private void recordFrame(IDebugTarget dt) {
        try {
            for (IThread t : dt.getThreads()) {
                if (!t.isSuspended()) continue;
                IStackFrame frame = resolveFrame(t);
                if (frame == null) continue;
                String targetName = safeTargetName(dt);
                String threadName = safeThreadName(t);
                String frameName = safeFrameName(frame);
                int line = safeLineNumber(frame);
                String sourceCode = resolveSourceLine(frame, line);
                String sourceUri = frame instanceof IBslStackFrame
                    ? String.valueOf(((IBslStackFrame) frame).getSource()) : "";
                addRecord(targetName, threadName, frameName, line, frame,
                    sourceCode, sourceUri);
                log("initial frame: " + targetName + "/" + threadName
                    + " " + frameName + ":" + line + " step=" + stepCount
                    + " src=" + sourceCode);
            }
        } catch (DebugException e) {
            // skip target
        }
    }

    private int stepTimeoutSerial;

    private void scheduleStepTimeout(IDebugTarget dt, IThread t, boolean stepOver) {
        final int serial = ++stepTimeoutSerial;
        Display display = Display.getDefault();
        if (display == null || display.isDisposed()) return;
        display.asyncExec(() -> {
            if (display.isDisposed()) return;
            display.timerExec(STEP_TIMEOUT_MS, () -> {
                synchronized (TraceView.this) {
                    if (!tracingActive || expectedStepSuspend <= 0) return;
                    if (serial != stepTimeoutSerial) return; // stale
                    expectedStepSuspend = 0;
                    log("step timeout — polling all targets");
                    for (IDebugTarget d : targets) {
                        IThread[] threads;
                        try {
                            threads = d.getThreads();
                        } catch (DebugException e) { continue; }
                        for (IThread th : threads) {
                            if (!th.isSuspended()) continue;
                            IStackFrame frame;
                            try {
                                frame = th.getTopStackFrame();
                            } catch (DebugException e) { continue; }
                            if (frame != null) {
                                log("step timeout: found suspended "
                                    + safeTargetName(d) + "/" + safeThreadName(th)
                                    + " " + safeFrameName(frame));
                                String threadName = safeThreadName(th);
                                String frameName = safeFrameName(frame);
                                int line = safeLineNumber(frame);
                                String sourceCode = resolveSourceLine(frame, line);
                                String sourceUri = frame instanceof IBslStackFrame
                                    ? String.valueOf(((IBslStackFrame) frame).getSource()) : "";
                                addRecord(safeTargetName(d), threadName, frameName,
                                    line, frame, sourceCode, sourceUri);
                                checkNewTargets();
                                if (stepCount < MAX_STEPS) stepNextTarget();
                                else stopTracing(true);
                                return;
                            }
                        }
                    }
                    log("step timeout: no suspended target, stepOver");
                    stepOverMode.add(dt);
                    stepNextTarget();
                }
            });
        });
    }

    private void asyncSuspendTarget(IDebugTarget dt) {
        // Caller must have already incremented pendingSuspends and
        // added dt to targets while holding the synchronized monitor.
        // The actual sr.suspend() runs on a daemon thread — it is an HTTP call
        // to dbgs.exe that can block for 30+ seconds on client targets.
        final ISuspendResume sr = (ISuspendResume) dt;
        String name;
        try { name = dt.getName(); } catch (DebugException e) { name = "?"; }
        Thread t = new Thread(() -> {
            try {
                // Race guard: target could have become suspended between the
                // isSuspended() check (caller) and this background thread's
                // execution. If already suspended, no SUSPEND event will fire,
                // so we must handle the counter directly.
                synchronized (TraceView.this) {
                    if (sr.isSuspended()) {
                        pendingSuspends--;
                        initializedTargets.add(dt);
                        log("async suspend cancelled — already suspended ("
                            + safeTargetName(dt) + "), pending=" + pendingSuspends);
                        if (pendingSuspends == 0 && !targets.isEmpty()) {
                            stepNextTarget();
                        }
                        return;
                    }
                }
                sr.suspend();
                synchronized (TraceView.this) {
                    suspendedByUs.add(dt);
                }
            } catch (DebugException e) {
                synchronized (TraceView.this) {
                    pendingSuspends--;
                    targets.remove(dt);
                    log("async suspend FAILED: " + safeTargetName(dt)
                        + " — " + e.getMessage());
                }
            }
        }, "suspend-" + name);
        t.setDaemon(true);
        t.start();
    }

    private void checkNewTargets() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        for (IDebugTarget dt : lm.getDebugTargets()) {
            if (targets.contains(dt)) continue;
            if (dt instanceof ISuspendResume) {
                ISuspendResume sr = (ISuspendResume) dt;
                targets.add(dt);
                String name = safeTargetName(dt);
                if (!sr.isSuspended() && sr.canSuspend()) {
                    pendingSuspends++;
                    log("checkNewTargets: added " + name + " (will async suspend)");
                    asyncSuspendTarget(dt);
                } else if (sr.isSuspended()) {
                    initializedTargets.add(dt);
                    log("checkNewTargets: added " + name + " (already suspended — initializing)");
                    recordFrame(dt);
                    if (pendingSuspends == 0) {
                        stepNextTargetWithRetry(0);
                    }
                } else {
                    log("checkNewTargets: added " + name
                        + " suspended=" + sr.isSuspended());
                }
            }
        }
    }

    private synchronized void handleCreate(DebugEvent event) {
        Object src = event.getSource();
        if (!(src instanceof IDebugTarget)) return;
        IDebugTarget dt = (IDebugTarget) src;
        if (targets.contains(dt)) return;
        if (!(dt instanceof ISuspendResume)) return;
        targets.add(dt);
        ISuspendResume sr = (ISuspendResume) dt;
        String name = safeTargetName(dt);
        if (!sr.isSuspended() && sr.canSuspend()) {
            pendingSuspends++;
            log("handleCreate: " + name + " (will async suspend)");
            asyncSuspendTarget(dt);
        } else if (sr.isSuspended() && tracingActive) {
            initializedTargets.add(dt);
            log("handleCreate: " + name + " (already suspended)");
            recordFrame(dt);
            stepNextTarget();
        } else {
            log("handleCreate: " + name + " ignored"
                + " suspended=" + sr.isSuspended()
                + " canSuspend=" + sr.canSuspend()
                + " active=" + tracingActive);
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

    private IStackFrame resolveFrame(IThread thread) {
        if (thread == null) return null;
        for (int attempt = 0; attempt < FRAME_POLL_ATTEMPTS; attempt++) {
            try {
                IStackFrame f = thread.getTopStackFrame();
                if (f != null) return f;
            } catch (DebugException e) { /* retry */ }
            try {
                IStackFrame[] frames = thread.getStackFrames();
                if (frames != null && frames.length > 0) return frames[0];
            } catch (DebugException e) { /* ignore */ }
            if (!tracingActive) break;
            try { Thread.sleep(FRAME_POLL_DELAY_MS); } catch (InterruptedException e) { break; }
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

    // ==================== Source code line resolution ====================

    private String resolveSourceLine(IStackFrame frame, int lineNumber) {
        if (frame == null || lineNumber <= 0) return "";
        try {
            if (frame instanceof IBslStackFrame) {
                URI sourceUri = ((IBslStackFrame) frame).getSource();
                if (sourceUri != null) {
                    String key = sourceUri.toString();
                    String[] lines = sourceLineCache.get(key);
                    if (lines == null) {
                        lines = readSourceLines(sourceUri);
                        if (lines != null) {
                            sourceLineCache.put(key, lines);
                        }
                    }
                    if (lines != null && lineNumber <= lines.length) {
                        return lines[lineNumber - 1].trim();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    private static IFile resolveFile(URI sourceUri) {
        try {
            java.net.URI uri = new java.net.URI(sourceUri.toString());
            if ("platform".equals(uri.getScheme())) {
                String path = uri.getPath();
                String wsPath = path.startsWith("/resource/")
                    ? path.substring("/resource/".length())
                    : path;
                return ResourcesPlugin.getWorkspace().getRoot().getFile(
                    new org.eclipse.core.runtime.Path(wsPath));
            }
            if ("file".equals(uri.getScheme())) {
                java.nio.file.Path filePath = java.nio.file.Paths.get(uri);
                return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
                    new org.eclipse.core.runtime.Path(filePath.toString()));
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private String[] readSourceLines(URI sourceUri) {
        try {
            IFile file = resolveFile(sourceUri);
            if (file != null && file.exists()) {
                try (InputStream in = file.getContents();
                     BufferedReader br = new BufferedReader(
                         new InputStreamReader(in, "UTF-8"))) {
                    List<String> lines = new ArrayList<>();
                    String l;
                    while ((l = br.readLine()) != null) {
                        lines.add(l);
                    }
                    return lines.toArray(new String[0]);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String[] readSourceLines(IEditorInput editorInput) {
        try {
            IStorage storage = null;
            if (editorInput instanceof IStorageEditorInput) {
                storage = ((IStorageEditorInput) editorInput).getStorage();
            } else if (editorInput instanceof IFileEditorInput) {
                storage = ((IFileEditorInput) editorInput).getFile();
            }
            if (storage == null) {
                return null;
            }
            String key = storage instanceof IFile
                ? ((IFile) storage).getFullPath().toString()
                : storage.getName();
            String[] cached = sourceLineCache.get(key);
            if (cached != null) return cached;
            try (InputStream in = storage.getContents();
                 BufferedReader br = new BufferedReader(
                     new InputStreamReader(in, "UTF-8"))) {
                List<String> lines = new ArrayList<>();
                String l;
                while ((l = br.readLine()) != null) {
                    lines.add(l);
                }
                String[] result = lines.toArray(new String[0]);
                sourceLineCache.put(key, result);
                return result;
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    // ==================== Source display ====================

    private void openFrameInEditor(TraceStepRecord rec) {
        int recLine = rec.lineNumber;
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
            if (page == null) return;

            IFile file = null;
            if (rec.sourceUri != null && !rec.sourceUri.isEmpty()
                && !"null".equals(rec.sourceUri)) {
                try {
                    URI emfUri = URI.createURI(rec.sourceUri);
                    file = resolveFile(emfUri);
                } catch (Exception e) { /* ignore */ }
            }

            if (file != null && file.exists()) {
                OpenHelper oh = new OpenHelper(page);
                IEditorPart editor = oh.openEditor(file, null);
                if (editor != null) {
                    if (recLine > 0) {
                        try {
                            TextEditorPositioner.positionEditor(editor, recLine - 1);
                        } catch (Exception e) {
                            positionToLine(editor, recLine);
                        }
                    }
                    return;
                }

                try {
                    IEditorPart ed2 = IDE.openEditor(page, file, true);
                    if (ed2 != null) {
                        positionToLine(ed2, recLine);
                        return;
                    }
                } catch (Exception e) { /* ignore */ }
            }

            // Fallback: openHelper.getModule() → openEditor(owner, crossRef)
            if (rec.frame instanceof IBslStackFrame) {
                IBslStackFrame bslFrame = (IBslStackFrame) rec.frame;
                try {
                    Module module = bslFrame.getModule();
                    if (module != null) {
                        EObject owner = module.getOwner();
                        if (owner != null) {
                            EReference crossRef;
                            synchronized (owner.eResource().getResourceSet()) {
                                crossRef = CrossReferenceFinder
                                    .findCrossReference(owner, module);
                            }
                            OpenHelper oh = new OpenHelper(page);
                            IEditorPart ed3 = oh.openEditor(owner, crossRef);
                            if (ed3 != null) {
                                if (recLine > 0) {
                                    TextEditorPositioner
                                        .positionEditor(ed3, recLine - 1);
                                }
                                return;
                            }
                        }
                    }
                } catch (Exception e) { /* ignore */ }
            }

            // Last resort: DebugUITools.displaySource
            if (rec.frame != null) {
                ISourceLookupResult result = DebugUITools
                    .lookupSource(rec.frame, null);
                if (result != null) {
                    try {
                        DebugUITools.displaySource(result, page);
                        return;
                    } catch (Exception e1) { /* ignore */ }
                    IEditorInput editorInput = result.getEditorInput();
                    String editorId = result.getEditorId();
                    if (editorInput != null && editorId != null) {
                        IEditorPart editor = page.openEditor(
                            editorInput, editorId);
                        if (editor != null) positionToLine(editor, recLine);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log("openFrameInEditor: " + e.getClass().getName()
                + ": " + e.getMessage());
        }
    }

    private static void positionToLine(IEditorPart editor, int line) {
        if (line <= 0) return;
        if (!(editor instanceof ITextEditor)) return;
        ITextEditor textEditor = (ITextEditor) editor;
        try {
            IDocumentProvider provider = textEditor.getDocumentProvider();
            IDocument doc = provider.getDocument(editor.getEditorInput());
            if (doc != null) {
                int offset = doc.getLineOffset(line - 1);
                textEditor.selectAndReveal(offset, 0);
            }
        } catch (Exception e) { /* ignore */ }
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
            case 6: return r.sourceCode != null ? r.sourceCode : "";
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
            "csv".equals(format) ? "CSV files (*.csv)" : "JSON (*.jsonl)"
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
        w.write("step,time,target,thread,frame,line,source"); w.newLine();
        for (TraceStepRecord r : traceRecords) {
            w.write(String.valueOf(r.stepIndex)); w.write(',');
            w.write(sdf.format(new Date(r.timestampMillis))); w.write(',');
            w.write(csvEscape(r.targetName)); w.write(',');
            w.write(csvEscape(r.threadName)); w.write(',');
            w.write(csvEscape(r.frameName)); w.write(',');
            w.write(r.lineNumber >= 0 ? String.valueOf(r.lineNumber) : ""); w.write(',');
            w.write(csvEscape(r.sourceCode));
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
            sb.append(",\"src\":").append(jsonEscape(r.sourceCode != null ? r.sourceCode : ""));
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
        if (tracingActive) stopTracing(true);
        super.dispose();
    }
}
