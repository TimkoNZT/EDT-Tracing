package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GitMcpTool extends BaseExecuteMcpTool<BaseExecuteRequest> implements IMcpTool {
   public static final String TOOL_NAME = "Git";
   private static String QuestionExample = "{\"working_directory\":\"C:\\\\Projects\",\"args\":[\"status\"]}";
   private static String AnswerExample = "{\"exit_code\":0,\"std_out\":\"On branch main\\n\",\"std_err\":\"\"}";
   private final McpToolCallSpecification spec = this.createSpecification();

   @Inject
   public GitMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory, IProcessRunner processRunner) {
      super(environment, json, messageFactory, processRunner);
   }

   protected String getExecutable(BaseExecuteRequest request) {
      return "git";
   }

   protected Class<BaseExecuteRequest> getRequestType() {
      return BaseExecuteRequest.class;
   }

   protected String getQuestionExample() {
      return QuestionExample;
   }

   protected String getAnswerExample() {
      return AnswerExample;
   }

   protected void validateRequest(BaseExecuteRequest request) throws ToolException {
   }

   protected String getToolName() {
      return "Git";
   }

   protected String getToolDescription() {
      return "Executes Git commands.\n\nUse for Git operations in repositories.";
   }

   protected void addToolSpecificProperties(HashMap<String, McpToolCallProperty> properties) {
   }

   protected List<String> getRequiredParameters() {
      return List.of();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<Boolean> getIsAvailable() {
      return this.processRunner.executeProcess("git", (String)null, List.of("--version"), 5L, TimeUnit.SECONDS, (Integer)null).thenApply((result) -> result.isPresent() && ((ProcessResult)result.get()).exitCode == 0).exceptionally((ex) -> false);
   }
}
