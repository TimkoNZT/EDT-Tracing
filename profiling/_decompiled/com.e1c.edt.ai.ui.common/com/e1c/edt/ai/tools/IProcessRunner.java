package com.e1c.edt.ai.tools;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface IProcessRunner {
   CompletableFuture<Optional<ProcessResult>> executeProcess(String var1, String var2, List<String> var3, Long var4, TimeUnit var5, Integer var6);
}
