package com._1c.g5.v8.dt.internal.debug.ui;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextEditor;
import com._1c.g5.v8.dt.core.platform.IExternalObjectProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.debug.core.model.BslModuleReference;
import com._1c.g5.v8.dt.debug.core.model.IBslModuleLocator;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com._1c.g5.v8.dt.debug.util.CrossReferenceFinder;
import com._1c.g5.v8.dt.internal.debug.ui.inlinedebug.IInlineDebuggerService;
import com._1c.g5.v8.dt.ui.util.Labeler;
import com._1c.g5.v8.dt.ui.util.OpenHelper;
import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.InstructionPointerManager;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupFacility;
import org.eclipse.debug.internal.ui.views.launch.Decoration;
import org.eclipse.debug.internal.ui.views.launch.DecorationManager;
import org.eclipse.debug.internal.ui.views.launch.StandardDecoration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugEditorPresentation;
import org.eclipse.debug.ui.IInstructionPointerPresentation;
import org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditorInput;
import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class BslSourceDisplay implements ISourceDisplay {
   @Inject
   private IV8ProjectManager v8projectManager;
   @Inject
   private IInlineDebuggerService inlineDebuggerService;
   @Inject
   private IBslModuleLocator moduleLocator;
   @Inject
   private OpenHelper openHelper;
   private final IInstructionPointerPresentation instructionPresentation = (IInstructionPointerPresentation)DebugUITools.newDebugModelPresentation();

   public BslSourceDisplay() {
      SourceLookupFacility.getDefault();
   }

   public void displaySource(Object element, IWorkbenchPage page, boolean forceSourceLookup) {
      if (element instanceof IBslStackFrame stackFrame) {
         BslModuleReference reference = stackFrame.getReference();
         Module module = reference == null ? null : this.moduleLocator.getModule(reference, false);
         if (module != null) {
            this.displayModule(element, page, stackFrame, module, forceSourceLookup);
         } else if (reference != null) {
            List<Module> foundModules = (List)this.v8projectManager.getProjects(IExternalObjectProject.class).stream().map((project) -> this.findModule(reference, project)).filter(Objects::nonNull).collect(Collectors.toList());
            if (!foundModules.isEmpty()) {
               if (foundModules.size() == 1) {
                  this.displayModule(element, page, stackFrame, (Module)foundModules.get(0), forceSourceLookup);
               } else {
                  ILabelProvider labelProvider = new ExternalObjcectModuleLabelProvider();
                  ElementListSelectionDialog dialog = new ElementListSelectionDialog(page.getWorkbenchWindow().getShell(), labelProvider);
                  dialog.setElements(foundModules.toArray(new Module[foundModules.size()]));
                  dialog.setTitle(Messages.BslSourceDisplay_External_object_modules);
                  dialog.setMessage(Messages.BslSourceDisplay_Select_external_object_module_to_debug);
                  dialog.setMultipleSelection(false);
                  int result = dialog.open();
                  labelProvider.dispose();
                  if (result == 0) {
                     this.displayModule(element, page, stackFrame, (Module)dialog.getFirstResult(), forceSourceLookup);
                  }
               }
            } else {
               SourceLookupFacility.getDefault().displaySource(element, page, forceSourceLookup);
            }
         } else {
            SourceLookupFacility.getDefault().displaySource(element, page, forceSourceLookup);
         }
      }

   }

   protected void displayModule(Object element, IWorkbenchPage page, IBslStackFrame stackFrame, Module module, boolean forceSourceLookup) {
      EObject owner = module.getOwner();
      EStructuralFeature reference;
      if (owner == null) {
         reference = null;
      } else {
         synchronized(owner.eResource().getResourceSet()) {
            reference = CrossReferenceFinder.findCrossReference(owner, module);
         }
      }

      SourceDisplayJob sourceDisplay = new SourceDisplayJob(page, owner, reference, stackFrame, forceSourceLookup);
      Job.getJobManager().cancel(sourceDisplay);
      sourceDisplay.schedule();
   }

   protected void displayModule(EObject moduleOwner, EStructuralFeature reference, IWorkbenchPage page, IBslStackFrame stackFrame, boolean forceSourceLookup) {
      IEditorPart editor = this.openModuleEditor(moduleOwner, reference, page, stackFrame, forceSourceLookup);
      IDebugEditorPresentation editorPresentation = this.getEditorPresentation();
      if (editorPresentation.addAnnotations(editor, stackFrame)) {
         Decoration decoration = new StandardDecoration(editorPresentation, editor, stackFrame.getThread());
         DecorationManager.addDecoration(decoration);
      } else {
         ITextEditor textEditor = null;
         if (editor instanceof ITextEditor) {
            textEditor = (ITextEditor)editor;
         } else {
            textEditor = (ITextEditor)editor.getAdapter(ITextEditor.class);
         }

         if (textEditor != null) {
            this.positionEditor(textEditor, stackFrame);
            InstructionPointerManager.getDefault().removeAnnotations(textEditor);
            Annotation annotation = this.instructionPresentation.getInstructionPointerAnnotation(textEditor, stackFrame);
            InstructionPointerManager.getDefault().addAnnotation(textEditor, stackFrame, annotation);
            if (textEditor instanceof BslXtextEditor && this.inlineDebuggerService.isEnabled()) {
               Display display = page.getActivePart().getSite().getShell().getDisplay();
               BslXtextEditor bslEditor = (BslXtextEditor)textEditor;
               display.asyncExec(() -> this.inlineDebuggerService.displayInlineDebugging(bslEditor, stackFrame));
            }
         }
      }

   }

   protected IEditorPart openModuleEditor(EObject moduleOwner, EStructuralFeature reference, IWorkbenchPage page, IBslStackFrame stackFrame, boolean forceSourceLookup) {
      return this.openHelper.openEditor(moduleOwner, reference);
   }

   protected void openSourceNotFoundEditor(IWorkbenchPage page, Object element) {
      IEditorInput input = new CommonSourceNotFoundEditorInput(element);
      String editorId = "org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor";
      IEditorReference[] references = page.findEditors((IEditorInput)null, editorId, 2);
      if (references.length > 0) {
         IEditorPart found = references[0].getEditor(false);
         if (found instanceof IReusableEditor) {
            page.bringToTop(found);
            page.reuseEditor((IReusableEditor)found, input);
            return;
         }
      }

      try {
         page.openEditor(input, editorId, false, 3);
      } catch (PartInitException e) {
         DebugUiPlugin.log((Throwable)e);
      }

   }

   protected IDebugEditorPresentation getEditorPresentation() {
      return (IDebugEditorPresentation)DebugUIPlugin.getModelPresentation();
   }

   protected IRegion getLineInformation(ITextEditor editor, int lineNumber) {
      IDocumentProvider provider = editor.getDocumentProvider();
      IEditorInput input = editor.getEditorInput();

      try {
         provider.connect(input);
      } catch (CoreException var11) {
         return null;
      }

      IRegion var7;
      try {
         IDocument document = provider.getDocument(input);
         if (document == null) {
            return null;
         }

         var7 = document.getLineInformation(lineNumber);
      } catch (BadLocationException var12) {
         return null;
      } finally {
         provider.disconnect(input);
      }

      return var7;
   }

   protected void positionEditor(ITextEditor editor, IStackFrame frame) {
      try {
         int charStart = frame.getCharStart();
         if (charStart >= 0) {
            editor.selectAndReveal(charStart, 0);
            return;
         }

         int lineNumber = frame.getLineNumber();
         --lineNumber;
         IRegion region = this.getLineInformation(editor, lineNumber);
         if (region != null) {
            editor.selectAndReveal(region.getOffset(), 0);
         }
      } catch (DebugException e) {
         DebugUiPlugin.log((Throwable)e);
      }

   }

   protected void clearSourceSelection(Object source) {
      if (source instanceof IThread thread) {
         DecorationManager.removeDecorations(thread);
         InstructionPointerManager.getDefault().removeAnnotations(thread);
      } else if (source instanceof IDebugTarget target) {
         DecorationManager.removeDecorations(target);
         InstructionPointerManager.getDefault().removeAnnotations(target);
      }

   }

   private Module findModule(BslModuleReference reference, IExternalObjectProject project) {
      BslModuleReference projectAwareReferece = new BslModuleReference(reference.getParentUuid(), reference.getPropertyUuid(), project.getProject());
      return this.moduleLocator.getModule(projectAwareReferece, false);
   }

   protected class SourceDisplayJob extends UIJob {
      private final IWorkbenchPage page;
      private final EStructuralFeature reference;
      private final IBslStackFrame stackFrame;
      private final EObject owner;
      private final boolean forceSourceLookup;

      protected SourceDisplayJob(IWorkbenchPage page, EObject owner, EStructuralFeature reference, IBslStackFrame stackFrame, boolean forceSourceLookup) {
         super("Debug Source Display");
         this.setSystem(true);
         this.setPriority(10);
         this.page = page;
         this.owner = owner;
         this.reference = reference;
         this.stackFrame = stackFrame;
         this.forceSourceLookup = forceSourceLookup;
      }

      public IStatus runInUIThread(IProgressMonitor monitor) {
         if (!monitor.isCanceled()) {
            BslSourceDisplay.this.displayModule(this.owner, this.reference, this.page, this.stackFrame, this.forceSourceLookup);
         }

         if (monitor.isCanceled()) {
            BslSourceDisplay.this.clearSourceSelection(this.stackFrame.getThread());
         }

         return Status.OK_STATUS;
      }

      public boolean belongsTo(Object family) {
         if (family instanceof SourceDisplayJob other) {
            return other.page.equals(this.page);
         } else {
            return false;
         }
      }
   }

   protected static class ExternalObjcectModuleLabelProvider extends LabelProvider {
      public String getText(Object element) {
         return element instanceof Module ? Labeler.path(element, '.').skipCommonNode().stopAfter(IProject.class).label() : super.getText(element);
      }
   }
}
