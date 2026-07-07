package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.google.common.base.Preconditions;
import org.eclipse.core.runtime.IProgressMonitor;

class JobCancellationTokenSource extends CancellationTokenSource {
   private final Object lock = new Object();
   private IProgressMonitor monitor;

   public void attachMonitor(IProgressMonitor monitor) {
      Preconditions.checkNotNull(monitor);
      synchronized(this.lock) {
         this.monitor = monitor;
      }
   }

   public Boolean isCanceled() {
      synchronized(this.lock) {
         return CancellationTokens.isStopped || this.monitor != null && this.monitor.isCanceled() || super.isCanceled();
      }
   }

   public void cancel() {
      synchronized(this.lock) {
         if (this.monitor != null) {
            this.monitor.setCanceled(true);
         }
      }

      super.cancel();
   }
}
