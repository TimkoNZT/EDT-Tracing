package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmEngine;
import com._1c.g5.v8.bm.core.IBmExternalUriResolver;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

public class BmObjectProvider implements IBmObjectProvider {
   private final IBmModelManager modelManager;

   @Inject
   public BmObjectProvider(IBmModelManager modelManager) {
      Preconditions.checkNotNull(modelManager);
      this.modelManager = modelManager;
   }

   public Optional<IBmObject> getObject(IFile file) {
      IProject project = file.getProject();
      IBmModel model = this.modelManager.getModel(project);
      if (model == null) {
         return Optional.empty();
      } else {
         IBmEngine engine = model.getEngine();
         if (engine == null) {
            return Optional.empty();
         } else {
            URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true).appendFragment("/0");

            for(IBmExternalUriResolver provider : engine.getExternalUriResolvers()) {
               try {
                  EObject obj = provider.getObject(uri);
                  if (obj != null && obj instanceof IBmObject) {
                     return Optional.of((IBmObject)obj);
                  }
               } catch (Exception var9) {
               }
            }

            return Optional.empty();
         }
      }
   }
}
