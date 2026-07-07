package com.e1c.edt.ai.ui;

import java.util.Optional;
import org.eclipse.swt.custom.StyledText;

public interface ITextWidgetInfoProvider {
   Optional<Integer> getLastMouseOffset(StyledText var1);
}
