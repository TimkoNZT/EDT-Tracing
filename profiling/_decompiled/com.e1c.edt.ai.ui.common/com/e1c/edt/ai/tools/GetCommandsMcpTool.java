package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.commands.ParameterType;
import org.eclipse.core.commands.ParameterValuesException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;

public class GetCommandsMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "GetCommands";
   public static final Comparator<CommandDescription> COMPARATOR = Comparator.comparing((i) -> {
      if (i.parameters != null && i.parameters.size() != 0) {
         return i.parameters.size() == 1 ? 0 : i.parameters.size();
      } else {
         return 1;
      }
   });
   private static String QuestionExample = "{\n  \"category_id\": \"system\"\n}";
   private static String AnswerExample = "[\n  {\n    \"return_is_defined\": true,\n    \"return_type_id\": \"string\",\n    \"is_enabled\": true,\n    \"description\": \"Initiates system shutdown\",\n    \"name\": \"Shutdown\",\n    \"id\": \"shutdown\",\n    \"parameters\": [\n      {\n        \"is_optional\": false,\n        \"values\": {\n          \"type\": \"integer\",\n          \"min\": 0,\n          \"max\": 60\n        },\n        \"name\": \"Delay (seconds)\",\n        \"id\": \"param_delay\"\n        \"friendly_id\": \"delay\"\n      },\n      {\n        \"is_optional\": true,\n        \"values\": {\n          \"type\": \"boolean\",\n          \"default\": false\n        },\n        \"name\": \"Force\",\n        \"id\": \"param_force\"\n      }\n    ]\n  },\n  {\n    \"return_is_defined\": false,\n    \"return_type_id\": \"void\",\n    \"is_enabled\": true,\n    \"description\": \"Reboots the system\",\n    \"name\": \"Reboot\",\n    \"id\": \"reboot\",\n    \"parameters\": [\n      {\n        \"is_optional\": true,\n        \"values\": {\n          \"options\": [\"safe\", \"full\", \"recovery\"]\n        },\n        \"name\": \"Mode\",\n        \"id\": \"param_mode\"\n      }\n    ]\n  },\n  {\n    \"return_is_defined\": true,\n    \"return_type_id\": \"TemperatureData\",\n    \"is_enabled\": true,\n    \"description\": \"Gets current CPU temperature\",\n    \"name\": \"Get CPU Temp\",\n    \"id\": \"cmd_cpu_temp\",\n    \"parameters\": []\n  }\n]";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IMarkdownUtils markdownUtils;

   @Inject
   public GetCommandsMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(markdownUtils);
      this.json = json;
      this.messageFactory = messageFactory;
      this.markdownUtils = markdownUtils;
      this.spec = createSpecification();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = true;
      details.hideAfter = true;
      Optional<CommandCategory> optionalRequest = this.json.deserialize(call.function.arguments, CommandCategory.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
      } else {
         CommandCategory request = (CommandCategory)optionalRequest.get();
         String categoryId = request.id;
         if (call.callKind == ToolCallKind.RENDER) {
            details.requestMarkdown = Messages.CommandsTitle;
            return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
         } else {
            return CompletableFuture.supplyAsync(() -> {
               if (cancellationToken.isCanceled()) {
                  throw new ToolException("Operation was cancelled before execution.", (Throwable)null, ToolErrorType.RETRYABLE);
               } else {
                  ICommandService commandService = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
                  IBindingService bindingService = (IBindingService)PlatformUI.getWorkbench().getService(IBindingService.class);
                  ArrayList<CommandDescription> commands = new ArrayList();

                  Command[] var11;
                  for(Command src : var11 = commandService.getDefinedCommands()) {
                     try {
                        if (!src.isEnabled()) {
                           continue;
                        }
                     } catch (Throwable var27) {
                        continue;
                     }

                     if (!categoryId.equalsIgnoreCase(GetCommandCategoriesMcpTool.UNCategorized.id)) {
                        try {
                           Category catogory = src.getCategory();
                           if (!categoryId.equalsIgnoreCase(catogory.getId())) {
                              continue;
                           }
                        } catch (NotDefinedException var26) {
                        }
                     }

                     CommandDescription dst = new CommandDescription();
                     dst.id = src.getId();

                     try {
                        dst.name = src.getName();
                     } catch (NotDefinedException var24) {
                     }

                     try {
                        dst.description = src.getDescription();
                     } catch (NotDefinedException var23) {
                     }

                     try {
                        ParameterType returnType = src.getReturnType();
                        if (returnType != null) {
                           dst.returnTypeId = returnType.getId();
                           dst.returnIsDefined = returnType.isDefined();
                        }
                     } catch (NotDefinedException var22) {
                     }

                     try {
                        IParameter[] params = src.getParameters();
                        if (params != null) {
                           ArrayList<CommandParameter> commandParameters = new ArrayList();

                           for(IParameter param : params) {
                              CommandParameter commandParameter = new CommandParameter();
                              commandParameters.add(commandParameter);
                              commandParameter.id = param.getId();
                              commandParameter.name = param.getName();
                              commandParameter.isOptional = param.isOptional();

                              try {
                                 IParameterValues vals = param.getValues();
                                 if (vals != null) {
                                    commandParameter.values = vals.getParameterValues();
                                 }
                              } catch (ParameterValuesException var21) {
                              }
                           }

                           if (!commandParameters.isEmpty()) {
                              dst.parameters = commandParameters;
                           }
                        }
                     } catch (NotDefinedException var25) {
                     }

                     TriggerSequence[] bindings = bindingService.getActiveBindingsFor(src.getId());
                     if (bindings != null && bindings.length > 0) {
                        dst.hotKey = bindings[0].format();
                     }

                     commands.add(dst);
                  }

                  String content = this.json.serialize(commands.stream().sorted(COMPARATOR).collect(Collectors.toList()));
                  int commandCount = commands.size();
                  String styledCommandCount = this.markdownUtils.createStyledText(String.valueOf(commandCount), TextColor.GREEN, FontWeight.BOLD, false);
                  details.responseMarkdown = MessageFormat.format(Messages.CommandsLoadedTemplate, styledCommandCount);
                  return this.messageFactory.createMessage(this, call, content, details);
               }
            });
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "GetCommands";
      StringBuilder description = new StringBuilder();
      description.append("Lists IDE commands and their metadata.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Use category ids to scope results.");
      description.append("\n- Add a description of what will be done when calling a command.");
      description.append("\n\nRelated tools:");
      description.append("\n- List categories: `GetCommandCategories`.");
      description.append("\n- Execute command: `ExecuteCommand`.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty categoryIdProp = new McpToolCallProperty();
      categoryIdProp.type = "string";
      categoryIdProp.description = "Command caterogy id.";
      properties.put("category_id", categoryIdProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("category_id");
      spec.function.parameters = parameters;
      return spec;
   }
}
