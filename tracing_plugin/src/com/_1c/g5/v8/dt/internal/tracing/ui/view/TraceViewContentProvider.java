package com._1c.g5.v8.dt.internal.tracing.ui.view;

import com._1c.g5.v8.dt.profiling.core.ILineProfilingResult;
import java.util.List;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TraceViewContentProvider implements IStructuredContentProvider {

    private List<ILineProfilingResult> lines;

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof List) {
            this.lines = (List<ILineProfilingResult>) newInput;
        } else {
            this.lines = null;
        }
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return lines != null ? lines.toArray() : new Object[0];
    }

    @Override
    public void dispose() {
        lines = null;
    }
}
