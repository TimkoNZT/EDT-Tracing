Compiled from "BslSourceDisplay.java"
public class com._1c.g5.v8.dt.internal.debug.ui.BslSourceDisplay implements org.eclipse.debug.ui.sourcelookup.ISourceDisplay {
  private com._1c.g5.v8.dt.core.platform.IV8ProjectManager v8projectManager;

  private com._1c.g5.v8.dt.internal.debug.ui.inlinedebug.IInlineDebuggerService inlineDebuggerService;

  private com._1c.g5.v8.dt.debug.core.model.IBslModuleLocator moduleLocator;

  private com._1c.g5.v8.dt.ui.util.OpenHelper openHelper;

  private final org.eclipse.debug.ui.IInstructionPointerPresentation instructionPresentation;

  public com._1c.g5.v8.dt.internal.debug.ui.BslSourceDisplay();
    Code:
       0: aload_0
       1: invokespecial #22                 // Method java/lang/Object."<init>":()V
       4: aload_0
       5: invokestatic  #24                 // Method org/eclipse/debug/ui/DebugUITools.newDebugModelPresentation:()Lorg/eclipse/debug/ui/IDebugModelPresentation;
       8: checkcast     #30                 // class org/eclipse/debug/ui/IInstructionPointerPresentation
      11: putfield      #32                 // Field instructionPresentation:Lorg/eclipse/debug/ui/IInstructionPointerPresentation;
      14: invokestatic  #34                 // Method org/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility.getDefault:()Lorg/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility;
      17: pop
      18: return

  public void displaySource(java.lang.Object, org.eclipse.ui.IWorkbenchPage, boolean);
    Code:
       0: aload_1
       1: instanceof    #46                 // class com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame
       4: ifeq          300
       7: aload_1
       8: checkcast     #46                 // class com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame
      11: astore        4
      13: aload         4
      15: invokeinterface #48,  1           // InterfaceMethod com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame.getReference:()Lcom/_1c/g5/v8/dt/debug/core/model/BslModuleReference;
      20: astore        5
      22: aload         5
      24: ifnonnull     31
      27: aconst_null
      28: goto          43
      31: aload_0
      32: getfield      #52                 // Field moduleLocator:Lcom/_1c/g5/v8/dt/debug/core/model/IBslModuleLocator;
      35: aload         5
      37: iconst_0
      38: invokeinterface #54,  3           // InterfaceMethod com/_1c/g5/v8/dt/debug/core/model/IBslModuleLocator.getModule:(Lcom/_1c/g5/v8/dt/debug/core/model/BslModuleReference;Z)Lcom/_1c/g5/v8/dt/bsl/model/Module;
      43: astore        6
      45: aload         6
      47: ifnull        64
      50: aload_0
      51: aload_1
      52: aload_2
      53: aload         4
      55: aload         6
      57: iload_3
      58: invokevirtual #60                 // Method displayModule:(Ljava/lang/Object;Lorg/eclipse/ui/IWorkbenchPage;Lcom/_1c/g5/v8/dt/debug/core/model/IBslStackFrame;Lcom/_1c/g5/v8/dt/bsl/model/Module;Z)V
      61: goto          300
      64: aload         5
      66: ifnull        291
      69: aload_0
      70: getfield      #64                 // Field v8projectManager:Lcom/_1c/g5/v8/dt/core/platform/IV8ProjectManager;
      73: ldc           #66                 // class com/_1c/g5/v8/dt/core/platform/IExternalObjectProject
      75: invokeinterface #68,  2           // InterfaceMethod com/_1c/g5/v8/dt/core/platform/IV8ProjectManager.getProjects:(Ljava/lang/Class;)Ljava/util/Collection;
      80: invokeinterface #74,  1           // InterfaceMethod java/util/Collection.stream:()Ljava/util/stream/Stream;
      85: aload_0
      86: aload         5
      88: invokedynamic #80,  0             // InvokeDynamic #0:apply:(Lcom/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay;Lcom/_1c/g5/v8/dt/debug/core/model/BslModuleReference;)Ljava/util/function/Function;
      93: invokeinterface #84,  2           // InterfaceMethod java/util/stream/Stream.map:(Ljava/util/function/Function;)Ljava/util/stream/Stream;
      98: invokedynamic #90,  0             // InvokeDynamic #1:test:()Ljava/util/function/Predicate;
     103: invokeinterface #94,  2           // InterfaceMethod java/util/stream/Stream.filter:(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;
     108: invokestatic  #98                 // Method java/util/stream/Collectors.toList:()Ljava/util/stream/Collector;
     111: invokeinterface #104,  2          // InterfaceMethod java/util/stream/Stream.collect:(Ljava/util/stream/Collector;)Ljava/lang/Object;
     116: checkcast     #108                // class java/util/List
     119: astore        7
     121: aload         7
     123: invokeinterface #110,  1          // InterfaceMethod java/util/List.isEmpty:()Z
     128: ifne          279
     131: aload         7
     133: invokeinterface #114,  1          // InterfaceMethod java/util/List.size:()I
     138: iconst_1
     139: if_icmpne     165
     142: aload_0
     143: aload_1
     144: aload_2
     145: aload         4
     147: aload         7
     149: iconst_0
     150: invokeinterface #118,  2          // InterfaceMethod java/util/List.get:(I)Ljava/lang/Object;
     155: checkcast     #122                // class com/_1c/g5/v8/dt/bsl/model/Module
     158: iload_3
     159: invokevirtual #60                 // Method displayModule:(Ljava/lang/Object;Lorg/eclipse/ui/IWorkbenchPage;Lcom/_1c/g5/v8/dt/debug/core/model/IBslStackFrame;Lcom/_1c/g5/v8/dt/bsl/model/Module;Z)V
     162: goto          300
     165: new           #124                // class com/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay$ExternalObjcectModuleLabelProvider
     168: dup
     169: invokespecial #126                // Method com/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay$ExternalObjcectModuleLabelProvider."<init>":()V
     172: astore        8
     174: new           #127                // class org/eclipse/ui/dialogs/ElementListSelectionDialog
     177: dup
     178: aload_2
     179: invokeinterface #129,  1          // InterfaceMethod org/eclipse/ui/IWorkbenchPage.getWorkbenchWindow:()Lorg/eclipse/ui/IWorkbenchWindow;
     184: invokeinterface #135,  1          // InterfaceMethod org/eclipse/ui/IWorkbenchWindow.getShell:()Lorg/eclipse/swt/widgets/Shell;
     189: aload         8
     191: invokespecial #141                // Method org/eclipse/ui/dialogs/ElementListSelectionDialog."<init>":(Lorg/eclipse/swt/widgets/Shell;Lorg/eclipse/jface/viewers/ILabelProvider;)V
     194: astore        9
     196: aload         9
     198: aload         7
     200: aload         7
     202: invokeinterface #114,  1          // InterfaceMethod java/util/List.size:()I
     207: anewarray     #122                // class com/_1c/g5/v8/dt/bsl/model/Module
     210: invokeinterface #144,  2          // InterfaceMethod java/util/List.toArray:([Ljava/lang/Object;)[Ljava/lang/Object;
     215: invokevirtual #148                // Method org/eclipse/ui/dialogs/ElementListSelectionDialog.setElements:([Ljava/lang/Object;)V
     218: aload         9
     220: getstatic     #152                // Field com/_1c/g5/v8/dt/internal/debug/ui/Messages.BslSourceDisplay_External_object_modules:Ljava/lang/String;
     223: invokevirtual #158                // Method org/eclipse/ui/dialogs/ElementListSelectionDialog.setTitle:(Ljava/lang/String;)V
     226: aload         9
     228: getstatic     #162                // Field com/_1c/g5/v8/dt/internal/debug/ui/Messages.BslSourceDisplay_Select_external_object_module_to_debug:Ljava/lang/String;
     231: invokevirtual #165                // Method org/eclipse/ui/dialogs/ElementListSelectionDialog.setMessage:(Ljava/lang/String;)V
     234: aload         9
     236: iconst_0
     237: invokevirtual #168                // Method org/eclipse/ui/dialogs/ElementListSelectionDialog.setMultipleSelection:(Z)V
     240: aload         9
     242: invokevirtual #172                // Method org/eclipse/ui/dialogs/ElementListSelectionDialog.open:()I
     245: istore        10
     247: aload         8
     249: invokeinterface #175,  1          // InterfaceMethod org/eclipse/jface/viewers/ILabelProvider.dispose:()V
     254: iload         10
     256: ifne          300
     259: aload_0
     260: aload_1
     261: aload_2
     262: aload         4
     264: aload         9
     266: invokevirtual #180                // Method org/eclipse/ui/dialogs/ElementListSelectionDialog.getFirstResult:()Ljava/lang/Object;
     269: checkcast     #122                // class com/_1c/g5/v8/dt/bsl/model/Module
     272: iload_3
     273: invokevirtual #60                 // Method displayModule:(Ljava/lang/Object;Lorg/eclipse/ui/IWorkbenchPage;Lcom/_1c/g5/v8/dt/debug/core/model/IBslStackFrame;Lcom/_1c/g5/v8/dt/bsl/model/Module;Z)V
     276: goto          300
     279: invokestatic  #34                 // Method org/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility.getDefault:()Lorg/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility;
     282: aload_1
     283: aload_2
     284: iload_3
     285: invokevirtual #184                // Method org/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility.displaySource:(Ljava/lang/Object;Lorg/eclipse/ui/IWorkbenchPage;Z)V
     288: goto          300
     291: invokestatic  #34                 // Method org/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility.getDefault:()Lorg/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility;
     294: aload_1
     295: aload_2
     296: iload_3
     297: invokevirtual #184                // Method org/eclipse/debug/internal/ui/sourcelookup/SourceLookupFacility.displaySource:(Ljava/lang/Object;Lorg/eclipse/ui/IWorkbenchPage;Z)V
     300: return

  protected void displayModule(java.lang.Object, org.eclipse.ui.IWorkbenchPage, com._1c.g5.v8.dt.debug.core.model.IBslStackFrame, com._1c.g5.v8.dt.bsl.model.Module, boolean);
    Code:
       0: aload         4
       2: invokeinterface #211,  1          // InterfaceMethod com/_1c/g5/v8/dt/bsl/model/Module.getOwner:()Lorg/eclipse/emf/ecore/EObject;
       7: astore        6
       9: aload         6
      11: ifnonnull     20
      14: aconst_null
      15: astore        7
      17: goto          55
      20: aload         6
      22: invokeinterface #215,  1          // InterfaceMethod org/eclipse/emf/ecore/EObject.eResource:()Lorg/eclipse/emf/ecore/resource/Resource;
      27: invokeinterface #221,  1          // InterfaceMethod org/eclipse/emf/ecore/resource/Resource.getResourceSet:()Lorg/eclipse/emf/ecore/resource/ResourceSet;
      32: dup
      33: astore        8
      35: monitorenter
      36: aload         6
      38: aload         4
      40: invokestatic  #227                // Method com/_1c/g5/v8/dt/debug/util/CrossReferenceFinder.findCrossReference:(Lorg/eclipse/emf/ecore/EObject;Lorg/eclipse/emf/ecore/EObject;)Lorg/eclipse/emf/ecore/EReference;
      43: astore        7
      45: aload         8
      47: monitorexit
      48: goto          55
      51: aload         8
      53: monitorexit
      54: athrow
      55: new           #233                // class com/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay$SourceDisplayJob
      58: dup
      59: aload_0
      60: aload_2
      61: aload         6
      63: aload         7
      65: aload_3
      66: iload         5
      68: invokespecial #235                // Method com/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay$SourceDisplayJob."<init>":(Lcom/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay;Lorg/eclipse/ui/IWorkbenchPage;Lorg/eclipse/emf/ecore/EObject;Lorg/eclipse/emf/ecore/EStructuralFeature;Lcom/_1c/g5/v8/dt/debug/core/model/IBslStackFrame;Z)V
      71: astore        8
      73: invokestatic  #238                // Method org/eclipse/core/runtime/jobs/Job.getJobManager:()Lorg/eclipse/core/runtime/jobs/IJobManager;
      76: aload         8
      78: invokeinterface #244,  2          // InterfaceMethod org/eclipse/core/runtime/jobs/IJobManager.cancel:(Ljava/lang/Object;)V
      83: aload         8
      85: invokevirtual #250                // Method com/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay$SourceDisplayJob.schedule:()V
      88: return
    Exception table:
       from    to  target type
          36    48    51   any
          51    54    51   any

  protected void displayModule(org.eclipse.emf.ecore.EObject, org.eclipse.emf.ecore.EStructuralFeature, org.eclipse.ui.IWorkbenchPage, com._1c.g5.v8.dt.debug.core.model.IBslStackFrame, boolean);
    Code:
       0: aload_0
       1: aload_1
       2: aload_2
       3: aload_3
       4: aload         4
       6: iload         5
       8: invokevirtual #265                // Method openModuleEditor:(Lorg/eclipse/emf/ecore/EObject;Lorg/eclipse/emf/ecore/EStructuralFeature;Lorg/eclipse/ui/IWorkbenchPage;Lcom/_1c/g5/v8/dt/debug/core/model/IBslStackFrame;Z)Lorg/eclipse/ui/IEditorPart;
      11: astore        6
      13: aload_0
      14: invokevirtual #269                // Method getEditorPresentation:()Lorg/eclipse/debug/ui/IDebugEditorPresentation;
      17: astore        7
      19: aload         7
      21: aload         6
      23: aload         4
      25: invokeinterface #273,  3          // InterfaceMethod org/eclipse/debug/ui/IDebugEditorPresentation.addAnnotations:(Lorg/eclipse/ui/IEditorPart;Lorg/eclipse/debug/core/model/IStackFrame;)Z
      30: ifeq          61
      33: new           #279                // class org/eclipse/debug/internal/ui/views/launch/StandardDecoration
      36: dup
      37: aload         7
      39: aload         6
      41: aload         4
      43: invokeinterface #281,  1          // InterfaceMethod com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame.getThread:()Lcom/_1c/g5/v8/dt/debug/core/model/IRuntimeDebugTargetThread;
      48: invokespecial #285                // Method org/eclipse/debug/internal/ui/views/launch/StandardDecoration."<init>":(Lorg/eclipse/debug/ui/IDebugEditorPresentation;Lorg/eclipse/ui/IEditorPart;Lorg/eclipse/debug/core/model/IThread;)V
      51: astore        8
      53: aload         8
      55: invokestatic  #288                // Method org/eclipse/debug/internal/ui/views/launch/DecorationManager.addDecoration:(Lorg/eclipse/debug/internal/ui/views/launch/Decoration;)V
      58: goto          208
      61: aconst_null
      62: astore        8
      64: aload         6
      66: instanceof    #294                // class org/eclipse/ui/texteditor/ITextEditor
      69: ifeq          82
      72: aload         6
      74: checkcast     #294                // class org/eclipse/ui/texteditor/ITextEditor
      77: astore        8
      79: goto          97
      82: aload         6
      84: ldc_w         #294                // class org/eclipse/ui/texteditor/ITextEditor
      87: invokeinterface #296,  2          // InterfaceMethod org/eclipse/ui/IEditorPart.getAdapter:(Ljava/lang/Class;)Ljava/lang/Object;
      92: checkcast     #294                // class org/eclipse/ui/texteditor/ITextEditor
      95: astore        8
      97: aload         8
      99: ifnull        208
     102: aload_0
     103: aload         8
     105: aload         4
     107: invokevirtual #302                // Method positionEditor:(Lorg/eclipse/ui/texteditor/ITextEditor;Lorg/eclipse/debug/core/model/IStackFrame;)V
     110: invokestatic  #306                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.getDefault:()Lorg/eclipse/debug/internal/ui/InstructionPointerManager;
     113: aload         8
     115: invokevirtual #311                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.removeAnnotations:(Lorg/eclipse/ui/texteditor/ITextEditor;)V
     118: aload_0
     119: getfield      #32                 // Field instructionPresentation:Lorg/eclipse/debug/ui/IInstructionPointerPresentation;
     122: aload         8
     124: aload         4
     126: invokeinterface #315,  3          // InterfaceMethod org/eclipse/debug/ui/IInstructionPointerPresentation.getInstructionPointerAnnotation:(Lorg/eclipse/ui/IEditorPart;Lorg/eclipse/debug/core/model/IStackFrame;)Lorg/eclipse/jface/text/source/Annotation;
     131: astore        9
     133: invokestatic  #306                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.getDefault:()Lorg/eclipse/debug/internal/ui/InstructionPointerManager;
     136: aload         8
     138: aload         4
     140: aload         9
     142: invokevirtual #319                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.addAnnotation:(Lorg/eclipse/ui/texteditor/ITextEditor;Lorg/eclipse/debug/core/model/IStackFrame;Lorg/eclipse/jface/text/source/Annotation;)V
     145: aload         8
     147: instanceof    #323                // class com/_1c/g5/v8/dt/bsl/ui/editor/BslXtextEditor
     150: ifeq          208
     153: aload_0
     154: getfield      #325                // Field inlineDebuggerService:Lcom/_1c/g5/v8/dt/internal/debug/ui/inlinedebug/IInlineDebuggerService;
     157: invokeinterface #327,  1          // InterfaceMethod com/_1c/g5/v8/dt/internal/debug/ui/inlinedebug/IInlineDebuggerService.isEnabled:()Z
     162: ifeq          208
     165: aload_3
     166: invokeinterface #332,  1          // InterfaceMethod org/eclipse/ui/IWorkbenchPage.getActivePart:()Lorg/eclipse/ui/IWorkbenchPart;
     171: invokeinterface #336,  1          // InterfaceMethod org/eclipse/ui/IWorkbenchPart.getSite:()Lorg/eclipse/ui/IWorkbenchPartSite;
     176: invokeinterface #342,  1          // InterfaceMethod org/eclipse/ui/IWorkbenchPartSite.getShell:()Lorg/eclipse/swt/widgets/Shell;
     181: invokevirtual #345                // Method org/eclipse/swt/widgets/Shell.getDisplay:()Lorg/eclipse/swt/widgets/Display;
     184: astore        10
     186: aload         8
     188: checkcast     #323                // class com/_1c/g5/v8/dt/bsl/ui/editor/BslXtextEditor
     191: astore        11
     193: aload         10
     195: aload_0
     196: aload         11
     198: aload         4
     200: invokedynamic #351,  0            // InvokeDynamic #2:run:(Lcom/_1c/g5/v8/dt/internal/debug/ui/BslSourceDisplay;Lcom/_1c/g5/v8/dt/bsl/ui/editor/BslXtextEditor;Lcom/_1c/g5/v8/dt/debug/core/model/IBslStackFrame;)Ljava/lang/Runnable;
     205: invokevirtual #355                // Method org/eclipse/swt/widgets/Display.asyncExec:(Ljava/lang/Runnable;)V
     208: return

  protected org.eclipse.ui.IEditorPart openModuleEditor(org.eclipse.emf.ecore.EObject, org.eclipse.emf.ecore.EStructuralFeature, org.eclipse.ui.IWorkbenchPage, com._1c.g5.v8.dt.debug.core.model.IBslStackFrame, boolean);
    Code:
       0: aload_0
       1: getfield      #376                // Field openHelper:Lcom/_1c/g5/v8/dt/ui/util/OpenHelper;
       4: aload_1
       5: aload_2
       6: invokevirtual #378                // Method com/_1c/g5/v8/dt/ui/util/OpenHelper.openEditor:(Lorg/eclipse/emf/ecore/EObject;Lorg/eclipse/emf/ecore/EStructuralFeature;)Lorg/eclipse/ui/IEditorPart;
       9: areturn

  protected void openSourceNotFoundEditor(org.eclipse.ui.IWorkbenchPage, java.lang.Object);
    Code:
       0: new           #386                // class org/eclipse/debug/ui/sourcelookup/CommonSourceNotFoundEditorInput
       3: dup
       4: aload_2
       5: invokespecial #388                // Method org/eclipse/debug/ui/sourcelookup/CommonSourceNotFoundEditorInput."<init>":(Ljava/lang/Object;)V
       8: astore_3
       9: ldc_w         #390                // String org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor
      12: astore        4
      14: aload_1
      15: aconst_null
      16: aload         4
      18: iconst_2
      19: invokeinterface #392,  4          // InterfaceMethod org/eclipse/ui/IWorkbenchPage.findEditors:(Lorg/eclipse/ui/IEditorInput;Ljava/lang/String;I)[Lorg/eclipse/ui/IEditorReference;
      24: astore        5
      26: aload         5
      28: arraylength
      29: ifle          73
      32: aload         5
      34: iconst_0
      35: aaload
      36: iconst_0
      37: invokeinterface #396,  2          // InterfaceMethod org/eclipse/ui/IEditorReference.getEditor:(Z)Lorg/eclipse/ui/IEditorPart;
      42: astore        6
      44: aload         6
      46: instanceof    #402                // class org/eclipse/ui/IReusableEditor
      49: ifeq          73
      52: aload_1
      53: aload         6
      55: invokeinterface #404,  2          // InterfaceMethod org/eclipse/ui/IWorkbenchPage.bringToTop:(Lorg/eclipse/ui/IWorkbenchPart;)V
      60: aload_1
      61: aload         6
      63: checkcast     #402                // class org/eclipse/ui/IReusableEditor
      66: aload_3
      67: invokeinterface #408,  3          // InterfaceMethod org/eclipse/ui/IWorkbenchPage.reuseEditor:(Lorg/eclipse/ui/IReusableEditor;Lorg/eclipse/ui/IEditorInput;)V
      72: return
      73: aload_1
      74: aload_3
      75: aload         4
      77: iconst_0
      78: iconst_3
      79: invokeinterface #412,  5          // InterfaceMethod org/eclipse/ui/IWorkbenchPage.openEditor:(Lorg/eclipse/ui/IEditorInput;Ljava/lang/String;ZI)Lorg/eclipse/ui/IEditorPart;
      84: pop
      85: goto          95
      88: astore        6
      90: aload         6
      92: invokestatic  #415                // Method com/_1c/g5/v8/dt/internal/debug/ui/DebugUiPlugin.log:(Ljava/lang/Throwable;)V
      95: return
    Exception table:
       from    to  target type
          73    85    88   Class org/eclipse/ui/PartInitException

  protected org.eclipse.debug.ui.IDebugEditorPresentation getEditorPresentation();
    Code:
       0: invokestatic  #436                // Method org/eclipse/debug/internal/ui/DebugUIPlugin.getModelPresentation:()Lorg/eclipse/debug/ui/IDebugModelPresentation;
       3: checkcast     #274                // class org/eclipse/debug/ui/IDebugEditorPresentation
       6: areturn

  protected org.eclipse.jface.text.IRegion getLineInformation(org.eclipse.ui.texteditor.ITextEditor, int);
    Code:
       0: aload_1
       1: invokeinterface #443,  1          // InterfaceMethod org/eclipse/ui/texteditor/ITextEditor.getDocumentProvider:()Lorg/eclipse/ui/texteditor/IDocumentProvider;
       6: astore_3
       7: aload_1
       8: invokeinterface #447,  1          // InterfaceMethod org/eclipse/ui/texteditor/ITextEditor.getEditorInput:()Lorg/eclipse/ui/IEditorInput;
      13: astore        4
      15: aload_3
      16: aload         4
      18: invokeinterface #451,  2          // InterfaceMethod org/eclipse/ui/texteditor/IDocumentProvider.connect:(Ljava/lang/Object;)V
      23: goto          30
      26: astore        5
      28: aconst_null
      29: areturn
      30: aload_3
      31: aload         4
      33: invokeinterface #456,  2          // InterfaceMethod org/eclipse/ui/texteditor/IDocumentProvider.getDocument:(Ljava/lang/Object;)Lorg/eclipse/jface/text/IDocument;
      38: astore        5
      40: aload         5
      42: ifnull        66
      45: aload         5
      47: iload_2
      48: invokeinterface #460,  2          // InterfaceMethod org/eclipse/jface/text/IDocument.getLineInformation:(I)Lorg/eclipse/jface/text/IRegion;
      53: astore        7
      55: aload_3
      56: aload         4
      58: invokeinterface #465,  2          // InterfaceMethod org/eclipse/ui/texteditor/IDocumentProvider.disconnect:(Ljava/lang/Object;)V
      63: aload         7
      65: areturn
      66: aload_3
      67: aload         4
      69: invokeinterface #465,  2          // InterfaceMethod org/eclipse/ui/texteditor/IDocumentProvider.disconnect:(Ljava/lang/Object;)V
      74: aconst_null
      75: areturn
      76: astore        5
      78: aload_3
      79: aload         4
      81: invokeinterface #465,  2          // InterfaceMethod org/eclipse/ui/texteditor/IDocumentProvider.disconnect:(Ljava/lang/Object;)V
      86: aconst_null
      87: areturn
      88: astore        6
      90: aload_3
      91: aload         4
      93: invokeinterface #465,  2          // InterfaceMethod org/eclipse/ui/texteditor/IDocumentProvider.disconnect:(Ljava/lang/Object;)V
      98: aload         6
     100: athrow
    Exception table:
       from    to  target type
          15    23    26   Class org/eclipse/core/runtime/CoreException
          30    55    76   Class org/eclipse/jface/text/BadLocationException
          30    55    88   any
          76    78    88   any

  protected void positionEditor(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.debug.core.model.IStackFrame);
    Code:
       0: aload_2
       1: invokeinterface #479,  1          // InterfaceMethod org/eclipse/debug/core/model/IStackFrame.getCharStart:()I
       6: istore_3
       7: iload_3
       8: iflt          20
      11: aload_1
      12: iload_3
      13: iconst_0
      14: invokeinterface #484,  3          // InterfaceMethod org/eclipse/ui/texteditor/ITextEditor.selectAndReveal:(II)V
      19: return
      20: aload_2
      21: invokeinterface #488,  1          // InterfaceMethod org/eclipse/debug/core/model/IStackFrame.getLineNumber:()I
      26: istore        4
      28: iinc          4, -1
      31: aload_0
      32: aload_1
      33: iload         4
      35: invokevirtual #491                // Method getLineInformation:(Lorg/eclipse/ui/texteditor/ITextEditor;I)Lorg/eclipse/jface/text/IRegion;
      38: astore        5
      40: aload         5
      42: ifnull        67
      45: aload_1
      46: aload         5
      48: invokeinterface #493,  1          // InterfaceMethod org/eclipse/jface/text/IRegion.getOffset:()I
      53: iconst_0
      54: invokeinterface #484,  3          // InterfaceMethod org/eclipse/ui/texteditor/ITextEditor.selectAndReveal:(II)V
      59: goto          67
      62: astore_3
      63: aload_3
      64: invokestatic  #415                // Method com/_1c/g5/v8/dt/internal/debug/ui/DebugUiPlugin.log:(Ljava/lang/Throwable;)V
      67: return
    Exception table:
       from    to  target type
           0    19    62   Class org/eclipse/debug/core/DebugException
          20    59    62   Class org/eclipse/debug/core/DebugException

  protected void clearSourceSelection(java.lang.Object);
    Code:
       0: aload_1
       1: instanceof    #507                // class org/eclipse/debug/core/model/IThread
       4: ifeq          26
       7: aload_1
       8: checkcast     #507                // class org/eclipse/debug/core/model/IThread
      11: astore_2
      12: aload_2
      13: invokestatic  #509                // Method org/eclipse/debug/internal/ui/views/launch/DecorationManager.removeDecorations:(Lorg/eclipse/debug/core/model/IThread;)V
      16: invokestatic  #306                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.getDefault:()Lorg/eclipse/debug/internal/ui/InstructionPointerManager;
      19: aload_2
      20: invokevirtual #513                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.removeAnnotations:(Lorg/eclipse/debug/core/model/IThread;)V
      23: goto          49
      26: aload_1
      27: instanceof    #515                // class org/eclipse/debug/core/model/IDebugTarget
      30: ifeq          49
      33: aload_1
      34: checkcast     #515                // class org/eclipse/debug/core/model/IDebugTarget
      37: astore_2
      38: aload_2
      39: invokestatic  #517                // Method org/eclipse/debug/internal/ui/views/launch/DecorationManager.removeDecorations:(Lorg/eclipse/debug/core/model/IDebugTarget;)V
      42: invokestatic  #306                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.getDefault:()Lorg/eclipse/debug/internal/ui/InstructionPointerManager;
      45: aload_2
      46: invokevirtual #520                // Method org/eclipse/debug/internal/ui/InstructionPointerManager.removeAnnotations:(Lorg/eclipse/debug/core/model/IDebugTarget;)V
      49: return

  private com._1c.g5.v8.dt.bsl.model.Module findModule(com._1c.g5.v8.dt.debug.core.model.BslModuleReference, com._1c.g5.v8.dt.core.platform.IExternalObjectProject);
    Code:
       0: new           #209                // class com/_1c/g5/v8/dt/debug/core/model/BslModuleReference
       3: dup
       4: aload_1
       5: invokevirtual #529                // Method com/_1c/g5/v8/dt/debug/core/model/BslModuleReference.getParentUuid:()Ljava/util/UUID;
       8: aload_1
       9: invokevirtual #533                // Method com/_1c/g5/v8/dt/debug/core/model/BslModuleReference.getPropertyUuid:()Ljava/util/UUID;
      12: aload_2
      13: invokeinterface #536,  1          // InterfaceMethod com/_1c/g5/v8/dt/core/platform/IExternalObjectProject.getProject:()Lorg/eclipse/core/resources/IProject;
      18: invokespecial #540                // Method com/_1c/g5/v8/dt/debug/core/model/BslModuleReference."<init>":(Ljava/util/UUID;Ljava/util/UUID;Lorg/eclipse/core/resources/IProject;)V
      21: astore_3
      22: aload_0
      23: getfield      #52                 // Field moduleLocator:Lcom/_1c/g5/v8/dt/debug/core/model/IBslModuleLocator;
      26: aload_3
      27: iconst_0
      28: invokeinterface #54,  3           // InterfaceMethod com/_1c/g5/v8/dt/debug/core/model/IBslModuleLocator.getModule:(Lcom/_1c/g5/v8/dt/debug/core/model/BslModuleReference;Z)Lcom/_1c/g5/v8/dt/bsl/model/Module;
      33: areturn

  private com._1c.g5.v8.dt.bsl.model.Module lambda$0(com._1c.g5.v8.dt.debug.core.model.BslModuleReference, com._1c.g5.v8.dt.core.platform.IExternalObjectProject);
    Code:
       0: aload_0
       1: aload_1
       2: aload_2
       3: invokevirtual #547                // Method findModule:(Lcom/_1c/g5/v8/dt/debug/core/model/BslModuleReference;Lcom/_1c/g5/v8/dt/core/platform/IExternalObjectProject;)Lcom/_1c/g5/v8/dt/bsl/model/Module;
       6: areturn

  private void lambda$2(com._1c.g5.v8.dt.bsl.ui.editor.BslXtextEditor, com._1c.g5.v8.dt.debug.core.model.IBslStackFrame);
    Code:
       0: aload_0
       1: getfield      #325                // Field inlineDebuggerService:Lcom/_1c/g5/v8/dt/internal/debug/ui/inlinedebug/IInlineDebuggerService;
       4: aload_1
       5: aload_2
       6: invokeinterface #551,  3          // InterfaceMethod com/_1c/g5/v8/dt/internal/debug/ui/inlinedebug/IInlineDebuggerService.displayInlineDebugging:(Lcom/_1c/g5/v8/dt/bsl/ui/editor/BslXtextEditor;Lcom/_1c/g5/v8/dt/debug/core/model/IBslStackFrame;)V
      11: return
}
