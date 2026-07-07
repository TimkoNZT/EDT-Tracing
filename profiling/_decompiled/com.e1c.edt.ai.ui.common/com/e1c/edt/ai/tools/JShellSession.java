package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.google.common.base.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.Snippet.Status;

class JShellSession implements IJShellSession {
   private final int sessionId;
   private final JShell shell;
   private final ByteArrayOutputStream outBuffer;
   private final ByteArrayOutputStream errBuffer;
   private final IRestrictedTypesValidator restrictedTypesValidator;
   private final Set<IJShellBindingProvider> bindingProviders;
   private final List<String> executionHistory = new ArrayList();
   private final AtomicBoolean isClosed = new AtomicBoolean(false);
   private ArrayList<String> cachedAvailableBindings;
   private static final int MAX_EXECUTION_HISTORY_SIZE = 100;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$jdk$jshell$Snippet$Status;

   JShellSession(int sessionId, JShell shell, ByteArrayOutputStream outBuffer, ByteArrayOutputStream errBuffer, IRestrictedTypesValidator restrictedTypesValidator, Set<IJShellBindingProvider> bindingProviders) {
      Preconditions.checkNotNull(shell);
      Preconditions.checkNotNull(outBuffer);
      Preconditions.checkNotNull(errBuffer);
      Preconditions.checkNotNull(restrictedTypesValidator);
      Preconditions.checkNotNull(bindingProviders);
      this.sessionId = sessionId;
      this.shell = shell;
      this.outBuffer = outBuffer;
      this.errBuffer = errBuffer;
      this.restrictedTypesValidator = restrictedTypesValidator;
      this.bindingProviders = bindingProviders;
      this.cachedAvailableBindings = this.buildAvailableBindings();
   }

   private ArrayList<String> buildAvailableBindings() {
      ArrayList<String> bindings = new ArrayList();

      for(IJShellBindingProvider provider : this.bindingProviders) {
         Map<String, JShellBindingDescription> descriptions = provider.getBindings();
         bindings.addAll(descriptions.keySet());
      }

      return bindings;
   }

   private void addToExecutionHistory(String source) {
      this.executionHistory.add(source);
      if (this.executionHistory.size() > 100) {
         this.executionHistory.remove(0);
      }

   }

   private void populateSessionResult(SessionResult result) {
      result.availableBindings = new ArrayList(this.cachedAvailableBindings);
      result.executionHistory = new ArrayList(this.executionHistory);
   }

   public int getSessionId() {
      return this.sessionId;
   }

   public JShellExecutionResult execute(String code) {
      if (this.isClosed.get()) {
         throw new ToolException("Session is closed. Cannot execute code.", (Throwable)null, ToolErrorType.RETRYABLE);
      } else {
         JShellExecutionResult result = new JShellExecutionResult();
         result.sessionId = this.sessionId;
         result.compilationErrors = new ArrayList();
         result.runtimeErrors = new ArrayList();
         if (this.restrictedTypesValidator != null) {
            try {
               this.restrictedTypesValidator.validate(code);
            } catch (ToolException e) {
               CompilationError error = new CompilationError();
               error.isError = true;
               error.message = e.getMessage();
               result.compilationErrors.add(error);
               return result;
            }
         }

         this.outBuffer.reset();
         this.errBuffer.reset();

         try {
            SourceCodeAnalysis analysis = this.shell.sourceCodeAnalysis();
            String remaining = code;

            while(result.compilationErrors.isEmpty() && remaining != null && !remaining.isBlank()) {
               SourceCodeAnalysis.CompletionInfo completion = analysis.analyzeCompletion(remaining);
               String source;
               if (completion.completeness().isComplete()) {
                  source = completion.source();
                  remaining = completion.remaining();
               } else {
                  source = completion.remaining();
               }

               List<SnippetEvent> events = this.shell.eval(source);
               if (!events.isEmpty()) {
                  for(SnippetEvent event : events) {
                     switch (event.status()) {
                        case VALID:
                           this.addToExecutionHistory(source);
                           if (event.exception() != null) {
                              JShellException exception = event.exception();
                              RuntimeError error = new RuntimeError();
                              error.exceptionType = exception.getClass().getName();
                              error.message = exception.getMessage();
                              StringWriter stackTrace = new StringWriter();
                              exception.printStackTrace(new PrintWriter(stackTrace));
                              error.stackTrace = stackTrace.toString();
                              result.runtimeErrors.add(error);
                           }
                           break;
                        case REJECTED:
                           List<Diag> diagnostics = (List)this.shell.diagnostics(event.snippet()).collect(Collectors.toList());
                           if (!diagnostics.isEmpty()) {
                              for(Diag diag : diagnostics) {
                                 CompilationError error = new CompilationError();
                                 error.isError = diag.isError();
                                 error.code = diag.getCode();
                                 error.message = diag.getMessage((Locale)null);
                                 error.position = diag.getPosition();
                                 error.startPosition = diag.getStartPosition();
                                 error.endPosition = diag.getEndPosition();
                                 String errorCode = diag.getCode();
                                 if (errorCode != null) {
                                    error.isResolutionError = errorCode.startsWith("compiler.err.cant.resolve") || "compiler.err.cant.apply.symbol".equals(errorCode);
                                    error.isUnreachableError = "compiler.err.unreachable.stmt".equals(errorCode);
                                    error.isNotAStatementError = "compiler.err.not.stmt".equals(errorCode);
                                 }

                                 result.compilationErrors.add(error);
                              }
                           }
                     }
                  }
               }
            }
         } catch (OutOfMemoryError e) {
            RuntimeError error = new RuntimeError();
            error.exceptionType = e.getClass().getName();
            error.message = "Out of memory during code execution. The code may have allocated too much memory.";
            error.stackTrace = e.getMessage();
            result.runtimeErrors.add(error);
            result.stdOut = this.outBuffer.toString();
            result.stdErr = this.errBuffer.toString();
            this.populateSessionResult(result);
            return result;
         } catch (ThreadDeath e) {
            throw e;
         } catch (Throwable var18) {
            RuntimeError error = new RuntimeError();
            error.exceptionType = var18.getClass().getName();
            error.message = var18.getMessage() != null ? var18.getMessage() : var18.getClass().getSimpleName();
            StringWriter stackTrace = new StringWriter();
            var18.printStackTrace(new PrintWriter(stackTrace));
            error.stackTrace = stackTrace.toString();
            result.runtimeErrors.add(error);
            result.stdOut = this.outBuffer.toString();
            result.stdErr = this.errBuffer.toString();
            this.populateSessionResult(result);
            return result;
         }

         result.stdOut = this.outBuffer.toString();
         result.stdErr = this.errBuffer.toString();
         this.populateSessionResult(result);
         return result;
      }
   }

   public List<String> getExecutionHistory() {
      return new ArrayList(this.executionHistory);
   }

   public SessionResult getSessionResult() {
      SessionResult result = new SessionResult();
      result.sessionId = this.sessionId;
      this.populateSessionResult(result);
      return result;
   }

   public void close() {
      if (!this.isClosed.getAndSet(true)) {
         try {
            this.shell.close();
            JShellObjectBridge.releaseSession(this.sessionId);
         } finally {
            this.executionHistory.clear();
         }
      }

   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$jdk$jshell$Snippet$Status() {
      int[] var10000 = $SWITCH_TABLE$jdk$jshell$Snippet$Status;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Status.values().length];

         try {
            var0[Status.DROPPED.ordinal()] = 4;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[Status.NONEXISTENT.ordinal()] = 7;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[Status.OVERWRITTEN.ordinal()] = 5;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[Status.RECOVERABLE_DEFINED.ordinal()] = 2;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[Status.RECOVERABLE_NOT_DEFINED.ordinal()] = 3;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[Status.REJECTED.ordinal()] = 6;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Status.VALID.ordinal()] = 1;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$jdk$jshell$Snippet$Status = var0;
         return var0;
      }
   }
}
