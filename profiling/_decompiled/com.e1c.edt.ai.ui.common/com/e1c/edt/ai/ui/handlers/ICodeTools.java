package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.AIContext;
import java.util.Optional;
import org.eclipse.jface.text.source.SourceViewer;

public interface ICodeTools {
   boolean hasTarget(CodeAction var1);

   Optional<AIContext> createContextForTarget(SourceViewer var1, CodeAction var2);

   Optional<TargetMethod> getTargetMethod();

   void selectMethodComment(TargetMethod var1);
}
