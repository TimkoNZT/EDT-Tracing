package com.e1c.edt.ai.context;

import com.google.common.base.Supplier;
import java.time.Duration;
import java.util.Optional;

public interface IDispatcher {
   <T> Optional<T> dispatch(Supplier<? extends T> var1, Duration var2);
}
