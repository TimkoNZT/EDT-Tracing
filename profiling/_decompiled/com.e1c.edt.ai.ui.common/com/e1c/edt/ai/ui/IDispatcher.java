package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Supplier;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.core.runtime.jobs.Job;

public interface IDispatcher {
   <T> Optional<T> dispatch(Supplier<? extends T> var1);

   Boolean dispatch(Runnable var1);

   void dispatchAsync(Runnable var1);

   Job createJob(String var1, Consumer<JobContext> var2, boolean var3, ICancellationToken var4);

   <T> Optional<T> dispatch(Supplier<? extends T> var1, Duration var2);

   boolean checkThread(boolean var1, boolean var2);
}
