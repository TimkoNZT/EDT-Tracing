package com.e1c.edt.ai.ui;

import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;

public interface IUI {
   Optional<Shell> getShell();

   Optional<StyledText> getTextWidget();

   Optional<SourceViewer> getLastSourceViewer();

   Optional<SourceViewer> getSourceViewer(StyledText var1);

   Optional<IFile> getFile(SourceViewer var1);

   Optional<IViewPart> showView(String var1);
}
