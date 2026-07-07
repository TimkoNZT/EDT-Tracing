package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class ContentSourceProvider implements IContentSourceProvider {
   private final ILog log;

   @Inject
   public ContentSourceProvider(ILog log) {
      Preconditions.checkNotNull(log);
      this.log = log;
   }

   public Optional<IFileDocument> getFileDocument(IFile file) {
      if (file == null) {
         return Optional.empty();
      } else {
         IDocument document = null;
         ITextEditor textEditor = null;

         IWorkbenchWindow[] var7;
         for(IWorkbenchWindow window : var7 = PlatformUI.getWorkbench().getWorkbenchWindows()) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
               IEditorReference[] var12;
               for(IEditorReference editorRef : var12 = page.getEditorReferences()) {
                  try {
                     IEditorPart editorPart = editorRef.getEditor(false);
                     if (editorPart != null) {
                        IEditorInput input = editorPart.getEditorInput();
                        if (input != null) {
                           IFile editorFile = (IFile)input.getAdapter(IFile.class);
                           if (editorFile != null && file.equals(editorFile)) {
                              ITextOperationTarget textOperationTarget = (ITextOperationTarget)editorPart.getAdapter(ITextOperationTarget.class);
                              if (textOperationTarget instanceof TextViewer) {
                                 TextViewer textViewer = (TextViewer)textOperationTarget;
                                 document = textViewer.getDocument();
                                 textEditor = (ITextEditor)editorPart.getAdapter(ITextEditor.class);
                                 break;
                              }
                           }
                        }
                     }
                  } catch (Exception error) {
                     this.log.logError(error);
                  }
               }

               if (document != null) {
                  break;
               }
            }
         }

         if (document != null) {
            return Optional.of(new FileDocument(document, textEditor, file, true));
         } else if (file.exists()) {
            try {
               byte[] bytes = file.getContents().readAllBytes();
               String content = new String(bytes, file.getCharset());
               IDocument var20 = new Document(content);
               return Optional.of(new FileDocument(var20, (ITextEditor)null, file, false));
            } catch (CoreException | IOException var18) {
               return Optional.empty();
            }
         } else {
            return Optional.empty();
         }
      }
   }
}
