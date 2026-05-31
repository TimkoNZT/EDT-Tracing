package com._1c.g5.v8.dt.internal.tracing.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com._1c.g5.v8.dt.internal.tracing.ui.TracingUIActivator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ModuleFilterDialog extends Dialog {

    private CheckboxTableViewer viewer;
    private final List<ModuleFilterEntry> filters;

    public ModuleFilterDialog(Shell parentShell, List<ModuleFilterEntry> filters) {
        super(parentShell);
        this.filters = new ArrayList<>(filters);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Фильтры модулей");
        newShell.setSize(500, 350);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Label hint = new Label(area, SWT.WRAP);
        hint.setText("Модули, попадающие под фильтр, не записываются в трассировку.\n"
            + "Шаблон: * — любая последовательность, ? — один символ.\n"
            + "Пример: СтандартныеПодсистемы*");
        hint.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        viewer = CheckboxTableViewer.newCheckList(area, SWT.FULL_SELECTION | SWT.BORDER);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableGd = new GridData(GridData.FILL_BOTH);
        tableGd.grabExcessVerticalSpace = true;
        tableGd.grabExcessHorizontalSpace = true;
        tableGd.minimumHeight = 250;
        table.setLayoutData(tableGd);

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(filters.toArray());

        TableViewerColumn colEnabled = new TableViewerColumn(viewer, SWT.LEFT);
        colEnabled.getColumn().setText("Вкл");
        colEnabled.getColumn().setWidth(40);
        colEnabled.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) { return ""; }
        });

        TableViewerColumn colPattern = new TableViewerColumn(viewer, SWT.LEFT);
        colPattern.getColumn().setText("Шаблон модуля");
        colPattern.getColumn().setWidth(380);
        colPattern.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModuleFilterEntry) element).pattern;
            }
        });

        for (ModuleFilterEntry e : filters) {
            viewer.setChecked(e, e.enabled);
        }

        viewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                ModuleFilterEntry e = (ModuleFilterEntry) event.getElement();
                e.enabled = event.getChecked();
            }
        });

        Composite buttonBar = new Composite(area, SWT.NONE);
        buttonBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        buttonBar.setLayout(new GridLayout(3, false));

        Button addBtn = new Button(buttonBar, SWT.PUSH);
        addBtn.setText("Добавить");
        addBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                InputDialog dlg = new InputDialog(getShell(),
                    "Новый фильтр", "Введите шаблон имени модуля:", "", null);
                if (dlg.open() == IDialogConstants.OK_ID) {
                    String pattern = dlg.getValue().trim();
                    if (!pattern.isEmpty()) {
                        ModuleFilterEntry entry = new ModuleFilterEntry(pattern, true);
                        filters.add(entry);
                        viewer.setInput(filters.toArray());
                        viewer.setChecked(entry, true);
                    }
                }
            }
        });

        Button editBtn = new Button(buttonBar, SWT.PUSH);
        editBtn.setText("Изменить");
        editBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
                if (sel.isEmpty()) return;
                ModuleFilterEntry entry = (ModuleFilterEntry) sel.getFirstElement();
                InputDialog dlg = new InputDialog(getShell(),
                    "Изменить фильтр", "Шаблон имени модуля:", entry.pattern, null);
                if (dlg.open() == IDialogConstants.OK_ID) {
                    String pattern = dlg.getValue().trim();
                    if (!pattern.isEmpty()) {
                        entry.pattern = pattern;
                        viewer.setInput(filters.toArray());
                        viewer.setChecked(entry, entry.enabled);
                    }
                }
            }
        });

        Button removeBtn = new Button(buttonBar, SWT.PUSH);
        removeBtn.setText("Удалить");
        removeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
                if (sel.isEmpty()) return;
                filters.remove(sel.getFirstElement());
                viewer.setInput(filters.toArray());
            }
        });

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "OK", true);
        createButton(parent, IDialogConstants.CANCEL_ID, "Отмена", false);
    }

    public List<ModuleFilterEntry> getFilters() {
        return new ArrayList<>(filters);
    }

    public static void saveToPrefs(List<ModuleFilterEntry> list) {
        StringBuilder sb = new StringBuilder();
        for (ModuleFilterEntry e : list) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(e.pattern).append('|').append(e.enabled);
        }
        TracingUIActivator.getDefault().getPreferenceStore()
            .setValue("moduleFilters", sb.toString());
    }

    public static List<ModuleFilterEntry> loadFromPrefs() {
        String raw = TracingUIActivator.getDefault().getPreferenceStore()
            .getString("moduleFilters");
        List<ModuleFilterEntry> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return list;
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int pipe = line.lastIndexOf('|');
            if (pipe <= 0) continue;
            String pattern = line.substring(0, pipe);
            boolean enabled = Boolean.parseBoolean(line.substring(pipe + 1));
            list.add(new ModuleFilterEntry(pattern, enabled));
        }
        return list;
    }
}
