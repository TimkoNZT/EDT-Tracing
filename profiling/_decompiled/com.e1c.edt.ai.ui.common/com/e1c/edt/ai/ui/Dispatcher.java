package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

class Dispatcher implements IDispatcher {
   private static final StackTraceElement[] EmptyStackTrace = new StackTraceElement[0];
   private final ILog log;
   private final ISettings settings;
   private final IClock clock;
   private final ArrayList<Job> currentJobs = new ArrayList();

   @Inject
   public Dispatcher(ILog log, ISettings settings, IClock clock) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(clock);
      this.settings = settings;
      this.log = log;
      this.clock = clock;
   }

   public <T> Optional<T> dispatch(Supplier<? extends T> supplier) {
      Preconditions.checkNotNull(supplier);
      return this.<T>dispatch(supplier, false);
   }

   public Boolean dispatch(Runnable runnable) {
      Preconditions.checkNotNull(runnable);
      return this.dispatch(() -> {
         runnable.run();
         return 0;
      }, false).isPresent();
   }

   public void dispatchAsync(Runnable runnable) {
      Preconditions.checkNotNull(runnable);
      this.dispatch(() -> {
         runnable.run();
         return 0;
      }, true);
   }

   private <T> Optional<T> dispatch(Supplier<? extends T> supplier, boolean async) {
      Preconditions.checkNotNull(supplier);
      LocalDateTime startTime = this.clock.now();
      ArrayList<T> vals = new ArrayList();
      if (async) {
         StackTraceElement[] stackTrace = this.getStack();
         Display.getDefault().asyncExec(() -> {
            try {
               vals.add(supplier.get());
               this.checkMicrofreeze("Async call", startTime, () -> stackTrace);
            } catch (Exception ex) {
               this.log.logError(ex);
            }

         });
      } else {
         if (Thread.currentThread() == Display.getDefault().getThread()) {
            try {
               vals.add(supplier.get());
            } catch (Exception ex) {
               this.log.logError(ex);
            }
         } else {
            Display.getDefault().syncExec(() -> {
               try {
                  vals.add(supplier.get());
               } catch (Exception ex) {
                  this.log.logError(ex);
               }

            });
         }

         this.checkMicrofreeze("Sync call", startTime, () -> Thread.currentThread().getStackTrace());
      }

      return vals.isEmpty() ? Optional.empty() : Optional.ofNullable(vals.get(0));
   }

   private StackTraceElement[] getStack() {
      return this.settings.getVerbosity().getLevel() >= Verbosity.TRACE.getLevel() ? Thread.currentThread().getStackTrace() : EmptyStackTrace;
   }

   private void checkMicrofreeze(String description, LocalDateTime startTime, Supplier<StackTraceElement[]> stackTraceSupplier) {
      if (this.settings.getVerbosity().getLevel() >= Verbosity.TRACE.getLevel()) {
         Duration duration = Duration.between(startTime, this.clock.now());
         if (duration.toMillis() > this.settings.getMinRequestDelay().toMillis()) {
            this.log.warning("Microfreeze UI", () -> {
               StringBuilder sb = new StringBuilder();
               sb.append("Description: ");
               sb.append(description);
               sb.append(System.lineSeparator());
               sb.append("Duration: ");
               sb.append(duration.toMillis());
               sb.append(" ms");
               sb.append(System.lineSeparator());
               sb.append("Stack:");
               StackTraceElement[] stackTrace = (StackTraceElement[])stackTraceSupplier.get();
               if (stackTrace.length > 0) {
                  sb.append(System.lineSeparator());

                  for(StackTraceElement ste : stackTrace) {
                     sb.append(System.lineSeparator());
                     sb.append(ste);
                  }
               } else {
                  sb.append(" empty");
               }

               return sb.toString();
            });
         }

      }
   }

   public Job createJob(final String jobName, final Consumer<JobContext> consumer, boolean isInfrastucture, ICancellationToken cancellationToken) {
      Preconditions.checkNotNull(jobName);
      Preconditions.checkNotNull(consumer);
      Preconditions.checkNotNull(cancellationToken);
      if (!this.settings.isEnabled() && !isInfrastucture) {
         this.log.warning("jobs", () -> "Running non infrastructure job \"" + jobName + "\" while plugin is disabled.");
      }

      boolean isTracing = this.log.isTracingEnabled("jobs");
      final ArrayList<AutoCloseable> resources = new ArrayList();
      final ArrayList<Job> jobs = new ArrayList();
      final <undefinedtype> job = new Job(jobName) {
         protected IStatus run(IProgressMonitor monitor) {
            JobCancellationTokenSource cancellationTokenSource = new JobCancellationTokenSource();
            cancellationTokenSource.attachMonitor(monitor);

            IStatus var6;
            try {
               LocalDateTime startTime = Dispatcher.this.clock.now();
               consumer.accept(new JobContext((Job)jobs.get(0), monitor, cancellationTokenSource));
               Duration duration = Duration.between(startTime, Dispatcher.this.clock.now());
               Dispatcher.this.log.trace("jobs", jobName, () -> "duration: " + duration);
               var6 = cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
               return var6;
            } catch (Throwable error) {
               Dispatcher.this.log.trace("jobs", jobName, () -> "error: " + error);
               var6 = Status.error(jobName, error);
            } finally {
               synchronized(resources) {
                  for(AutoCloseable resource : resources) {
                     try {
                        resource.close();
                     } catch (Exception error) {
                        Dispatcher.this.log.logError(error);
                     }
                  }

               }
            }

            return var6;
         }
      };
      jobs.add(job);
      AutoCloseable attachToken = CancellationTokenSource.attach(cancellationToken, () -> job.cancel());
      synchronized(resources) {
         resources.add(attachToken);
      }

      if (isTracing) {
         synchronized(this.currentJobs) {
            this.currentJobs.add(job);
         }

         job.addJobChangeListener(new IJobChangeListener() {
            public void aboutToRun(IJobChangeEvent event) {
               Dispatcher.this.log.trace("jobs", jobName, () -> "about to run" + Dispatcher.this.getJobsInfo());
            }

            public void awake(IJobChangeEvent event) {
               Dispatcher.this.log.trace("jobs", jobName, () -> "awake" + Dispatcher.this.getJobsInfo());
            }

            public void done(IJobChangeEvent event) {
               synchronized(Dispatcher.this.currentJobs) {
                  Dispatcher.this.currentJobs.remove(job);
               }

               Dispatcher.this.log.trace("jobs", jobName, () -> "done" + Dispatcher.this.getJobsInfo());
            }

            public void running(IJobChangeEvent event) {
               Dispatcher.this.log.trace("jobs", jobName, () -> "running" + Dispatcher.this.getJobsInfo());
            }

            public void scheduled(IJobChangeEvent event) {
               Dispatcher.this.log.trace("jobs", jobName, () -> "scheduled" + Dispatcher.this.getJobsInfo());
            }

            public void sleeping(IJobChangeEvent event) {
               Dispatcher.this.log.trace("jobs", jobName, () -> "sleeping" + Dispatcher.this.getJobsInfo());
            }
         });
      }

      return job;
   }

   private String getJobsInfo() {
      synchronized(this.currentJobs) {
         return " jobs: " + String.join(", ", (Iterable)this.currentJobs.stream().map((i) -> i.getName()).sorted().collect(Collectors.toList()));
      }
   }

   public <T> Optional<T> dispatch(Supplier<? extends T> supplier, Duration timeout) {
      Preconditions.checkNotNull(supplier);
      Preconditions.checkNotNull(timeout);
      ExecutorService executor = Executors.newCachedThreadPool();

      Optional var6;
      try {
         T result = (T)executor.submit(() -> supplier.get()).get(timeout.toNanos(), TimeUnit.NANOSECONDS);
         var6 = Optional.ofNullable(result);
         return var6;
      } catch (ExecutionException | TimeoutException | InterruptedException error) {
         this.log.warning("Dispatch", () -> error.toString());
         var6 = Optional.empty();
      } finally {
         executor.shutdown();
      }

      return var6;
   }

   public boolean checkThread(boolean isUI, boolean showWarning) {
      boolean actualIsUI = Thread.currentThread() == Display.getDefault().getThread();
      if (this.settings.getVerbosity().getLevel() < Verbosity.TRACE.getLevel()) {
         return actualIsUI;
      } else if (actualIsUI == isUI) {
         return true;
      } else if (!showWarning) {
         return false;
      } else {
         StackTraceElement[] stackTrace = this.getStack();
         this.log.warning(isUI ? "Execution in the UI thread is expected" : "Execution not in a UI thread is expected", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Stack:");
            if (stackTrace.length > 0) {
               sb.append(System.lineSeparator());

               for(StackTraceElement ste : stackTrace) {
                  sb.append(System.lineSeparator());
                  sb.append(ste);
               }
            } else {
               sb.append(" empty");
            }

            return sb.toString();
         });
         return false;
      }
   }
}
