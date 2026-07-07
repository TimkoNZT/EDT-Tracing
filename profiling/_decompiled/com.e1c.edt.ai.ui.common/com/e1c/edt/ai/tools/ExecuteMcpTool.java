package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ExecuteMcpTool extends BaseExecuteMcpTool<Request> implements IMcpTool {
   public static final String TOOL_NAME = "Execute";
   private static String QuestionExample = "{\"executable\":\"cmd\",\"working_directory\":\"C:\\\\\",\"args\":[\"/c\",\"whoami\"],\"timeout\":3}";
   private static String AnswerExample = "{\"exit_code\":0,\"std_out\":\"john_smith\\n\",\"std_err\":\"\"}";
   private final McpToolCallSpecification spec = this.createSpecification();

   @Inject
   public ExecuteMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory, IProcessRunner processRunner) {
      super(environment, json, messageFactory, processRunner);
   }

   protected String getExecutable(Request request) {
      return request.executable;
   }

   protected Class<Request> getRequestType() {
      return Request.class;
   }

   protected String getQuestionExample() {
      return QuestionExample;
   }

   protected String getAnswerExample() {
      return AnswerExample;
   }

   protected void validateRequest(Request request) throws ToolException {
      if (request.executable == null || request.executable.isBlank()) {
         throw new ToolException("`executable` cannot be empty.");
      }
   }

   protected String getToolName() {
      return "Execute";
   }

   protected String getToolDescription() {
      StringBuilder description = new StringBuilder();
      description.append("Executes a system process.");
      description.append("\n\nUse for OS-level commands, not IDE actions.");
      description.append("\n\nRelated tools:");
      description.append("\n- IDE commands: `ExecuteCommand`.");
      return description.toString();
   }

   protected void addToolSpecificProperties(HashMap<String, McpToolCallProperty> properties) {
      McpToolCallProperty executableProp = new McpToolCallProperty();
      executableProp.type = "string";
      executableProp.description = "Executable name or path.";
      properties.put("executable", executableProp);
   }

   protected List<String> getRequiredParameters() {
      return Arrays.asList("executable");
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public static class Request extends BaseExecuteRequest {
      @SerializedName("executable")
      public String executable;
   }
}
