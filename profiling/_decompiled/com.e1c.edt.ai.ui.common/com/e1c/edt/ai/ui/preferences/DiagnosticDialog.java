package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.DiagnosticResult;
import com.e1c.edt.ai.assistent.DiagnosticSeverity;
import com.e1c.edt.ai.assistent.IDiagnosticContext;
import com.e1c.edt.ai.assistent.IDiagnosticTest;
import com.e1c.edt.ai.assistent.IDiagnosticsFactory;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IWeb;
import com.google.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class DiagnosticDialog extends TitleAreaDialog {
   private final List<IDiagnosticTest> tests;
   private ProgressBar progressBar;
   private Label currentTestLabel;
   private Table resultTable;
   private TableColumn iconColumn;
   private TableColumn titleColumn;
   private TableColumn statusColumn;
   private TableColumn buttonColumn;
   private Label summaryLabel;
   private final List<TableEditor> tableEditors = new ArrayList();
   private final AtomicReference<Job> runningJob = new AtomicReference();
   private final List<Map.Entry<IDiagnosticTest, DiagnosticResult>> outcomes = new ArrayList();
   @Inject
   private IDispatcher dispatcher;
   @Inject
   private IDiagnosticsFactory diagnosticsFactory;
   @Inject
   private IDiagnosticContext context;
   @Inject
   private IDiagnosticReportDialogProvider diagnosticsReportDialogProvider;
   @Inject
   private ISettings settings;
   @Inject
   private IWeb web;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity;

   public DiagnosticDialog(Shell shell) {
      super(shell);
      this.setShellStyle(this.getShellStyle() | 16);
      BaseActivator.injectMembers(this);
      this.tests = this.diagnosticsFactory.createDiagnostics();
      this.setHelpAvailable(false);
   }

   protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setMinimumSize(720, 360);
   }

   public void create() {
      super.create();
      this.setTitle(Messages.DiagnosticDialog_Title);
      this.setMessage(Messages.DiagnosticDialog_Message);
      this.dispatcher.dispatchAsync(() -> {
         if (this.getShell() != null && !this.getShell().isDisposed()) {
            this.startDiagnostic();
         }

      });
      this.getShell().addListener(12, (e) -> this.cancelDiagnostic());
   }

   protected Control createDialogArea(Composite parent) {
      Composite area = (Composite)super.createDialogArea(parent);
      Composite root = new Composite(area, 0);
      root.setLayoutData(new GridData(4, 4, true, true));
      GridLayout gridLayout = new GridLayout(1, false);
      gridLayout.marginWidth = 12;
      gridLayout.marginHeight = 10;
      gridLayout.verticalSpacing = 8;
      root.setLayout(gridLayout);
      this.progressBar = new ProgressBar(root, 65536);
      this.progressBar.setLayoutData(new GridData(4, 16777216, true, false));
      this.progressBar.setMinimum(0);
      this.progressBar.setMaximum(Math.max(1, this.tests.size()));
      this.progressBar.setSelection(0);
      this.currentTestLabel = new Label(root, 64);
      GridData currentTest = new GridData(4, 128, true, false);
      currentTest.widthHint = 620;
      this.currentTestLabel.setLayoutData(currentTest);
      this.currentTestLabel.setText(Messages.DiagnosticDialog_Preparing);
      this.summaryLabel = new Label(root, 64);
      GridData result = new GridData(4, 128, true, false);
      result.widthHint = 620;
      this.summaryLabel.setLayoutData(result);
      this.summaryLabel.setText("");
      this.setExcluded(this.summaryLabel, true);
      this.resultTable = new Table(root, 67584);
      this.resultTable.setHeaderVisible(false);
      this.resultTable.setLinesVisible(true);
      GridData tableGd = new GridData(4, 4, true, true);
      tableGd.heightHint = 220;
      this.resultTable.setLayoutData(tableGd);
      this.iconColumn = new TableColumn(this.resultTable, 16384);
      this.titleColumn = new TableColumn(this.resultTable, 16384);
      this.statusColumn = new TableColumn(this.resultTable, 16384);
      this.buttonColumn = new TableColumn(this.resultTable, 16384);
      this.resultTable.addListener(11, (e) -> {
         if (this.resultTable != null && !this.resultTable.isDisposed()) {
            int width = this.resultTable.getClientArea().width;
            if (width > 0) {
               int iconColumnWidth = 40;
               int statusColumnWidth = 90;
               int buttonColumnWidth = 140;
               int titleColumnWidth = width - iconColumnWidth - statusColumnWidth - buttonColumnWidth;
               if (titleColumnWidth < 150) {
                  titleColumnWidth = 150;
               }

               this.titleColumn.setWidth(titleColumnWidth);
               this.iconColumn.setWidth(iconColumnWidth);
               this.statusColumn.setWidth(statusColumnWidth);
               this.buttonColumn.setWidth(buttonColumnWidth);
            }
         }
      });
      this.resultTable.redraw();
      this.setExcluded(this.resultTable, true);
      Link troubleshootingLink = new Link(root, 0);
      GridData linkGd = new GridData(4, 16777224, true, false);
      troubleshootingLink.setLayoutData(linkGd);
      troubleshootingLink.setText(Messages.DiagnosticDialog_TroubleshootingLink);
      troubleshootingLink.addSelectionListener(new SelectionListener() {
         public void widgetSelected(SelectionEvent e) {
            String homePage = DiagnosticDialog.this.settings.getHomePage();
            if (homePage != null && !homePage.isEmpty()) {
               DiagnosticDialog.this.web.browse(homePage + "/troubleshooting/");
            } else {
               DiagnosticDialog.this.web.browse("https://code.1c.ai/troubleshooting/");
            }

         }

         public void widgetDefaultSelected(SelectionEvent e) {
            this.widgetSelected(e);
         }
      });
      return area;
   }

   protected void createButtonsForButtonBar(Composite parent) {
      this.createButton(parent, 0, Messages.DiagnosticDialog_CloseButton, true);
   }

   private void startDiagnostic() {
      if (this.runningJob.get() == null) {
         this.progressBar.setMaximum(this.tests.size());
         this.progressBar.setSelection(0);
         this.currentTestLabel.setText(Messages.DiagnosticDialog_Preparing);
         this.setMessage(Messages.DiagnosticDialog_CheckingConnection);
         this.outcomes.clear();
         this.clearResultsUI();
         Job job = this.dispatcher.createJob(Messages.DiagnosticDialog_Title, (jobCtx) -> {
            ProjectId project = ProjectId.Default;
            boolean stopFuther = false;
            this.context.setProject(project);
            this.context.setAIContext(new AIContext(project, "", (IDocument)null));

            for(int i = 0; i < this.tests.size(); ++i) {
               if (jobCtx.Monitor.isCanceled()) {
                  return;
               }

               IDiagnosticTest test = (IDiagnosticTest)this.tests.get(i);
               int step = i + 1;
               this.dispatcher.dispatchAsync(() -> {
                  if (!this.isUiGone()) {
                     this.currentTestLabel.setText(test.title());
                     this.currentTestLabel.getParent().layout(true, true);
                  }
               });
               DiagnosticResult result;
               if (stopFuther) {
                  result = DiagnosticResult.defaultResult();
               } else {
                  try {
                     result = test.execute(this.context, jobCtx.Monitor);
                     if (result == null) {
                        result = DiagnosticResult.error(Messages.DiagnosticDialog_ExecutionFailed, ServiceState.NONE, (Map)null, (Throwable)null, "DiagnosticResult was null");
                     }
                  } catch (Throwable t) {
                     result = DiagnosticResult.error(Messages.DiagnosticDialog_ExecutionFailed, ServiceState.NONE, (Map)null, t, this.stackTraceToString(t));
                  }
               }

               if (result.getSeverity() != DiagnosticSeverity.OK) {
                  stopFuther = true;
               }

               this.outcomes.add(Map.entry(test, result));
               this.dispatcher.dispatchAsync(() -> {
                  if (!this.isUiGone()) {
                     this.progressBar.setSelection(step);
                  }
               });
            }

            this.dispatcher.dispatchAsync(() -> this.showResults(this.outcomes));
            this.context.releaseContext();
         }, false, CancellationTokens.NONE);
         this.runningJob.set(job);
         job.addJobChangeListener(new JobChangeAdapter() {
            public void done(IJobChangeEvent job) {
               DiagnosticDialog.this.runningJob.set((Object)null);
            }
         });
         job.schedule();
      }
   }

   private void setExcluded(Control control, boolean excluded) {
      GridData gd = (GridData)control.getLayoutData();
      gd.exclude = excluded;
      control.setVisible(!excluded);
   }

   private void buildResultsTable(List<Map.Entry<IDiagnosticTest, DiagnosticResult>> results) {
      for(TableEditor editor : this.tableEditors) {
         Control control = editor.getEditor();
         if (control != null && !control.isDisposed()) {
            control.dispose();
         }

         editor.dispose();
      }

      this.tableEditors.clear();

      for(final Map.Entry<IDiagnosticTest, DiagnosticResult> entry : results) {
         Map.Entry<Image, String> processedSeverity = this.processSeverity((DiagnosticResult)entry.getValue());
         String severity = (String)processedSeverity.getValue();
         Image image = (Image)processedSeverity.getKey();
         TableItem tableItem = new TableItem(this.resultTable, 0);
         tableItem.setImage(0, image);
         tableItem.setText(1, ((IDiagnosticTest)entry.getKey()).title());
         tableItem.setText(2, severity);
         if (((DiagnosticResult)entry.getValue()).getSeverity() == DiagnosticSeverity.ERROR) {
            final Button reportButton = new Button(this.resultTable, 8);
            reportButton.setText(Messages.DiagnosticDialog_OpenReport);
            reportButton.addSelectionListener(new SelectionListener() {
               public void widgetSelected(SelectionEvent e) {
                  DiagnosticDialog.this.diagnosticsReportDialogProvider.openErrorDialog(DiagnosticDialog.this.getShell(), (IDiagnosticTest)entry.getKey(), (DiagnosticResult)entry.getValue(), DiagnosticDialog.this.context);
               }

               public void widgetDefaultSelected(SelectionEvent e) {
               }
            });
            final TableEditor editor = new TableEditor(this.resultTable);
            editor.grabHorizontal = true;
            editor.minimumWidth = 130;
            editor.setEditor(reportButton, tableItem, 3);
            this.tableEditors.add(editor);
            tableItem.addDisposeListener(new DisposeListener() {
               public void widgetDisposed(DisposeEvent e) {
                  if (!reportButton.isDisposed()) {
                     reportButton.dispose();
                  }

                  editor.dispose();
               }
            });
         } else {
            tableItem.setText(3, "");
         }
      }

   }

   private Map.Entry<Image, String> processSeverity(DiagnosticResult result) {
      Display display = Display.getCurrent();
      Map.Entry<Image, String> entry = null;
      switch (result.getSeverity()) {
         case OK:
            entry = Map.entry(display.getSystemImage(2), Messages.DiagnosticDialog_OK);
            break;
         case DEFAULT:
            entry = Map.entry(display.getSystemImage(8), Messages.DiagnosticDialog_Skip);
            break;
         case ERROR:
         default:
            entry = Map.entry(display.getSystemImage(1), Messages.DiagnosticDialog_Failed);
      }

      return entry;
   }

   private void clearResultsUI() {
      this.setExcluded(this.summaryLabel, true);
      this.setExcluded(this.resultTable, true);
      this.summaryLabel.setText("");

      for(TableEditor editor : this.tableEditors) {
         Control control = editor.getEditor();
         if (control != null && !control.isDisposed()) {
            control.dispose();
         }

         editor.dispose();
      }

      this.tableEditors.clear();
      this.resultTable.removeAll();
      this.resultTable.getParent().layout(true, true);
   }

   private void showResults(List<Map.Entry<IDiagnosticTest, DiagnosticResult>> outcomes) {
      if (!this.isUiGone()) {
         this.setMessage(Messages.DiagnosticDialog_Ready);
         this.currentTestLabel.setText("");
         int errorCount = 0;

         for(Map.Entry<IDiagnosticTest, DiagnosticResult> entry : outcomes) {
            if (((DiagnosticResult)entry.getValue()).getSeverity() == DiagnosticSeverity.ERROR) {
               ++errorCount;
            }
         }

         if (errorCount == 0) {
            this.summaryLabel.setText(Messages.DiagnosticDialog_Successful);
         } else {
            this.summaryLabel.setText(Messages.DiagnosticDialog_ProblemsDetected + errorCount);
         }

         this.setExcluded(this.summaryLabel, false);
         this.setExcluded(this.resultTable, false);
         this.buildResultsTable(outcomes);
         this.summaryLabel.getParent().layout(true, true);
      }
   }

   private boolean isUiGone() {
      return this.getShell() == null || this.getShell().isDisposed() || this.progressBar == null || this.progressBar.isDisposed() || this.summaryLabel == null || this.summaryLabel.isDisposed() || this.currentTestLabel == null || this.currentTestLabel.isDisposed();
   }

   private void cancelDiagnostic() {
      Job job = (Job)this.runningJob.get();
      if (job != null) {
         job.cancel();
      }

   }

   private String stackTraceToString(Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      return sw.toString();
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[DiagnosticSeverity.values().length];

         try {
            var0[DiagnosticSeverity.DEFAULT.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[DiagnosticSeverity.ERROR.ordinal()] = 3;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[DiagnosticSeverity.OK.ordinal()] = 1;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity = var0;
         return var0;
      }
   }
}
