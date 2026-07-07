package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;
import org.eclipse.jface.text.IDocument;

interface IEntitiesWalker {
   boolean walk(IDocument var1, String var2, int var3, int var4, IModuleProvider var5, IEntityVisitor var6, IStatistics var7, ICancellationToken var8);
}
