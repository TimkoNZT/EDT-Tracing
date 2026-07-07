package com.e1c.edt.ai.ui;

import com._1c.g5.v8.dt.ui.util.Labeler;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.context.IModuleProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;

public class ModuleNameProvider implements IModuleNameProvider {
   private final IModuleProvider moduleProvider;

   @Inject
   public ModuleNameProvider(IModuleProvider moduleProvider) {
      this.moduleProvider = (IModuleProvider)Preconditions.checkNotNull(moduleProvider);
   }

   public Optional<String> getModuleName(String path) {
      return this.moduleProvider.getModule((IDocument)null, path, CancellationTokens.NONE).map((module) -> module.getModule()).map((module) -> Labeler.path(module, '→').skipCommonNode().filter((candidate) -> !(candidate instanceof IProject))).map((moduleLabel) -> moduleLabel.stopAfter(IProject.class)).map((labeler) -> labeler.label());
   }
}
