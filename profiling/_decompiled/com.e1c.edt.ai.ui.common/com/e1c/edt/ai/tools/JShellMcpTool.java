package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
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
import com.e1c.edt.ai.ui.IDispatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class JShellMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "JShell";
   private static String QuestionExample = "{\"code\":\"var window = workbench.getActiveWorkbenchWindow();\\nif (window != null) { System.out.println(\\\"Active window: \\\" + window.getShell().getText()); }\",\"repl_session_id\":123}";
   private static String AnswerExample = "{\"return_value\":null,\"repl_session_id\":123,\"std_out\":\"Active window: Eclipse\\n\",\"std_err\":\"\"}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IJShellSessionManager sessions;
   private final Set<IJShellBindingProvider> bindingProviders;
   private final IMarkdownUtils markdownUtils;
   private final IRestrictedTypesProvider restrictedTypesProvider;
   private final IDispatcher dispatcher;

   @Inject
   public JShellMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IJShellSessionManager sessions, Set<IJShellBindingProvider> bindingProviders, IMarkdownUtils markdownUtils, IRestrictedTypesProvider restrictedTypesProvider, IDispatcher dispatcher) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(sessions);
      Preconditions.checkNotNull(bindingProviders);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(restrictedTypesProvider);
      Preconditions.checkNotNull(dispatcher);
      this.json = json;
      this.messageFactory = messageFactory;
      this.sessions = sessions;
      this.bindingProviders = bindingProviders;
      this.markdownUtils = markdownUtils;
      this.restrictedTypesProvider = restrictedTypesProvider;
      this.dispatcher = dispatcher;
      this.spec = this.createSpecification();
   }

   public boolean isExperimental() {
      return true;
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = false;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         if (call.callKind == ToolCallKind.RENDER) {
            StringBuilder requestMarkdown = new StringBuilder();
            requestMarkdown.append(Messages.JShellExecutingTemplate);
            requestMarkdown.append("\n\n");
            requestMarkdown.append("```java\n").append(request.code).append("\n```");
            details.requestMarkdown = requestMarkdown.toString();
            return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
         } else {
            return CompletableFuture.supplyAsync(() -> this.executeCode(request, call, details, cancellationToken));
         }
      }
   }

   private ToolCallMessage executeCode(Request request, McpToolCall call, ToolCallMessageDetails details, ICancellationToken cancellationToken) {
      if (cancellationToken.isCanceled()) {
         throw new ToolException("Operation was cancelled before execution.", (Throwable)null, ToolErrorType.RETRYABLE);
      } else {
         try {
            IJShellSession session = this.sessions.getSession(request.sessionId);
            if (session == null) {
               throw new ToolException("Session not found. Use JShellSession tool to create a session first.", (Throwable)null, ToolErrorType.RETRYABLE);
            } else {
               Optional<JShellExecutionResult> optionalResult = this.dispatcher.dispatch((Supplier)(() -> session.execute(request.code)));
               if (optionalResult.isEmpty()) {
                  throw new ToolException("Can't execute code.", (Throwable)null, ToolErrorType.RETRYABLE);
               } else {
                  JShellExecutionResult result = (JShellExecutionResult)optionalResult.get();
                  String content = this.json.serialize(result);
                  String responseMarkdown = this.buildResponseMarkdown(request.code, result);
                  details.responseMarkdown = responseMarkdown;
                  return this.messageFactory.createMessage(this, call, content, details);
               }
            }
         } catch (ToolException e) {
            throw e;
         } catch (Exception e) {
            throw new ToolException("JShell execution failed: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
         }
      }
   }

   private String buildResponseMarkdown(String code, JShellExecutionResult result) {
      StringBuilder md = new StringBuilder();
      if (!result.compilationErrors.isEmpty()) {
         md.append(Messages.JShellErrorTemplate);
      } else {
         md.append(Messages.JShellExecutedTemplate);
      }

      md.append("\n\n");
      md.append("```java\n").append(code).append("\n```\n");
      if (result.stdOut != null && !result.stdOut.isEmpty()) {
         md.append(this.markdownUtils.escapeForMarkdown(result.stdOut));
         md.append("\n");
      }

      return md.toString();
   }

   private String getBindingVariableNamesExample() {
      ArrayList<String> bindingNames = new ArrayList();

      for(IJShellBindingProvider provider : this.bindingProviders) {
         Map<String, JShellBindingDescription> bindings = provider.getBindings();
         if (bindings != null) {
            bindingNames.addAll(bindings.keySet());
         }
      }

      if (bindingNames.isEmpty()) {
         return "";
      } else {
         bindingNames.sort(String::compareTo);
         ArrayList<String> exampleNames = new ArrayList();

         for(int i = 0; i < Math.min(3, bindingNames.size()); ++i) {
            exampleNames.add("`" + (String)bindingNames.get(i) + "`");
         }

         return String.join(", ", exampleNames);
      }
   }

   private String getProviderUseCases(IJShellBindingProvider provider) {
      return provider.getUseCases();
   }

   private McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "JShell";
      StringBuilder description = new StringBuilder();
      description.append("Executes Java code using JShell REPL. Preserves state across executions.");
      description.append("\n\n**IMPORTANT:**");
      description.append("\n- You MUST call ").append("JShellSession").append(" tool first to create or get a valid session ID");
      description.append("\n- This tool will fail with error if you provide an invalid or non-existent session ID");
      description.append("\n\n**When to use:**");
      description.append("\n- Use when other IDE tools (Git , Read, etc.) cannot accomplish the task");
      description.append("\n\n**Key requirements:**");
      description.append("\n- Use ONLY complete statements with `;` (e.g., `int x = 10;`)");
      description.append("\n- NO expressions like `x`, `2+2` - use `System.out.println()` instead");
      description.append("\n- Output MUST be in main thread for result capture");
      description.append("\n\n**Available bindings:**");
      if (!this.bindingProviders.isEmpty()) {
         description.append("\n- Pre-configured objects are available in JShell");
         description.append("\n- See ").append("JShellSession").append(" tool for detailed binding documentation with examples");
         String bindingExamples = this.getBindingVariableNamesExample();
         description.append("\\n- **IMPORTANT:** Bindings are already available as variables (e.g., ").append(bindingExamples).append("). DO NOT use `JShellObjectBridge.retrieve()` - it's for internal use only.");
         description.append("\n\n**Binding providers:**");

         for(IJShellBindingProvider provider : this.bindingProviders) {
            Map<String, JShellBindingDescription> descriptions = provider.getBindings();
            if (!descriptions.isEmpty()) {
               description.append("\n\n**");
               description.append(provider.getDescription());
               description.append("**");
               String useCases = this.getProviderUseCases(provider);
               if (!useCases.isEmpty()) {
                  description.append("\n\n");
                  description.append(useCases);
               }

               description.append("\n\nAvailable bindings:");
               int count = 0;

               for(Map.Entry<String, JShellBindingDescription> entry : descriptions.entrySet()) {
                  if (count >= 3) {
                     description.append("\n- ... and more (see JShellSession tool)");
                     break;
                  }

                  String bindingName = (String)entry.getKey();
                  JShellBindingDescription bindingInfo = (JShellBindingDescription)entry.getValue();
                  String bindingRestriction = bindingInfo.getRestriction();
                  description.append("\n- `").append(bindingName).append("`: ").append(bindingInfo.getDescription());
                  if (bindingRestriction != null && !bindingRestriction.isEmpty()) {
                     description.append(" (has restriction)");
                  }

                  ++count;
               }
            }
         }
      }

      description.append("\n\n**Workflow:**");
      description.append("\n1. Call ").append("JShellSession").append(" to create/get session and ID");
      description.append("\n2. Use ").append("JShell").append(" with that ID to execute code");
      description.append("\n3. Reuse same ID to maintain state");
      Set<String> restrictedTypes = this.restrictedTypesProvider.getRestrictedTypes();
      if (!restrictedTypes.isEmpty()) {
         description.append("\n\n**⚠️ RESTRICTED TYPES (security restrictions):**");
         description.append("\nThe following types are NOT ALLOWED:");

         for(String type : (List)restrictedTypes.stream().sorted().collect(Collectors.toList())) {
            description.append("\n- `").append(type).append("`");
         }
      }

      description.append("\n\n**Parameters:**");
      description.append("\n- `code` (required): Complete Java statements ending with `;`");
      description.append("\n- `repl_session_id` (required): Session ID from JShellSession tool");
      description.append("\n\nExample: `").append(QuestionExample).append("`");
      description.append("\nResponse: `").append(AnswerExample).append("`");
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty codeProp = new McpToolCallProperty();
      codeProp.type = "string";
      codeProp.description = "Java code to execute (required)";
      properties.put("code", codeProp);
      McpToolCallProperty sessionIdProp = new McpToolCallProperty();
      sessionIdProp.type = "integer";
      sessionIdProp.description = "Session ID from JShellSession tool (required)";
      properties.put("repl_session_id", sessionIdProp);
      parameters.properties = properties;
      ArrayList<String> required = new ArrayList();
      required.add("code");
      required.add("repl_session_id");
      parameters.required = required;
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("code")
      public String code;
      @SerializedName("repl_session_id")
      public int sessionId;
   }
}
