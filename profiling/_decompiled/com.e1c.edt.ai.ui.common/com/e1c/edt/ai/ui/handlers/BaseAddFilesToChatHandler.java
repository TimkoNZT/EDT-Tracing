package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class BaseAddFilesToChatHandler extends AbstractHandler {
   @Inject
   IChat chat;
   @Inject
   IProjectIdProvider projectIdProvider;
   @Inject
   IFileSystem fileSystem;
   @Inject
   ISettings settings;
   @Inject
   IContentSourceProvider contentSourceProvider;
   @Inject
   IFiles files;
   @Inject
   ILog log;

   public BaseAddFilesToChatHandler() {
      BaseActivator.injectMembers(this);
   }

   public void setEnabled(Object evaluationContext) {
      try {
         if (!this.settings.isEnabled()) {
            this.setBaseEnabled(false);
            return;
         }

         if (evaluationContext instanceof ExpressionContext) {
            ExpressionContext expressionContext = (ExpressionContext)evaluationContext;
            Object elements = expressionContext.getDefaultVariable();
            if (elements instanceof List) {
               this.setBaseEnabled(!this.getContents((List)elements).isEmpty());
               return;
            }
         }

         this.setBaseEnabled(false);
      } catch (Exception e) {
         if (this.log != null) {
            this.log.logError(e);
         }

         this.setBaseEnabled(false);
      }

   }

   public Object execute(ExecutionEvent event) {
      try {
         ISelection selection = HandlerUtil.getCurrentSelection(event);
         if (selection == null || !(selection instanceof IStructuredSelection)) {
            return null;
         }

         IStructuredSelection structuredSelection = (IStructuredSelection)HandlerUtil.getCurrentSelection(event);
         List contents = this.getContents(structuredSelection.toList());
         if (!contents.isEmpty()) {
            this.chat.addFiles(contents);
         }
      } catch (Exception e) {
         if (this.log != null) {
            this.log.logError(e);
         }
      }

      return null;
   }

   private List<IFileDocument> getContents(List<Object> targets) {
      Stack<IFileDocument> contents = new Stack();
      if (targets != null && !targets.isEmpty()) {
         LinkedList<Object> elements = new LinkedList();
         elements.addAll(targets);

         while(elements.size() > 0) {
            Object element = elements.removeFirst();
            if (element instanceof EObject) {
               Optional<IFile> file = this.files.getCodeFile((EObject)element);
               if (file.isPresent()) {
                  Optional<IFileDocument> optionalContent = this.contentSourceProvider.getFileDocument((IFile)file.get());
                  if (optionalContent.isPresent()) {
                     contents.add((IFileDocument)optionalContent.get());
                  }
                  continue;
               }
            }

            if (element instanceof IFile) {
               IFile file = (IFile)element;
               if (!file.isHidden() && !file.isVirtual() && file.exists()) {
                  Optional<IFileDocument> optionalContent = this.contentSourceProvider.getFileDocument(file);
                  if (optionalContent.isPresent()) {
                     contents.add((IFileDocument)optionalContent.get());
                  }
               }
            } else if (element instanceof IContainer) {
               IContainer container = (IContainer)element;
               if (!container.isHidden() && !container.isVirtual() && !(container instanceof IProject)) {
                  try {
                     IResource[] var9;
                     for(IResource member : var9 = container.members()) {
                        elements.add(member);
                     }
                  } catch (CoreException var10) {
                  }
               }
            }
         }

         return contents;
      } else {
         return contents;
      }
   }
}
