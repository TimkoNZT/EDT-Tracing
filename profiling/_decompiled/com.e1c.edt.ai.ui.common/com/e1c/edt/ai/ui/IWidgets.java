package com.e1c.edt.ai.ui;

import java.util.stream.Stream;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface IWidgets {
   Stream<Control> getChildren(Composite var1);
}
