package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProcessRunner implements IProcessRunner {
   private static final String CONSOLE_CHARSET = detectConsoleCharset();
   private final ILog log;
   private final ExecutorService executor;

   @Inject
   public ProcessRunner(ILog log) {
      Preconditions.checkNotNull(log);
      this.log = log;
      this.executor = Executors.newCachedThreadPool();
   }

   public CompletableFuture<Optional<ProcessResult>> executeProcess(String executable, String workingDirectory, List<String> args, Long timeout, TimeUnit timeUnit, Integer maxLines) {
      Preconditions.checkNotNull(executable);
      if (timeout == null) {
         timeout = 15L;
      }

      if (maxLines == null) {
         maxLines = 1000;
      }

      Process process;
      try {
         ProcessBuilder processBuilder = new ProcessBuilder(new String[0]);
         List<String> command = processBuilder.command();
         command.add(executable);
         if (args != null && !args.isEmpty()) {
            command.addAll(args);
         }

         if (workingDirectory != null) {
            processBuilder.directory(new File(workingDirectory));
         }

         process = processBuilder.start();
      } catch (IOException e) {
         this.log.logError(e);
         return CompletableFuture.failedFuture(e);
      }

      CompletableFuture<StreamReadResult> stdOutFuture = this.readStreamAsync(process.getInputStream(), maxLines);
      CompletableFuture<StreamReadResult> stdErrFuture = this.readStreamAsync(process.getErrorStream(), maxLines);
      CompletableFuture<Integer> exitCodeFuture = CompletableFuture.supplyAsync(() -> {
         try {
            return process.waitFor();
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process execution interrupted", e);
         }
      }, this.executor);
      CompletableFuture<Optional<ProcessResult>> resultFuture = exitCodeFuture.thenCombineAsync(stdOutFuture.thenCombine(stdErrFuture, (stdOutResult, stdErrResult) -> {
         ProcessResult result = new ProcessResult();
         result.stdOut = stdOutResult.content;
         result.stdErr = stdErrResult.content;
         result.stdOutTruncated = stdOutResult.truncated;
         result.stdErrTruncated = stdErrResult.truncated;
         return result;
      }), (exitCode, result) -> {
         result.exitCode = exitCode;
         return Optional.of(result);
      }, this.executor).exceptionally((ex) -> {
         this.log.logError(ex);
         if (process.isAlive()) {
            process.destroyForcibly();
         }

         stdOutFuture.cancel(true);
         stdErrFuture.cancel(true);
         return Optional.empty();
      });
      return resultFuture.orTimeout(timeout, timeUnit).exceptionally((ex) -> {
         if (ex instanceof TimeoutException) {
            this.log.logError(ex);
            if (process.isAlive()) {
               process.destroyForcibly();
            }

            stdOutFuture.cancel(true);
            stdErrFuture.cancel(true);
         }

         return Optional.empty();
      });
   }

   private CompletableFuture<StreamReadResult> readStreamAsync(InputStream inputStream, int maxLines) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CONSOLE_CHARSET));
            StringBuilder lineBuffer = new StringBuilder();
            int lineCount = 0;

            boolean truncated;
            String line;
            for(truncated = false; (line = reader.readLine()) != null && lineCount < maxLines; ++lineCount) {
               if (lineCount > 0) {
                  lineBuffer.append('\n');
               }

               lineBuffer.append(line);
            }

            if (line != null) {
               truncated = true;
            }

            String content = lineBuffer.toString();
            return new StreamReadResult(content, truncated);
         } catch (IOException e) {
            throw new UncheckedIOException("Error reading process stream", e);
         }
      }, this.executor);
   }

   private static String detectConsoleCharset() {
      String os = System.getProperty("os.name", "").toLowerCase();
      if (os.contains("win")) {
         try {
            Method charsetMethod = Console.class.getMethod("charset");
            Console console = System.console();
            if (console != null) {
               Charset charset = (Charset)charsetMethod.invoke(console);
               return charset.name();
            }
         } catch (Exception var4) {
         }

         String encoding = System.getProperty("sun.stdout.encoding");
         if (encoding != null && !encoding.isEmpty()) {
            return encoding;
         } else {
            String lang = System.getProperty("user.language", "en").toLowerCase();
            return lang.equals("ru") ? "CP866" : "CP437";
         }
      } else {
         return "UTF-8";
      }
   }
}
