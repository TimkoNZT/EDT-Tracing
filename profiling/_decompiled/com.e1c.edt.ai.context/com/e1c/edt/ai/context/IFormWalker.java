package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;

interface IFormWalker {
   void walk(EObject var1, IFormVisitor var2, ICancellationToken var3);
}
