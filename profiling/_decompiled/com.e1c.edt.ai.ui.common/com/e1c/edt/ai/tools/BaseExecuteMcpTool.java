package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public abstract class BaseExecuteMcpTool<TRequest extends BaseExecuteRequest> implements IMcpTool {
   protected final IJson json;
   protected final IMcpToolsCallMessageFactory messageFactory;
   protected final IProcessRunner processRunner;
   protected final IEnvironment environment;
   protected final int DEFAULT_MAX_LINES = 500;

   protected BaseExecuteMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory, IProcessRunner processRunner) {
      this.environment = environment;
      this.json = json;
      this.messageFactory = messageFactory;
      this.processRunner = processRunner;
   }

   protected abstract Class<TRequest> getRequestType();

   protected abstract String getExecutable(TRequest var1);

   protected abstract String getQuestionExample();

   protected abstract String getAnswerExample();

   protected abstract void validateRequest(TRequest var1) throws ToolException;

   protected abstract String getToolName();

   protected abstract String getToolDescription();

   protected abstract void addToolSpecificProperties(HashMap<String, McpToolCallProperty> var1);

   protected abstract List<String> getRequiredParameters();

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = false;
      Optional<TRequest> optionalRequest = this.json.deserialize(call.function.arguments, this.getRequestType());
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use this example: " + this.getQuestionExample());
      } else {
         TRequest request = (TRequest)(optionalRequest.get());
         this.validateRequest(request);
         long timeout = request.timeout != null ? request.timeout : 30L;
         if (timeout <= 0L || timeout > 300L) {
            timeout = 30L;
         }

         String executable = this.getExecutable(request);
         String commandLine = this.buildCommandLine(executable, request.args);
         if (call.callKind == ToolCallKind.RENDER) {
            details.requestMarkdown = MessageFormat.format(Messages.ExecuteTitleTemplate, commandLine);
            return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
         } else {
            CompletableFuture<Optional<ProcessResult>> futureResult = this.processRunner.executeProcess(executable, request.working_directory, request.args, timeout, TimeUnit.SECONDS, 500);
            return futureResult.thenCompose((optResult) -> {
               if (cancellationToken.isCanceled()) {
                  throw new ToolException("Operation was cancelled during process execution.");
               } else {
                  return (CompletionStage)optResult.map((response) -> {
                     String content = this.json.serialize(response);
                     String responseMarkdown = this.buildResponseMarkdown(response, commandLine);
                     details.responseMarkdown = responseMarkdown;
                     return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, content, details));
                  }).orElseGet(() -> {
                     throw new ToolException("Process execution failed - no result.");
                  });
               }
            }).exceptionally((error) -> {
               Throwable cause = error.getCause();
               throw new ToolException("Process execution failed", cause, ToolErrorType.RETRYABLE);
            });
         }
      }
   }

   public McpToolCallSpecification getSpecification() {
      return this.createSpecification();
   }

   protected McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = this.getToolName();
      StringBuilder description = new StringBuilder();
      description.append(this.getToolDescription());
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Runs under ").append(this.environment.getOSName()).append(" ").append(this.environment.getOSVersion()).append(" (").append(this.environment.getArch()).append(").");
      description.append("\n\nExample:");
      description.append("\n  Q: ").append(this.getQuestionExample());
      description.append("\n  A: ").append(this.getAnswerExample());
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      this.addToolSpecificProperties(properties);
      McpToolCallProperty workingDirProp = new McpToolCallProperty();
      workingDirProp.type = "string";
      workingDirProp.description = "Working directory (optional)";
      properties.put("working_directory", workingDirProp);
      McpToolCallProperty argsProp = new McpToolCallProperty();
      argsProp.type = "array";
      argsProp.description = "Command arguments as array of strings";
      properties.put("args", argsProp);
      McpToolCallProperty timeoutProp = new McpToolCallProperty();
      timeoutProp.type = "integer";
      timeoutProp.description = "Timeout in seconds (1-300, default: 30)";
      properties.put("timeout", timeoutProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList((String[])this.getRequiredParameters().toArray(new String[0]));
      spec.function.parameters = parameters;
      return spec;
   }

   private String buildResponseMarkdown(ProcessResult response, String commandLine) {
      StringBuilder responseMarkdown = new StringBuilder();
      responseMarkdown.append(MessageFormat.format(Messages.ExecutedTemplate, commandLine));
      responseMarkdown.append("\n\n<details><summary>").append(Messages.ExecutionDetails).append("</summary>\n\n");
      responseMarkdown.append("__").append(Messages.ExitCode).append(":__ `").append(String.valueOf(response.exitCode)).append("`\n");
      if (response.stdOut != null && !response.stdOut.isEmpty()) {
         responseMarkdown.append("\n__").append(Messages.StdOutLabel).append(":__\n");
         if (response.stdOutTruncated) {
            responseMarkdown.append(this.getTruncationWarning());
         }

         responseMarkdown.append("```\n");
         responseMarkdown.append(response.stdOut);
         responseMarkdown.append("\n```\n");
      }

      if (response.stdErr != null && !response.stdErr.isEmpty()) {
         responseMarkdown.append("\n__").append(Messages.StdErrLabel).append(":__\n");
         if (response.stdErrTruncated) {
            responseMarkdown.append(this.getTruncationWarning());
         }

         responseMarkdown.append("```\n");
         responseMarkdown.append(response.stdErr);
         responseMarkdown.append("\n```\n");
      }

      responseMarkdown.append("</details>");
      return responseMarkdown.toString();
   }

   private String getTruncationWarning() {
      return "*Warning: " + MessageFormat.format(Messages.OutputTruncatedLines, 500) + "*\n\n";
   }

   private String buildCommandLine(String executable, List<String> args) {
      StringBuilder commandLine = new StringBuilder();
      commandLine.append("`").append(this.escapeArgument(executable));
      if (args != null && !args.isEmpty()) {
         commandLine.append(" ");

         for(String arg : args) {
            commandLine.append(this.escapeArgument(arg)).append(" ");
         }

         commandLine.setLength(commandLine.length() - 1);
      }

      return commandLine.append("`").toString();
   }

   private String escapeArgument(String arg) {
      return arg != null && arg.contains(" ") ? "\"" + arg + "\"" : arg;
   }
}
