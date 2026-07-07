package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICodeCompletionContext;
import org.eclipse.swt.custom.StyledText;

interface ICodeCompletionViewModel<TContext extends ICodeCompletionContext> {
   AutoCloseable activate(StyledText var1);
}
