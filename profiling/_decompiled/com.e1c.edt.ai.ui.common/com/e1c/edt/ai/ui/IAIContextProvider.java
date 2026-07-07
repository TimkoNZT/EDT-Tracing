package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import java.util.Optional;
import org.eclipse.jface.text.source.SourceViewer;

public interface IAIContextProvider {
   Optional<AIContext> create(SourceViewer var1, AITarget var2, ICancellationToken var3);
}
