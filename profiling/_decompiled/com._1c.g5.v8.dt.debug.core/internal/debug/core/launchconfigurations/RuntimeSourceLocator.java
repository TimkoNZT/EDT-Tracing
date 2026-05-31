Compiled from "RuntimeSourceLocator.java"
public class com._1c.g5.v8.dt.internal.debug.core.launchconfigurations.RuntimeSourceLocator extends org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector {
  private com._1c.g5.v8.dt.core.platform.IBmModelManager modelManager;

  private com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter qualifiedNameFilePathConverter;

  public com._1c.g5.v8.dt.internal.debug.core.launchconfigurations.RuntimeSourceLocator();
    Code:
       0: aload_0
       1: invokespecial #14                 // Method org/eclipse/debug/core/sourcelookup/AbstractSourceLookupDirector."<init>":()V
       4: return

  public void initializeParticipants();
    Code:
       0: aload_0
       1: iconst_1
       2: anewarray     #21                 // class org/eclipse/debug/core/sourcelookup/ISourceLookupParticipant
       5: dup
       6: iconst_0
       7: new           #23                 // class com/_1c/g5/v8/dt/internal/debug/core/launchconfigurations/BslSourceLookupParticipant
      10: dup
      11: aload_0
      12: getfield      #25                 // Field modelManager:Lcom/_1c/g5/v8/dt/core/platform/IBmModelManager;
      15: aload_0
      16: getfield      #27                 // Field qualifiedNameFilePathConverter:Lcom/_1c/g5/v8/dt/core/filesystem/IQualifiedNameFilePathConverter;
      19: invokespecial #29                 // Method com/_1c/g5/v8/dt/internal/debug/core/launchconfigurations/BslSourceLookupParticipant."<init>":(Lcom/_1c/g5/v8/dt/core/platform/IBmModelManager;Lcom/_1c/g5/v8/dt/core/filesystem/IQualifiedNameFilePathConverter;)V
      22: aastore
      23: invokevirtual #32                 // Method addParticipants:([Lorg/eclipse/debug/core/sourcelookup/ISourceLookupParticipant;)V
      26: return
}
