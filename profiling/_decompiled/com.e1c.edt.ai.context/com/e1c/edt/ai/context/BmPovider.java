package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.BmObject;
import com._1c.g5.v8.bm.core.IBmEngine;
import com._1c.g5.v8.bm.core.IBmExternalUriResolver;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

public class BmPovider implements IBmPovider {
   private final IResourceLookup resourceLookup;
   private final IBmModelManager modelManager;
   private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;

   @Inject
   public BmPovider(IResourceLookup resourceLookup, IBmModelManager modelManager, IProjectFileSystemSupportProvider projectFileSystemSupportProvider) {
      Preconditions.checkNotNull(resourceLookup);
      Preconditions.checkNotNull(modelManager);
      Preconditions.checkNotNull(projectFileSystemSupportProvider);
      this.resourceLookup = resourceLookup;
      this.modelManager = modelManager;
      this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
   }

   public Optional<BmRoot> getRoot(IDocument document, String filePath, final ICancellationToken cancellationToken) {
      if (filePath != null && !filePath.isBlank()) {
         URI uri = this.getURI(filePath);
         IProject project = this.resourceLookup.getProject(uri);
         if (project == null) {
            return Optional.empty();
         } else {
            IBmModel model = this.modelManager.getModel(project);
            if (model == null) {
               return Optional.empty();
            } else {
               IDtProject dtProject = this.modelManager.getDtProject(model);
               if (dtProject == null) {
                  return Optional.empty();
               } else {
                  IBmEngine engine = model.getEngine();
                  if (engine == null) {
                     return Optional.empty();
                  } else {
                     IBmObject bmObject = null;
                     if (document != null && document instanceof BslXtextDocument) {
                        IUnitOfWork<XtextResource, XtextResource> work = (res) -> res;
                        XtextResource bslXtextDocument = (XtextResource)((BslXtextDocument)document).readOnlyDataModel(work);
                        if (bslXtextDocument != null) {
                           for(EObject content : bslXtextDocument.getContents()) {
                              if (content instanceof Module) {
                                 Module module = (Module)content;
                                 Resource moduleResource = module.eResource();
                                 if (moduleResource instanceof BslResource) {
                                    ((BslResource)moduleResource).setDeepAnalysis(true);
                                    EcoreUtil2.resolveLazyCrossReferences(moduleResource, new CancelIndicator() {
                                       public boolean isCanceled() {
                                          return cancellationToken.isCanceled();
                                       }
                                    });
                                 }

                                 if (module instanceof BmObject) {
                                    bmObject = (BmObject)module;
                                    break;
                                 }
                              }
                           }
                        }
                     }

                     if (bmObject == null) {
                        for(IBmExternalUriResolver provider : engine.getExternalUriResolvers()) {
                           if (cancellationToken.isCanceled()) {
                              break;
                           }

                           EObject obj = provider.getObject(uri);
                           if (obj != null && obj instanceof IBmObject) {
                              bmObject = (IBmObject)obj;
                              break;
                           }
                        }
                     }

                     return bmObject == null ? Optional.empty() : Optional.of(new BmRoot(filePath, uri, project, model, dtProject, engine, bmObject, this.projectFileSystemSupportProvider));
                  }
               }
            }
         }
      } else {
         return Optional.empty();
      }
   }

   private URI getURI(String filePath) {
      Preconditions.checkNotNull(filePath);
      return URI.createPlatformResourceURI(filePath, true).appendFragment("/0");
   }
}
