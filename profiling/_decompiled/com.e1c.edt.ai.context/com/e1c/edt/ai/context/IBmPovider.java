package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import java.util.Optional;
import org.eclipse.jface.text.IDocument;

public interface IBmPovider {
   Optional<BmRoot> getRoot(IDocument var1, String var2, ICancellationToken var3);
}
