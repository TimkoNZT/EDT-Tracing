Compiled from "IBslStackFrame.java"
public interface com._1c.g5.v8.dt.debug.core.model.IBslStackFrame extends org.eclipse.debug.core.model.IStackFrame,com._1c.g5.v8.dt.debug.core.model.IRuntimeDebugElement {
  public abstract int getLineNumber();

  public abstract java.lang.String getSignature();

  public abstract com._1c.g5.v8.dt.debug.core.model.IRuntimeDebugTargetThread getThread();

  public abstract com._1c.g5.v8.dt.debug.core.model.IBslVariable[] getVariables();

  public abstract boolean hasModuleVariables();

  public abstract com._1c.g5.v8.dt.debug.core.model.IBslVariable[] getModuleVariables();

  public abstract boolean hasModuleProperties();

  public abstract com._1c.g5.v8.dt.debug.core.model.IBslVariable[] getModuleProperties();

  public abstract boolean isEnabled();

  public abstract int getLevel();

  public abstract org.eclipse.emf.common.util.URI getSource();

  public abstract com._1c.g5.v8.dt.bsl.model.Module getModule();

  public abstract com._1c.g5.v8.dt.debug.core.model.BslModuleReference getReference();

  public abstract com._1c.g5.v8.dt.debug.core.model.evaluation.IEvaluationChain reevaluateVariables();

  public abstract boolean isFantom();

  public abstract boolean isRootForUuid(java.util.UUID);

  public default org.eclipse.debug.core.model.IVariable[] getVariables() throws org.eclipse.debug.core.DebugException;
    Code:
       0: aload_0
       1: invokeinterface #40,  1           // InterfaceMethod getVariables:()[Lcom/_1c/g5/v8/dt/debug/core/model/IBslVariable;
       6: areturn

  public default org.eclipse.debug.core.model.IThread getThread();
    Code:
       0: aload_0
       1: invokeinterface #45,  1           // InterfaceMethod getThread:()Lcom/_1c/g5/v8/dt/debug/core/model/IRuntimeDebugTargetThread;
       6: areturn
}
