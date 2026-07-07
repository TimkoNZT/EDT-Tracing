package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import java.util.Optional;
import org.eclipse.jface.text.IDocument;

public interface IModuleProvider {
   Optional<ModuleInfo> getModule(IDocument var1, String var2, ICancellationToken var3);

   Optional<ModuleInfo> getModuleInfo(IDocument var1, ICancellationToken var2);
}
