package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class FileDocument implements IFileDocument {
   private final IDocument document;
   private final ITextEditor textEditor;
   private final IFile file;
   private final boolean isOpened;
   private int savedCursorOffset = -1;
   private int savedSelectionLength = 0;

   public FileDocument(IDocument document, ITextEditor textEditor, IFile file, boolean isOpened) {
      Preconditions.checkNotNull(document);
      Preconditions.checkNotNull(file);
      this.document = document;
      this.textEditor = textEditor;
      this.file = file;
      this.isOpened = isOpened;
   }

   public ProjectId getProjectId() {
      return new ProjectId(this.file.getProject());
   }

   public Charset getCharset() {
      try {
         return Charset.forName(this.file.getCharset());
      } catch (CoreException var2) {
         return Charset.forName(ResourcesPlugin.getEncoding());
      }
   }

   public IFile getFile() {
      return this.file;
   }

   public IDocument getDocument() {
      return this.document;
   }

   public ITextEditor getTextEditor() {
      return this.textEditor;
   }

   public void save() throws CoreException {
      if (this.isOpened) {
         ITextEditor textEditor = this.getTextEditor();
         if (textEditor != null) {
            this.saveCursorPosition();
            this.saveThroughEditor(textEditor);
            this.restoreCursorPosition();
            return;
         }
      }

      this.saveDirectly();
   }

   public void setContent(String content) {
      if (content == null) {
         content = "";
      }

      this.saveCursorPosition();

      try {
         this.document.set(content);
      } finally {
         this.restoreCursorPosition();
      }

   }

   private void saveThroughEditor(ITextEditor textEditor) throws CoreException {
      textEditor.doSave(new NullProgressMonitor());
   }

   private void saveDirectly() throws CoreException {
      String content = this.document.get();
      byte[] bytes = content.getBytes(this.getCharset());
      this.file.setContents(new ByteArrayInputStream(bytes), true, true, (IProgressMonitor)null);
   }

   public void delete() throws CoreException {
      if (this.isOpened) {
         ITextEditor textEditor = this.getTextEditor();
         if (textEditor != null) {
            this.deleteThroughEditor(textEditor);
            return;
         }
      }

      this.deleteDirectly();
   }

   private void deleteThroughEditor(ITextEditor textEditor) throws CoreException {
      textEditor.close(false);
      this.file.delete(true, new NullProgressMonitor());
   }

   private void deleteDirectly() throws CoreException {
      this.file.delete(true, new NullProgressMonitor());
   }

   private void saveCursorPosition() {
      if (this.textEditor != null) {
         try {
            ISelectionProvider selectionProvider = this.textEditor.getSelectionProvider();
            if (selectionProvider != null) {
               ISelection selection = selectionProvider.getSelection();
               if (selection instanceof ITextSelection) {
                  ITextSelection textSelection = (ITextSelection)selection;
                  this.savedCursorOffset = textSelection.getOffset();
                  this.savedSelectionLength = textSelection.getLength();
               }
            }
         } catch (Exception var4) {
            this.savedCursorOffset = -1;
            this.savedSelectionLength = 0;
         }
      }

   }

   private void restoreCursorPosition() {
      if (this.textEditor != null && this.savedCursorOffset >= 0) {
         try {
            ISelectionProvider selectionProvider = this.textEditor.getSelectionProvider();
            if (selectionProvider == null) {
               return;
            }

            IDocument currentDocument = this.textEditor.getDocumentProvider().getDocument(this.textEditor.getEditorInput());
            if (currentDocument == null) {
               return;
            }

            int documentLength = currentDocument.getLength();
            int cursorOffset = Math.min(this.savedCursorOffset, Math.max(0, documentLength));
            int selectionLength = Math.min(this.savedSelectionLength, Math.max(0, documentLength - cursorOffset));
            TextSelection newSelection = new TextSelection(currentDocument, cursorOffset, selectionLength);
            selectionProvider.setSelection(newSelection);
         } catch (Exception var7) {
         }
      }

   }
}
