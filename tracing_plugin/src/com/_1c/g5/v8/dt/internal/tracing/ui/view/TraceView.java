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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.jface.action.Action;
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

    private static final String BUILD_TAG = "20260601-030";
    private static final String PREF_COL_WIDTH_PREFIX = "colWidth_";
    private static final int[] DEFAULT_COL_WIDTHS = { 50, 120, 120, 100, 320, 60, 400 };
    private static final int MAX_STEPS = 100000;
    private static final int POLL_INTERVAL_MS = 100;

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
    private final Set<IDebugTarget> suspendedByUs = new HashSet<>();
    private int stepCount;
    private int currentTargetIndex;
    private final Set<IDebugTarget> stepOverMode = new HashSet<>();
    private final Map<String, String[]> sourceLineCache = new HashMap<>();

    // key = dt.toString() + "|" + thread.toString() → "frameName:line"
    private final Map<String, String> lastPositions = new HashMap<>();

    private Thread tracingThread;
    private final Object lock = new Object();
    private volatile boolean steppingInProgress;

    private volatile List<ModuleFilterEntry> moduleFilters = new ArrayList<>();

    private static void log(String msg) {
        TracingUIActivator.getDefault().getLog().log(
            new Status(IStatus.INFO, TracingUIActivator.PLUGIN_ID, msg));
    }

    @Override
    public void createPartControl(Composite parent) {
        log("createPartControl (build " + BUILD_TAG + ")");
        moduleFilters = ModuleFilterDialog.loadFromPrefs();
        parent.setLayout(new FillLayout());

        tableViewer = new TableViewer(parent,
            SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        tableViewer.setContentProvider(new TraceViewContentProvider());
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.getTable().setHeaderVisible(true);

        createColumns();
        createToolbarActions();

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
    }

    private void createColumns() {
        String[] titles = {
            TRACE_COL_STEP, TRACE_COL_TIME, TRACE_COL_TARGET,
            TRACE_COL_THREAD, TRACE_COL_FRAME, TRACE_COL_LINE,
            TRACE_COL_SOURCE
        };
        IPreferenceStore prefs = TracingUIActivator.getDefault().getPreferenceStore();

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
            int w = prefs.getInt(PREF_COL_WIDTH_PREFIX + i);
            if (w <= 0) w = DEFAULT_COL_WIDTHS[i];
            col.setWidth(w);
            col.setResizable(true);
            col.setMoveable(true);
            final int ci = i;
            col.addControlListener(new org.eclipse.swt.events.ControlAdapter() {
                @Override
                public void controlResized(org.eclipse.swt.events.ControlEvent e) {
                    TableColumn c = (TableColumn) e.widget;
                    prefs.setValue(PREF_COL_WIDTH_PREFIX + ci, c.getWidth());
                }
            });
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
        clearAction.setToolTipText("Очистить список");
        clearAction.setImageDescriptor(PlatformUI.getWorkbench()
            .getSharedImages().getImageDescriptor(
                org.eclipse.ui.ISharedImages.IMG_ELCL_REMOVE));
        mgr.add(clearAction);

        Action filterAction = new Action("Filters") {
            @Override
            public void run() { openFilterDialog(); }
        };
        filterAction.setToolTipText("Фильтры исключения модулей");
        filterAction.setImageDescriptor(
            TracingUIActivator.getImageDescriptor("icons/filter.png"));
        mgr.add(filterAction);

        mgr.add(new org.eclipse.jface.action.Separator());

        Action exportCsv = new Action("CSV") {
            @Override
            public void run() { doExport("csv"); }
        };
        exportCsv.setToolTipText("Экспорт трассировки в CSV");
        mgr.add(exportCsv);

        Action exportJson = new Action("JSON") {
            @Override
            public void run() { doExport("jsonl"); }
        };
        exportJson.setToolTipText("Экспорт трассировки в JSON");
        mgr.add(exportJson);
    }

    private void clearTrace() {
        traceRecords.clear();
        stepCount = 0;
        sourceLineCache.clear();
        lastPositions.clear();
        Display.getDefault().asyncExec(() -> {
            if (!tableViewer.getTable().isDisposed()) {
                tableViewer.setInput(new TraceStepRecord[0]);
                tableViewer.refresh();
            }
        });
    }

    private void openFilterDialog() {
        ModuleFilterDialog dlg = new ModuleFilterDialog(
            getSite().getShell(), moduleFilters);
        if (dlg.open() == org.eclipse.jface.dialogs.IDialogConstants.OK_ID) {
            moduleFilters = dlg.getFilters();
            ModuleFilterDialog.saveToPrefs(moduleFilters);
            log("module filters updated: " + moduleFilters.size() + " entries");
        }
    }

    // ==================== Tracing logic ====================

    public boolean toggleTracing() {
        if (!tracingActive) { startTracing(); return tracingActive; }
        else { stopTracing(); return false; }
    }

    private void startTracing() {
        synchronized (lock) {
        stepCount = 0;
        sourceLineCache.clear();
        lastPositions.clear();
        targets.clear();
        suspendedByUs.clear();
        stepOverMode.clear();
        currentTargetIndex = -1;

        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        for (IDebugTarget dt : lm.getDebugTargets()) {
            if (!(dt instanceof ISuspendResume)) continue;
            targets.add(dt);
        }

        log("startTracing: " + targets.size() + " targets");

        if (targets.isEmpty()) {
            log("no targets yet — will wait for CREATE events + poll");
        }

        tracingActive = true;
        DebugPlugin.getDefault().addDebugEventListener(this);

        int nSuspend = 0;
        for (IDebugTarget dt : targets) {
            ISuspendResume sr = (ISuspendResume) dt;
            if (!sr.isSuspended() && sr.canSuspend()) {
                asyncSuspendTarget(dt);
                nSuspend++;
            }
        }
        log("async suspend on " + nSuspend + " targets");
        startPollThread();
        }
    }

    private void stopTracing() {
        synchronized (lock) {
        tracingActive = false;
        DebugPlugin.getDefault().removeDebugEventListener(this);
        if (tracingThread != null) {
            tracingThread.interrupt();
            tracingThread = null;
        }
        log("stopTracing: " + stepCount + " steps recorded");

        for (IDebugTarget dt : suspendedByUs) {
            ISuspendResume sr = (ISuspendResume) dt;
            if (!sr.isSuspended() || !sr.canResume()) continue;
            final IDebugTarget tgt = dt;
            Thread th = new Thread(() -> {
                try { sr.resume(); }
                catch (DebugException e) {
                    log("resume failed: " + safeTargetName(tgt) + " " + e.getMessage());
                }
            }, "resume-" + safeTargetName(dt));
            th.setDaemon(true);
            th.start();
        }
        targets.clear();
        suspendedByUs.clear();
        stepOverMode.clear();
        lastPositions.clear();
        }
    }

    // ==================== Poll loop (background thread) ====================

    private void startPollThread() {
        tracingThread = new Thread(() -> {
            log("poll thread started");
            while (tracingActive) {
                try {
                    stepLoopBg();
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log("poll thread exception: " + e.getClass().getName()
                        + " " + e.getMessage());
                }
            }
            log("poll thread exited");
        }, "tracing-poll");
        tracingThread.setDaemon(true);
        tracingThread.start();
    }

    private void stepLoopBg() {
        if (targets.isEmpty()) return;

        synchronized (lock) {
            // 1. Scan all suspended targets — record any new position.
            for (IDebugTarget dt : targets) {
                try {
                    ISuspendResume sr = (ISuspendResume) dt;
                    if (!sr.isSuspended()) continue;

                    IThread[] threads = dt.getThreads();
                    if (threads == null) continue;
                    for (IThread t : threads) {
                        if (!t.isSuspended()) continue;
                        IStackFrame frame = getTopFrame(t);
                        if (frame == null) continue;

                        String key = dt.toString() + "|" + t.toString();
                        String pos = safeFrameName(frame) + ":" + safeLineNumber(frame);
                        String oldPos = lastPositions.get(key);

                        if (!pos.equals(oldPos)) {
                            lastPositions.put(key, pos);
                            String targetName = safeTargetName(dt);
                            String threadName = safeThreadName(t);
                            String frameName = safeFrameName(frame);

                            if (!isFiltered(frameName)) {
                                int line = safeLineNumber(frame);
                                String sourceUri = frame instanceof IBslStackFrame
                                    ? String.valueOf(((IBslStackFrame) frame).getSource()) : "";
                                String sourceCode = resolveSourceLine(frame, line);
                                addRecord(targetName, threadName, frameName, line, frame,
                                    sourceCode, sourceUri);
                                log("step " + stepCount + " " + targetName + "/" + threadName
                                    + " " + frameName + ":" + line + " " + sourceCode);

                                if (stepCount >= MAX_STEPS) {
                                    log("max steps reached");
                                    stopTracing();
                                    return;
                                }
                            }
                        }
                    }
                } catch (DebugException e) {
                    // target became invalid — will be pruned next cycle
                }
            }

            if (!tracingActive) return;

            // 2. Step the next target (round-robin).
            tryStepNext();
        }
    }

    private boolean tryStepNext() {
        if (!tracingActive || targets.isEmpty()) return false;

        int attempts = 0;
        while (attempts < targets.size()) {
            currentTargetIndex = (currentTargetIndex + 1) % targets.size();
            IDebugTarget dt = targets.get(currentTargetIndex);
            try {
                IThread[] threads = dt.getThreads();
                if (threads == null) { attempts++; continue; }
                boolean useStepOver = stepOverMode.contains(dt);
                for (IThread t : threads) {
                    if (!t.isSuspended()) continue;
                    if (!(t instanceof IStep)) continue;
                    IStep step = (IStep) t;
                    if (useStepOver ? step.canStepOver() : step.canStepInto()) {
                        stepOnBackgroundThread(dt, t, useStepOver);
                        return true;
                    }
                }
            } catch (DebugException e) {
                // skip target
            }
            attempts++;
        }
        return false;
    }

    private void stepOnBackgroundThread(IDebugTarget dt, IThread t, boolean stepOver) {
        final String name = safeTargetName(dt) + "/" + safeThreadName(t);
        Thread th = new Thread(() -> {
            try {
                IStep step = (IStep) t;
                if (stepOver) {
                    step.stepOver();
                } else {
                    step.stepInto();
                }
            } catch (DebugException e) {
                log("step failed on " + name + " " + e.getMessage());
            } catch (Exception e) {
                log("step exception on " + name + " "
                    + e.getClass().getName() + ": " + e.getMessage());
            }
        }, "step-" + safeTargetName(dt));
        th.setDaemon(true);
        th.start();
    }

    // ==================== Event listener (CREATE / TERMINATE / SUSPEND) ====================

    @Override
    public void handleDebugEvents(DebugEvent[] events) {
        if (!tracingActive) return;
        for (DebugEvent ev : events) {
            if (ev.getKind() == DebugEvent.CREATE) {
                Object src = ev.getSource();
                if (!(src instanceof IDebugTarget)) continue;
                IDebugTarget dt = (IDebugTarget) src;
                synchronized (lock) {
                    if (targets.contains(dt)) continue;
                    if (!(dt instanceof ISuspendResume)) continue;
                    targets.add(dt);
                }
                ISuspendResume sr = (ISuspendResume) dt;
                log("CREATE " + safeTargetName(dt) + " suspended=" + sr.isSuspended());
                if (!sr.isSuspended() && sr.canSuspend()) {
                    asyncSuspendTarget(dt);
                }
            } else if (ev.getKind() == DebugEvent.SUSPEND) {
                if (steppingInProgress) continue;
                steppingInProgress = true;
                try {
                    IDebugTarget dt = resolveTarget(ev);
                    if (dt == null || !targets.contains(dt)) continue;
                    handleSuspendAndStep(dt);
                } finally {
                    steppingInProgress = false;
                }
            } else if (ev.getKind() == DebugEvent.TERMINATE) {
                Object src = ev.getSource();
                if (src instanceof IDebugTarget) {
                    IDebugTarget dt = (IDebugTarget) src;
                    synchronized (lock) {
                        if (targets.remove(dt)) {
                            suspendedByUs.remove(dt);
                            stepOverMode.remove(dt);
                            log("TERMINATE " + safeTargetName(dt));
                        }
                        if (targets.isEmpty()) {
                            log("all targets terminated");
                            stopTracing();
                            return;
                        }
                    }
                }
            }
        }
    }

    private IDebugTarget resolveTarget(DebugEvent ev) {
        Object src = ev.getSource();
        if (src instanceof IDebugTarget) return (IDebugTarget) src;
        if (src instanceof IThread) return ((IThread) src).getDebugTarget();
        return null;
    }

    private void handleSuspendAndStep(IDebugTarget dt) {
        ISuspendResume sr = (ISuspendResume) dt;
        if (!sr.isSuspended()) return;
        try {
            IThread[] threads = dt.getThreads();
            if (threads == null) return;
            IThread stepped = null;
            for (IThread t : threads) {
                if (!t.isSuspended()) continue;
                IStackFrame frame = getTopFrame(t);
                if (frame == null) continue;

                String key = dt.toString() + "|" + t.toString();
                String pos = safeFrameName(frame) + ":" + safeLineNumber(frame);
                String oldPos = lastPositions.get(key);

                if (!pos.equals(oldPos)) {
                    lastPositions.put(key, pos);
                    String targetName = safeTargetName(dt);
                    String threadName = safeThreadName(t);
                    String frameName = safeFrameName(frame);

                    if (!isFiltered(frameName)) {
                        int line = safeLineNumber(frame);
                        String sourceUri = frame instanceof IBslStackFrame
                            ? String.valueOf(((IBslStackFrame) frame).getSource()) : "";
                        String sourceCode = resolveSourceLine(frame, line);
                        addRecord(targetName, threadName, frameName, line, frame,
                            sourceCode, sourceUri);

                        if (stepCount >= MAX_STEPS) {
                            stopTracing();
                            return;
                        }
                    }
                }

                if (stepped == null && (t instanceof IStep)) {
                    IStep step = (IStep) t;
                    boolean useStepOver = stepOverMode.contains(dt);
                    if (useStepOver ? step.canStepOver() : step.canStepInto()) {
                        stepped = t;
                    }
                }
            }
            if (stepped != null) {
                int idx = currentTargetIndex;
                // rotate index so poll loop continues from next target
                currentTargetIndex = targets.indexOf(dt);
                boolean useStepOver = stepOverMode.contains(dt);
                if (useStepOver) ((IStep) stepped).stepOver();
                else ((IStep) stepped).stepInto();
            }
        } catch (DebugException e) {
            log("handleSuspendAndStep error: " + e.getMessage());
        }
    }

    // ==================== Async suspend ====================

    private void asyncSuspendTarget(IDebugTarget dt) {
        final ISuspendResume sr = (ISuspendResume) dt;
        Thread th = new Thread(() -> {
            try {
                sr.suspend();
                suspendedByUs.add(dt);
                log("suspend OK " + safeTargetName(dt));
            } catch (DebugException e) {
                log("suspend FAILED " + safeTargetName(dt) + " " + e.getMessage());
            }
        }, "suspend-" + safeTargetName(dt));
        th.setDaemon(true);
        th.start();
    }

    // ==================== Helpers ====================

    private static IStackFrame getTopFrame(IThread thread) {
        if (thread == null) return null;
        try {
            IStackFrame f = thread.getTopStackFrame();
            if (f != null) return f;
        } catch (DebugException e) { /* ignore */ }
        try {
            IStackFrame[] frames = thread.getStackFrames();
            if (frames != null && frames.length > 0) return frames[0];
        } catch (DebugException e) { /* ignore */ }
        return null;
    }

    private boolean isFiltered(String frameName) {
        if (frameName == null || frameName.isEmpty()) return false;
        for (ModuleFilterEntry f : moduleFilters) {
            if (!f.enabled) continue;
            if (globMatch(frameName, f.pattern)) return true;
        }
        return false;
    }

    private static boolean globMatch(String name, String pattern) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '*': sb.append(".*"); break;
                case '?': sb.append('.'); break;
                case '.': sb.append("\\."); break;
                case '\\': sb.append("\\\\"); break;
                case '(': sb.append("\\("); break;
                case ')': sb.append("\\)"); break;
                case '[': sb.append("\\["); break;
                case ']': sb.append("\\]"); break;
                case '+': sb.append("\\+"); break;
                case '^': sb.append("\\^"); break;
                case '$': sb.append("\\$"); break;
                case '|': sb.append("\\|"); break;
                default: sb.append(c);
            }
        }
        return name.matches(sb.toString());
    }

    // ==================== Safe accessors ====================

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

    // ==================== Record ====================

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

            ISourceLookupResult result = DebugUITools.lookupSource(frame, null);
            if (result != null) {
                IEditorInput editorInput = result.getEditorInput();
                if (editorInput != null) {
                    String[] lines = readSourceLines(editorInput);
                    if (lines != null && lineNumber <= lines.length) {
                        return lines[lineNumber - 1].trim();
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
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
        } catch (Exception e) { /* ignore */ }
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
            if (storage == null) return null;

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
        log("openFrameInEditor called: step=" + rec.stepIndex
            + " frame=" + rec.frameName + ":" + rec.lineNumber
            + " sourceUri=" + rec.sourceUri
            + " frame!=null=" + (rec.frame != null));
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
                                    TextEditorPositioner.positionEditor(ed3, recLine - 1);
                                }
                                return;
                            }
                        }
                    }
                } catch (Exception e) { /* ignore */ }
            }

            if (rec.frame != null) {
                ISourceLookupResult result = DebugUITools.lookupSource(rec.frame, null);
                if (result != null) {
                    try {
                        DebugUITools.displaySource(result, page);
                        return;
                    } catch (Exception e1) { /* ignore */ }
                    IEditorInput editorInput = result.getEditorInput();
                    String editorId = result.getEditorId();
                    if (editorInput != null && editorId != null) {
                        IEditorPart editor = page.openEditor(editorInput, editorId);
                        if (editor != null) positionToLine(editor, recLine);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            TracingUIActivator.getDefault().getLog().log(
                new Status(IStatus.ERROR, TracingUIActivator.PLUGIN_ID,
                    "open editor failed", e));
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
    public void dispose() {
        if (tracingActive) stopTracing();
        DebugPlugin.getDefault().removeDebugEventListener(this);
        super.dispose();
    }
}
