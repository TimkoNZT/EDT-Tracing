package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationToken;
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
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class JShellSessionMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "JShellSession";
   private static final String AnswerExample = "{\"repl_session_id\":123,\"available_bindings\":[\"workbench\"]}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IJShellSessionManager sessions;
   private final Set<IJShellBindingProvider> bindingProviders;

   @Inject
   public JShellSessionMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IJShellSessionManager sessions, Set<IJShellBindingProvider> bindingProviders) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(sessions);
      Preconditions.checkNotNull(bindingProviders);
      this.json = json;
      this.messageFactory = messageFactory;
      this.sessions = sessions;
      this.bindingProviders = bindingProviders;
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
      if (call.callKind == ToolCallKind.RENDER) {
         details.requestMarkdown = Messages.JShellSessionCreating;
         return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
      } else {
         return CompletableFuture.supplyAsync(() -> this.createSession(call, details, cancellationToken));
      }
   }

   private ToolCallMessage createSession(McpToolCall call, ToolCallMessageDetails details, ICancellationToken cancellationToken) {
      if (cancellationToken.isCanceled()) {
         throw new ToolException("Operation was cancelled before execution.", (Throwable)null, ToolErrorType.RETRYABLE);
      } else {
         try {
            IJShellSession session = this.sessions.getOrCreateSession(0);
            SessionResult result = session.getSessionResult();
            String content = this.json.serialize(result);
            details.responseMarkdown = Messages.JShellSessionCreated;
            return this.messageFactory.createMessage(this, call, content, details);
         } catch (Exception e) {
            throw new ToolException("JShell session creation failed: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
         }
      }
   }

   private McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "JShellSession";
      StringBuilder description = new StringBuilder();
      description.append("Creates a new JShell REPL session.");
      description.append("\n\n**Purpose:**");
      description.append("\n- JShell provides access to Eclipse API");
      description.append("\n- Use when you need to perform Eclipse-specific operations not available via other tools");
      description.append("\n- Session preserves state between JShell calls");
      description.append("\n\n**When to use:**");
      description.append("\n- Before calling ").append("JShell").append(" tool (required)");
      description.append("\n- To check available bindings and execution history");
      description.append("\n- To get session info for existing session");
      description.append("\n\n**Usage:**");
      description.append("\n- If `repl_session_id` is not provided → creates NEW session");
      description.append("\n- If `repl_session_id` is provided → returns info about EXISTING session");
      description.append("\n- Returns: session ID, available bindings, execution history");
      description.append("\n\n### Available bindings:");
      description.append("\nPre-configured Eclipse objects available in JShell sessions:");
      if (!this.bindingProviders.isEmpty()) {
         for(IJShellBindingProvider provider : this.bindingProviders) {
            Map<String, JShellBindingDescription> descriptions = provider.getBindings();
            if (!descriptions.isEmpty()) {
               description.append("\n");

               for(Map.Entry<String, JShellBindingDescription> entry : descriptions.entrySet()) {
                  String bindingName = (String)entry.getKey();
                  JShellBindingDescription bindingInfo = (JShellBindingDescription)entry.getValue();
                  String bindingDesc = bindingInfo.getDescription();
                  String bindingExample = bindingInfo.getExample();
                  String bindingRestriction = bindingInfo.getRestriction();
                  description.append("\n**`").append(bindingName).append("`**");
                  description.append("\n").append(bindingDesc);
                  if (bindingRestriction != null && !bindingRestriction.isEmpty()) {
                     description.append("\n\n").append(bindingRestriction);
                  }

                  if (bindingExample != null && !bindingExample.isEmpty()) {
                     description.append("\n```java\n").append(bindingExample).append("\n```");
                  }
               }
            }
         }
      }

      description.append("\n\n**Parameter:**");
      description.append("\n- `repl_session_id` (optional): Session ID. If not provided, creates new session.");
      description.append("\n\n**Examples:**");
      description.append("\n  Create new session: Q: {}");
      description.append("\n  A: ").append("{\"repl_session_id\":123,\"available_bindings\":[\"workbench\"]}");
      description.append("\n  Get existing session: Q: {\"repl_session_id\": 123}");
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty sessionIdProp = new McpToolCallProperty();
      sessionIdProp.type = "integer";
      sessionIdProp.description = "Optional session ID. If 0 or not provided, a new session will be created. If provided, returns info about the existing session.";
      properties.put("repl_session_id", sessionIdProp);
      parameters.properties = properties;
      ArrayList<String> required = new ArrayList();
      parameters.required = required;
      spec.function.parameters = parameters;
      return spec;
   }
}
