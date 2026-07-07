package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.StatisticsType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.nodemodel.ICompositeNode;

class EntitiesWalker implements IEntitiesWalker {
   private final ILog log;
   private final IV8Model v8Model;
   private final IIdFactory idFactory;
   private final IBmPovider bmPovider;

   @Inject
   public EntitiesWalker(ILog log, IV8Model v8Model, IIdFactory idFactory, IBmPovider bmPovider) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(v8Model);
      Preconditions.checkNotNull(idFactory);
      Preconditions.checkNotNull(bmPovider);
      this.log = log;
      this.v8Model = v8Model;
      this.idFactory = idFactory;
      this.bmPovider = bmPovider;
   }

   public boolean walk(IDocument document, String path, int start, int finish, IModuleProvider resourceSetProvider, IEntityVisitor visitor, IStatistics statistics, ICancellationToken cancellationToken) {
      try {
         Throwable var10 = null;
         EObject nextObject = null;

         Optional<BmRoot> optionalRoot;
         try {
            label463: {
               AutoCloseable measurement = statistics.measureDuration(StatisticsType.LOAD_MODULE_DURATUION);

               try {
                  optionalRoot = this.bmPovider.getRoot(document, path, cancellationToken);
                  if (!optionalRoot.isEmpty()) {
                     break label463;
                  }
               } finally {
                  if (measurement != null) {
                     measurement.close();
                  }

               }

               return false;
            }
         } catch (Throwable var25) {
            if (var10 == null) {
               var10 = var25;
            } else if (var10 != var25) {
               var10.addSuppressed(var25);
            }

            throw var10;
         }

         BmRoot root = (BmRoot)optionalRoot.get();
         nextObject = null;

         while(!cancellationToken.isCanceled()) {
            if (nextObject == null) {
               nextObject = root.getBmObject();
            } else {
               EObject newOwner = nextObject.eContainer();
               if (newOwner == null) {
                  nextObject = this.v8Model.getBmObjectOwner(root.getModel(), nextObject);
               } else {
                  nextObject = newOwner;
               }

               if (nextObject == null) {
                  break;
               }
            }

            if (!(nextObject instanceof Module)) {
               if (nextObject instanceof Form) {
                  if (visitor.visitForm(root, (Form)nextObject)) {
                     return true;
                  }
               } else if (nextObject instanceof IBmObject) {
                  IBmObject bmObject = (IBmObject)nextObject;
                  if (visitor.visitBmObject(root, bmObject)) {
                     return true;
                  }
               }
            } else {
               visitor.visitModule(root, (Module)nextObject);
               TreeIterator<EObject> contentsIterator = nextObject.eAllContents();

               while(contentsIterator.hasNext() && !cancellationToken.isCanceled()) {
                  EObject obj = (EObject)contentsIterator.next();
                  ICompositeNode node = this.v8Model.getNode(obj);
                  if (node != null) {
                     visitor.visitNode(root, obj, node);
                     if (obj instanceof Variable || obj instanceof Invocation || obj instanceof FeatureAccess || obj instanceof Method) {
                        int nodeStart = node.getTotalOffset();
                        int nodeFinish = node.getTotalEndOffset();
                        if ((nodeStart < start || nodeStart > finish) && (nodeFinish < start || nodeFinish > finish) && !(obj instanceof Method)) {
                           continue;
                        }

                        String nodeId = this.idFactory.createNodeId(path, node);
                        if (nodeId == null) {
                           continue;
                        }

                        if (obj instanceof Method && visitor.visitMethod(root, nodeId, (Method)obj, node)) {
                           this.traceVisitEObject(obj, true);
                           return true;
                        }

                        if (!cancellationToken.isCanceled()) {
                           if (obj instanceof Variable && visitor.visitVariable(root, nodeId, (Variable)obj, node)) {
                              this.traceVisitEObject(obj, true);
                              return true;
                           }

                           if (obj instanceof Invocation && visitor.visitInvocation(root, nodeId, (Invocation)obj, node)) {
                              this.traceVisitEObject(obj, true);
                              return true;
                           }

                           if (obj instanceof FeatureAccess && visitor.visitFeatureAccess(root, nodeId, (FeatureAccess)obj, node)) {
                              this.traceVisitEObject(obj, true);
                              return true;
                           }
                        }
                     }

                     this.traceVisitEObject(obj, false);
                  }
               }
            }
         }

         return true;
      } catch (Exception error) {
         this.log.logError(error);
         return false;
      }
   }

   private void traceVisitEObject(EObject eObject, boolean visited) {
   }
}
