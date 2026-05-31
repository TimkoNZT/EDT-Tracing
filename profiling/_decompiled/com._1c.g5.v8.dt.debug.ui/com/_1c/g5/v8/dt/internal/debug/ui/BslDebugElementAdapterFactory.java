package com._1c.g5.v8.dt.internal.debug.ui;

import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com._1c.g5.v8.dt.internal.debug.ui.hover.DebugElementHoverRoot;
import com._1c.g5.v8.dt.internal.debug.ui.namingcollections.DebugNamingCollectionsService;
import com._1c.g5.v8.dt.internal.debug.ui.values.BslIndexedValuePartition;
import com._1c.g5.v8.dt.internal.debug.ui.values.BslValueColumnFactoryAdapter;
import com._1c.g5.v8.dt.internal.debug.ui.variables.BslExpressionContentProvider;
import com._1c.g5.v8.dt.internal.debug.ui.variables.BslStackFrameMementoProvider;
import com._1c.g5.v8.dt.internal.debug.ui.variables.BslVariableContentProvider;
import com._1c.g5.v8.dt.internal.debug.ui.variables.BslVariableLabelProvider;
import com._1c.g5.v8.dt.internal.debug.ui.variables.BslWatchExpressionFactoryAdapter;
import com._1c.g5.v8.dt.internal.debug.ui.variables.BslWatchExpressionLabelProvider;
import com._1c.g5.v8.dt.internal.debug.ui.variables.OriginalAwareIndexedVariablePartition;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoProvider;
import org.eclipse.debug.internal.ui.views.variables.IndexedVariablePartition;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter;
import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
import org.eclipse.jface.preference.IPreferenceStore;

public class BslDebugElementAdapterFactory implements IAdapterFactory {
   private final BslVariableContentProvider variableContentProvider;
   private final BslExpressionContentProvider expressionContentProvider;
   @Inject
   private BslVariableLabelProvider variableLabelProvider;
   @Inject
   private BslStackFrameContentProvider stackFrameContentProvider;
   @Inject
   private BslStackFrameMementoProvider stackFrameMementoProvider;
   @Inject
   private BslValueColumnFactoryAdapter valueColumnFactory;
   @Inject
   private BslWatchExpressionLabelProvider watchExpressionLabelProvider;
   @Inject
   private BslWatchExpressionFactoryAdapter watchExpressionAdapter;
   @Inject
   private Provider<ISourceDisplay> sourceDisplayProvider;

   @Inject
   public BslDebugElementAdapterFactory(DebugNamingCollectionsService namingCollectionsService) {
      this.variableContentProvider = new BslVariableContentProvider(namingCollectionsService);
      this.expressionContentProvider = new BslExpressionContentProvider(this.variableContentProvider);
   }

   public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
      if (adapterType == ISourceDisplay.class && adaptableObject instanceof IBslStackFrame) {
         return (T)this.sourceDisplayProvider.get();
      } else {
         if (adapterType == IElementContentProvider.class) {
            if (adaptableObject instanceof IBslStackFrame) {
               return (T)this.stackFrameContentProvider;
            }

            if (adaptableObject instanceof IBslVariable || this.isBslVariablePartition(adaptableObject)) {
               return (T)this.variableContentProvider;
            }

            if (adaptableObject instanceof IExpression) {
               return (T)this.expressionContentProvider;
            }
         }

         if (adapterType == IElementLabelProvider.class) {
            if (adaptableObject instanceof IBslVariable) {
               return (T)this.variableLabelProvider;
            }

            if (adaptableObject instanceof IExpression) {
               return (T)this.watchExpressionLabelProvider;
            }
         }

         if (adapterType == IColumnPresentationFactory.class) {
            if (adaptableObject instanceof IBslStackFrame || adaptableObject instanceof IBslValue || adaptableObject instanceof IBslVariable) {
               return (T)this.valueColumnFactory;
            }

            if (adaptableObject instanceof DebugElementHoverRoot) {
               IPreferenceStore preferenceStore = DebugUiPlugin.getDefault().getPreferenceStore();
               if (preferenceStore.getString("debug.dialog.hover.style") == "HOVERDIALOG_STYLE_AS_TABLE") {
                  return (T)this.valueColumnFactory;
               }
            }

            if (adaptableObject instanceof IExpressionManager) {
               IExpressionManager expressionManager = (IExpressionManager)adaptableObject;
               if (expressionManager.hasWatchExpressionDelegate("com._1c.g5.v8.dt.debug")) {
                  return (T)this.valueColumnFactory;
               }
            }
         }

         if (adapterType != IWatchExpressionFactoryAdapter.class || !(adaptableObject instanceof IBslVariable) && !this.isBslVariablePartition(adaptableObject)) {
            if (adapterType == IElementMementoProvider.class && adaptableObject instanceof IBslStackFrame) {
               return (T)this.stackFrameMementoProvider;
            } else {
               return null;
            }
         } else {
            return (T)this.watchExpressionAdapter;
         }
      }
   }

   public Class<?>[] getAdapterList() {
      return new Class[]{ISourceDisplay.class, IElementContentProvider.class, IElementLabelProvider.class, IColumnPresentationFactory.class, IWatchExpressionFactoryAdapter.class, IElementMementoProvider.class};
   }

   private boolean isBslVariablePartition(Object adaptableObject) {
      return adaptableObject instanceof IndexedVariablePartition && ((IndexedVariablePartition)adaptableObject).getValue() instanceof BslIndexedValuePartition || adaptableObject instanceof OriginalAwareIndexedVariablePartition;
   }
}
