package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import java.util.Optional;
import org.eclipse.core.resources.IFile;

public interface IBmObjectProvider {
   Optional<IBmObject> getObject(IFile var1);
}
