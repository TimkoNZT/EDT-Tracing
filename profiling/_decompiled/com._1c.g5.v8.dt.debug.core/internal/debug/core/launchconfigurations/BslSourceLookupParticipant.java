Compiled from "BslSourceLookupParticipant.java"
public class com._1c.g5.v8.dt.internal.debug.core.launchconfigurations.BslSourceLookupParticipant extends org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant {
  private com._1c.g5.v8.dt.core.platform.IBmModelManager modelManager;

  private com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter qualifiedNameFilePathConverter;

  public com._1c.g5.v8.dt.internal.debug.core.launchconfigurations.BslSourceLookupParticipant(com._1c.g5.v8.dt.core.platform.IBmModelManager, com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter);
    Code:
       0: aload_0
       1: invokespecial #12                 // Method org/eclipse/debug/core/sourcelookup/AbstractSourceLookupParticipant."<init>":()V
       4: aload_0
       5: aload_1
       6: putfield      #15                 // Field modelManager:Lcom/_1c/g5/v8/dt/core/platform/IBmModelManager;
       9: aload_0
      10: aload_2
      11: putfield      #17                 // Field qualifiedNameFilePathConverter:Lcom/_1c/g5/v8/dt/core/filesystem/IQualifiedNameFilePathConverter;
      14: return

  public java.lang.String getSourceName(java.lang.Object) throws org.eclipse.core.runtime.CoreException;
    Code:
       0: aload_1
       1: instanceof    #28                 // class com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame
       4: ifeq          188
       7: aload_1
       8: checkcast     #28                 // class com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame
      11: astore_2
      12: aload_2
      13: invokeinterface #30,  1           // InterfaceMethod com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame.getSource:()Lorg/eclipse/emf/common/util/URI;
      18: astore_3
      19: aload_3
      20: ifnull        127
      23: getstatic     #34                 // Field org/eclipse/xtext/resource/IResourceServiceProvider$Registry.INSTANCE:Lorg/eclipse/xtext/resource/IResourceServiceProvider$Registry;
      26: aload_3
      27: invokeinterface #40,  2           // InterfaceMethod org/eclipse/xtext/resource/IResourceServiceProvider$Registry.getResourceServiceProvider:(Lorg/eclipse/emf/common/util/URI;)Lorg/eclipse/xtext/resource/IResourceServiceProvider;
      32: astore        4
      34: aload         4
      36: ldc           #44                 // class com/_1c/g5/v8/dt/core/filesystem/IProjectFileSystemSupportProvider
      38: invokeinterface #46,  2           // InterfaceMethod org/eclipse/xtext/resource/IResourceServiceProvider.get:(Ljava/lang/Class;)Ljava/lang/Object;
      43: checkcast     #44                 // class com/_1c/g5/v8/dt/core/filesystem/IProjectFileSystemSupportProvider
      46: astore        5
      48: aload_0
      49: getfield      #15                 // Field modelManager:Lcom/_1c/g5/v8/dt/core/platform/IBmModelManager;
      52: aload_3
      53: invokeinterface #52,  2           // InterfaceMethod com/_1c/g5/v8/dt/core/platform/IBmModelManager.getModel:(Lorg/eclipse/emf/common/util/URI;)Lcom/_1c/g5/v8/bm/integration/IBmModel;
      58: astore        6
      60: aload         6
      62: invokeinterface #58,  1           // InterfaceMethod com/_1c/g5/v8/bm/integration/IBmModel.getEngine:()Lcom/_1c/g5/v8/bm/core/IBmEngine;
      67: aload_3
      68: aconst_null
      69: invokeinterface #64,  3           // InterfaceMethod com/_1c/g5/v8/bm/core/IBmEngine.resolve:(Lorg/eclipse/emf/common/util/URI;Lorg/eclipse/emf/ecore/EClass;)Lorg/eclipse/emf/ecore/EObject;
      74: checkcast     #70                 // class com/_1c/g5/v8/bm/core/IBmObject
      77: astore        7
      79: aload_0
      80: getfield      #15                 // Field modelManager:Lcom/_1c/g5/v8/dt/core/platform/IBmModelManager;
      83: aload         6
      85: invokeinterface #72,  2           // InterfaceMethod com/_1c/g5/v8/dt/core/platform/IBmModelManager.getProject:(Lcom/_1c/g5/v8/bm/integration/IBmModel;)Lorg/eclipse/core/resources/IProject;
      90: astore        8
      92: aload         5
      94: aload         8
      96: invokeinterface #76,  2           // InterfaceMethod com/_1c/g5/v8/dt/core/filesystem/IProjectFileSystemSupportProvider.getProjectFileSystemSupport:(Lorg/eclipse/core/resources/IProject;)Lcom/_1c/g5/v8/dt/core/filesystem/IProjectFileSystemSupport;
     101: astore        9
     103: aload         9
     105: aload         7
     107: invokeinterface #80,  2           // InterfaceMethod com/_1c/g5/v8/dt/core/filesystem/IProjectFileSystemSupport.getFile:(Lorg/eclipse/emf/ecore/EObject;)Lorg/eclipse/core/resources/IFile;
     112: astore        10
     114: aload         10
     116: invokeinterface #86,  1           // InterfaceMethod org/eclipse/core/resources/IFile.getLocation:()Lorg/eclipse/core/runtime/IPath;
     121: invokeinterface #92,  1           // InterfaceMethod org/eclipse/core/runtime/IPath.lastSegment:()Ljava/lang/String;
     126: areturn
     127: aload_2
     128: invokeinterface #98,  1           // InterfaceMethod com/_1c/g5/v8/dt/debug/core/model/IBslStackFrame.getName:()Ljava/lang/String;
     133: ldc           #101                // String
     135: invokevirtual #103                // Method java/lang/String.split:(Ljava/lang/String;)[Ljava/lang/String;
     138: iconst_0
     139: aaload
     140: astore        4
     142: aload         4
     144: bipush        40
     146: invokevirtual #109                // Method java/lang/String.indexOf:(I)I
     149: istore        5
     151: iload         5
     153: iconst_m1
     154: if_icmpeq     172
     157: aload         4
     159: iconst_0
     160: aload         4
     162: bipush        46
     164: invokevirtual #113                // Method java/lang/String.lastIndexOf:(I)I
     167: invokevirtual #116                // Method java/lang/String.substring:(II)Ljava/lang/String;
     170: astore        4
     172: aload_0
     173: getfield      #17                 // Field qualifiedNameFilePathConverter:Lcom/_1c/g5/v8/dt/core/filesystem/IQualifiedNameFilePathConverter;
     176: aload         4
     178: invokeinterface #120,  2          // InterfaceMethod com/_1c/g5/v8/dt/core/filesystem/IQualifiedNameFilePathConverter.getFilePath:(Ljava/lang/String;)Ljava/lang/String;
     183: astore        6
     185: aload         6
     187: areturn
     188: aconst_null
     189: areturn
}
