package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import java.util.Optional;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

interface IIdFactory {
   String createNodeId(String var1, ICompositeNode var2);

   String createObjectId(String var1, EObject var2, ICancellationToken var3);

   Optional<SourceSpan> getNodeId(String var1);
}
