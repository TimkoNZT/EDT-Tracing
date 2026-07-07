package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.model.EditingMode;
import com._1c.g5.v8.dt.core.model.IModelEditingSupport;
import com.e1c.edt.ai.IEditingSupport;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.Set;
import org.eclipse.core.resources.IFile;

public class EditingSupport implements IEditingSupport {
   private static final Set<String> RESTRICTED_EXTENSIONS = Set.of(".form", ".mdo");
   private final IBmObjectProvider bmObjectProvider;
   private final IModelEditingSupport modelEditingSupport;

   @Inject
   public EditingSupport(IBmObjectProvider bmObjectProvider, IModelEditingSupport modelEditingSupport) {
      Preconditions.checkNotNull(bmObjectProvider);
      Preconditions.checkNotNull(modelEditingSupport);
      this.bmObjectProvider = bmObjectProvider;
      this.modelEditingSupport = modelEditingSupport;
   }

   public boolean canEdit(IFile file) {
      return !this.isRestrictedFile(file) && (Boolean)this.getObject(file).map((obj) -> this.canEdit(obj)).orElse(true);
   }

   public boolean canDelete(IFile file) {
      return !this.isRestrictedFile(file) && (Boolean)this.getObject(file).map((obj) -> this.canDelete(obj)).orElse(true);
   }

   private boolean isRestrictedFile(IFile file) {
      String fileName = file.getName();
      int lastDotIndex = fileName.lastIndexOf(46);
      if (lastDotIndex < 0) {
         return false;
      } else {
         String extension = fileName.substring(lastDotIndex);
         return RESTRICTED_EXTENSIONS.contains(extension);
      }
   }

   private boolean canEdit(IBmObject obj) {
      return this.modelEditingSupport.canEdit(obj, EditingMode.DIRECT);
   }

   private boolean canDelete(IBmObject obj) {
      return this.modelEditingSupport.canDelete(obj, EditingMode.DIRECT);
   }

   private Optional<IBmObject> getObject(IFile file) {
      return this.bmObjectProvider.getObject(file);
   }
}
