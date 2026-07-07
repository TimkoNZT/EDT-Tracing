package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class Dispatcher implements IDispatcher {
   private ILog log;

   @Inject
   public Dispatcher(ILog log) {
      Preconditions.checkNotNull(log);
      this.log = log;
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
}
