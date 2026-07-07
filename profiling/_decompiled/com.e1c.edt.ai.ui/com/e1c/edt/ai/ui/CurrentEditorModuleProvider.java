package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.IModuleProvider;
import com.e1c.edt.ai.context.ModuleInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Optional;
import org.eclipse.jface.text.IDocument;

class CurrentEditorModuleProvider implements IModuleProvider {
   private final IModuleProvider baseResourceSetProvider;

   @Inject
   public CurrentEditorModuleProvider(@Named("BaseModuleProvider") IModuleProvider baseResourceSetProvider) {
      Preconditions.checkNotNull(baseResourceSetProvider);
      this.baseResourceSetProvider = baseResourceSetProvider;
   }

   public Optional<ModuleInfo> getModule(IDocument document, String filePath, ICancellationToken cancellationToken) {
      Optional<ModuleInfo> optionalModuleInfo = this.baseResourceSetProvider.getModuleInfo(document, cancellationToken);
      if (optionalModuleInfo.isEmpty()) {
         return this.baseResourceSetProvider.getModule(document, filePath, cancellationToken);
      } else {
         ModuleInfo moduleInfo = (ModuleInfo)optionalModuleInfo.get();
         String moduleFilePath = moduleInfo.getFilePath();
         return !filePath.equals(moduleFilePath) ? this.baseResourceSetProvider.getModule(document, moduleFilePath, cancellationToken) : Optional.of(moduleInfo);
      }
   }

   public Optional<ModuleInfo> getModuleInfo(IDocument document, ICancellationToken cancellationToken) {
      return this.baseResourceSetProvider.getModuleInfo(document, cancellationToken);
   }
}
