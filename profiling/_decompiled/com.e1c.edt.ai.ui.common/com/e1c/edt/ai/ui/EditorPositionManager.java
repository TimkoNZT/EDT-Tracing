package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorPositionManager implements IEditorPositionManager {
   private static final String AI_CHAT = "AI Chat";
   private final ILog log;
   private final IDispatcher dispatcher;
   private final ISpecializedEditorOpener specializedEditorOpener;

   @Inject
   public EditorPositionManager(ILog log, IDispatcher dispatcher, ISpecializedEditorOpener specializedEditorOpener) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(specializedEditorOpener);
      this.log = log;
      this.dispatcher = dispatcher;
      this.specializedEditorOpener = specializedEditorOpener;
   }

   public void openFileInEditor(String filePath, IEdtLinkHandler.CursorPositionInfo cursorPosition, IEdtLinkHandler.SelectionInfo selection) {
      IEditorPart editor = null;

      try {
         IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
         IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

         try {
            IFile file = root.getFile(new Path(filePath));
            if (file != null && file.exists()) {
               editor = this.specializedEditorOpener.openInSpecializedEditor(page, file);
               if (editor == null) {
                  editor = IDE.openEditor(page, file, true);
               }
            }
         } catch (Exception e) {
            this.log.logError("workspace-relative branch threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
         }

         if (editor == null) {
            try {
               IPath osPath = Path.fromOSString(filePath);
               IFile fileForLocation = root.getFileForLocation(osPath);
               if (fileForLocation == null) {
                  IFile[] found = root.findFilesForLocationURI((new File(filePath)).toURI());

                  for(IFile f : found) {
                     if (fileForLocation == null && f.exists()) {
                        fileForLocation = f;
                     }
                  }
               }

               if (fileForLocation != null && fileForLocation.exists()) {
                  editor = this.specializedEditorOpener.openInSpecializedEditor(page, fileForLocation);
                  if (editor == null) {
                     editor = IDE.openEditor(page, fileForLocation, true);
                  }
               }
            } catch (Exception e) {
               this.log.logError("workspace-absolute branch threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
         }

         if (editor == null) {
            File externalFile = new File(filePath);
            if (!externalFile.exists() || !externalFile.isFile()) {
               this.log.logError("File not found: " + filePath);
               return;
            }

            IFileStore fileStore = EFS.getLocalFileSystem().getStore(externalFile.toURI());
            editor = IDE.openEditorOnFileStore(page, fileStore);
         }

         if (editor != null) {
            this.dispatcher.dispatchAsync(() -> {
               if (selection != null) {
                  this.restoreSelection(editor, selection);
               } else if (cursorPosition != null) {
                  this.restoreCursorPosition(editor, cursorPosition);
               }

            });
         }
      } catch (PartInitException e) {
         this.log.logError(e);
      } catch (Exception e) {
         this.log.logError(e);
      }

   }

   public void restoreCursorPosition(IEditorPart editor, IEdtLinkHandler.CursorPositionInfo cursorPosition) {
      this.log.trace("chat", "AI Chat", () -> "restoreCursorPosition: editor=" + editor.getClass().getSimpleName());
      IDocument document = null;
      ITextOperationTarget sourceViewer = null;
      if (editor instanceof ITextEditor) {
         ITextEditor textEditor = (ITextEditor)editor;
         IDocumentProvider documentProvider = textEditor.getDocumentProvider();
         if (documentProvider != null) {
            document = documentProvider.getDocument(textEditor.getEditorInput());
         }
      }

      if (document == null) {
         sourceViewer = (ITextOperationTarget)editor.getAdapter(ITextOperationTarget.class);
         if (sourceViewer != null && sourceViewer instanceof SourceViewer) {
            SourceViewer viewer = (SourceViewer)sourceViewer;
            document = viewer.getDocument();
            this.log.trace("chat", "AI Chat", () -> "Got document from SourceViewer adapter");
         }
      }

      if (document == null) {
         this.log.trace("chat", "AI Chat", () -> "Cannot get document from editor");
      } else {
         try {
            int numberOfLines = document.getNumberOfLines();
            this.log.logError("Document has " + numberOfLines + " lines");
            int line = Math.max(0, cursorPosition.getLine() - 1);
            int column = Math.max(0, cursorPosition.getColumn() - 1);
            if (line >= numberOfLines) {
               this.log.logError("Cursor line out of bounds: line=" + line + ", numberOfLines=" + numberOfLines);
               line = numberOfLines - 1;
               column = 0;
            }

            int offset = Math.max(0, Math.min(document.getLineOffset(line) + column, document.getLength()));
            this.log.logError("Setting cursor position: line=" + (line + 1) + ", column=" + (column + 1) + ", offset=" + offset + ", documentLength=" + document.getLength());
            TextSelection textSelection = new TextSelection(offset, 0);
            if (editor instanceof ITextEditor) {
               ITextEditor textEditor = (ITextEditor)editor;
               textEditor.getSelectionProvider().setSelection(textSelection);
            } else if (sourceViewer instanceof SourceViewer) {
               SourceViewer viewer = (SourceViewer)sourceViewer;
               viewer.getSelectionProvider().setSelection(textSelection);
            }
         } catch (BadLocationException e) {
            this.log.logError("BadLocationException when restoring cursor position: " + e.getMessage());

            try {
               TextSelection textSelection = new TextSelection(0, 0);
               if (editor instanceof ITextEditor) {
                  ITextEditor textEditor = (ITextEditor)editor;
                  textEditor.getSelectionProvider().setSelection(textSelection);
               } else if (sourceViewer instanceof SourceViewer) {
                  SourceViewer viewer = (SourceViewer)sourceViewer;
                  viewer.getSelectionProvider().setSelection(textSelection);
               }
            } catch (Exception ex) {
               this.log.logError("Failed to set fallback cursor position: " + ex.getMessage());
            }
         }

      }
   }

   public void restoreSelection(IEditorPart editor, IEdtLinkHandler.SelectionInfo selection) {
      this.log.trace("chat", "AI Chat", () -> "restoreSelection: editor=" + editor.getClass().getSimpleName());
      IDocument document = null;
      ITextOperationTarget sourceViewer = null;
      if (editor instanceof ITextEditor) {
         ITextEditor textEditor = (ITextEditor)editor;
         IDocumentProvider documentProvider = textEditor.getDocumentProvider();
         if (documentProvider != null) {
            document = documentProvider.getDocument(textEditor.getEditorInput());
         }
      }

      if (document == null) {
         sourceViewer = (ITextOperationTarget)editor.getAdapter(ITextOperationTarget.class);
         if (sourceViewer != null && sourceViewer instanceof SourceViewer) {
            SourceViewer viewer = (SourceViewer)sourceViewer;
            document = viewer.getDocument();
            this.log.trace("chat", "AI Chat", () -> "Got document from SourceViewer adapter");
         }
      }

      if (document == null) {
         this.log.trace("chat", "AI Chat", () -> "Cannot get document from editor");
      } else {
         try {
            int numberOfLines = document.getNumberOfLines();
            this.log.logError("Document has " + numberOfLines + " lines");
            int startLine = Math.max(0, selection.getStartLine() - 1);
            int startColumn = Math.max(0, selection.getStartColumn() - 1);
            int endLine = Math.max(0, selection.getEndLine() - 1);
            int endColumn = Math.max(0, selection.getEndColumn() - 1);
            if (endLine == numberOfLines - 1) {
               ++endColumn;
            }

            if (startLine >= numberOfLines || endLine >= numberOfLines) {
               this.log.logError("Selection lines out of bounds: startLine=" + startLine + ", endLine=" + endLine + ", numberOfLines=" + numberOfLines);
               startLine = Math.min(startLine, numberOfLines - 1);
               endLine = Math.min(endLine, numberOfLines - 1);
               if (endLine == numberOfLines - 1) {
                  try {
                     int lineLength = document.getLineLength(endLine);
                     endColumn = lineLength;
                  } catch (Exception e) {
                     this.log.logError("Error getting line length: " + e.getMessage());
                     endColumn = 0;
                  }
               }
            }

            int startOffset = Math.max(0, Math.min(document.getLineOffset(startLine) + startColumn, document.getLength()));
            int endOffset = Math.max(0, Math.min(document.getLineOffset(endLine) + endColumn, document.getLength()));
            int length = endOffset - startOffset;
            int finalStartOffset = length >= 0 ? startOffset : endOffset;
            int finalLength = Math.abs(length);
            this.log.logError("Setting selection: startOffset=" + finalStartOffset + ", length=" + finalLength + ", documentLength=" + document.getLength());
            if (finalLength >= 0) {
               TextSelection textSelection = new TextSelection(finalStartOffset, finalLength);
               if (editor instanceof ITextEditor) {
                  ITextEditor textEditor = (ITextEditor)editor;
                  textEditor.getSelectionProvider().setSelection(textSelection);
               } else if (sourceViewer instanceof SourceViewer) {
                  SourceViewer viewer = (SourceViewer)sourceViewer;
                  viewer.getSelectionProvider().setSelection(textSelection);
               }
            }
         } catch (BadLocationException e) {
            this.log.logError("BadLocationException when restoring selection: " + e.getMessage());

            try {
               TextSelection textSelection = new TextSelection(0, 0);
               if (editor instanceof ITextEditor) {
                  ITextEditor textEditor = (ITextEditor)editor;
                  textEditor.getSelectionProvider().setSelection(textSelection);
               } else if (sourceViewer instanceof SourceViewer) {
                  SourceViewer viewer = (SourceViewer)sourceViewer;
                  viewer.getSelectionProvider().setSelection(textSelection);
               }
            } catch (Exception ex) {
               this.log.logError("Failed to set fallback cursor position: " + ex.getMessage());
            }
         }

      }
   }
}
