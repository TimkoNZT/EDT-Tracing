package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContextInitializer;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

class AIContextProvider implements IAIContextProvider {
   private final IUI ui;
   private final IContentProvider contentProvider;
   private final IContextInitializer contextInitializer;
   private final IProjectIdProvider projectIdProvider;

   @Inject
   public AIContextProvider(IUI ui, IContentProvider contentProvider, IContextInitializer contextInitializer, IProjectIdProvider projectIdProvider) {
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(contentProvider);
      Preconditions.checkNotNull(contextInitializer);
      Preconditions.checkNotNull(projectIdProvider);
      this.ui = ui;
      this.contentProvider = contentProvider;
      this.contextInitializer = contextInitializer;
      this.projectIdProvider = projectIdProvider;
   }

   public Optional<AIContext> create(SourceViewer sourceViewer, AITarget target, ICancellationToken cancellationToken) {
      Preconditions.checkNotNull(sourceViewer);
      Preconditions.checkNotNull(target);
      Preconditions.checkNotNull(cancellationToken);
      Optional<IFile> file = this.ui.getFile(sourceViewer);
      String path = "";
      ProjectId projectId = ProjectId.Default;
      if (file.isPresent()) {
         path = ((IFile)file.get()).getFullPath().makeRelative().toPortableString();
         projectId = (ProjectId)this.projectIdProvider.getProjectId(path, cancellationToken).orElse(ProjectId.Default);
      }

      StyledText textWidget = sourceViewer.getTextWidget();
      if (textWidget == null) {
         return Optional.empty();
      } else {
         Content content = this.contentProvider.get(textWidget, textWidget.getCaretOffset());
         AIContext aiContext;
         if (target.isPreferSelection() && !content.selectionText.isBlank()) {
            aiContext = new AIContext(projectId, textWidget.getCaretOffset(), content.text, content.offset, path, content.selectionText, content.selectionOffset, sourceViewer.getDocument(), () -> (Boolean)Optional.ofNullable(new WeakReference(sourceViewer)).map((i) -> (SourceViewer)i.get()).map((i) -> i.getTextWidget()).map((i) -> i.isDisposed()).orElse(true));
         } else {
            aiContext = new AIContext(projectId, textWidget.getCaretOffset(), content.text, content.offset, path, content.text, content.offset, sourceViewer.getDocument(), () -> (Boolean)Optional.ofNullable(new WeakReference(sourceViewer)).map((i) -> (SourceViewer)i.get()).map((i) -> i.getTextWidget()).map((i) -> i.isDisposed()).orElse(true));
         }

         return this.contextInitializer.initialize(aiContext, target.getLimitSize());
      }
   }
}
