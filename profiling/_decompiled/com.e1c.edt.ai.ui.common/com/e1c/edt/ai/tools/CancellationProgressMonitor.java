package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;

public class CancellationProgressMonitor implements ICancellationProgressMonitor {
   private ICancellationToken cancellationToken;
   private boolean isCanceled = false;

   public void setCancellationToken(ICancellationToken cancellationToken) {
      this.cancellationToken = cancellationToken;
   }

   public void beginTask(String name, int totalWork) {
   }

   public void done() {
   }

   public void internalWorked(double work) {
   }

   public boolean isCanceled() {
      return this.isCanceled || this.cancellationToken.isCanceled();
   }

   public void setCanceled(boolean value) {
      this.isCanceled = value;
   }

   public void setTaskName(String name) {
   }

   public void subTask(String name) {
   }

   public void worked(int work) {
   }
}
