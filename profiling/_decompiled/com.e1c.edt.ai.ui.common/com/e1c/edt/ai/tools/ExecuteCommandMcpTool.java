package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
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
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.commands.AbstractParameterValueConverter;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.ParameterType;
import org.eclipse.core.commands.ParameterValueConversionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

public class ExecuteCommandMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "ExecuteCommand";
   private static String QuestionExample = "{\n  \"command_id\": \"file_open\",\n  \"parameters\": [\n    {\n      \"id\": \"file_path\",\n      \"value\": \"/documents/report.txt\"\n    },\n    {\n      \"id\": \"file_encoding\",\n      \"value\": \"UTF-8\"\n    }\n  ]\n}";
   private static String AnswerExample = "";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IDispatcher dispatcher;

   @Inject
   public ExecuteCommandMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IDispatcher dispatcher) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(dispatcher);
      this.json = json;
      this.messageFactory = messageFactory;
      this.dispatcher = dispatcher;
      this.spec = createSpecification();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = false;
      if (call.callKind == ToolCallKind.RENDER) {
         details.requestMarkdown = Messages.ExecuteCommandTitle;
         return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
      } else {
         Optional<CommandDescription> optionalRequest = this.json.deserialize(call.function.arguments, CommandDescription.class);
         if (optionalRequest.isEmpty()) {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
         } else {
            CommandDescription request = (CommandDescription)optionalRequest.get();
            String commandId = request.id;
            if (commandId != null && !commandId.isBlank()) {
               List<CommandParameter> params = request.parameters;
               HashMap<String, Object> paramsMap = new HashMap();
               if (params != null) {
                  for(CommandParameter param : params) {
                     String id = param.id;
                     String val = param.value;
                     if (id != null && !id.isBlank() && val != null) {
                        paramsMap.put(param.id, param.value);
                     }
                  }
               }

               return CompletableFuture.supplyAsync(() -> {
                  if (cancellationToken.isCanceled()) {
                     throw new ToolException("Operation was cancelled before execution.");
                  } else {
                     ICommandService commandService = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
                     Command command = commandService.getCommand(commandId);
                     if (command == null) {
                        throw new ToolException("The command was not found.");
                     } else {
                        try {
                           IParameter[] commandParameters = command.getParameters();
                           if (commandParameters != null) {
                              StringBuilder paramsError = new StringBuilder();

                              for(IParameter commandParameter : commandParameters) {
                                 String id = commandParameter.getId();
                                 if (id != null && !id.isBlank()) {
                                    String val = (String)paramsMap.get(id);
                                    if (val == null) {
                                       if (!commandParameter.isOptional() && val == null) {
                                          paramsError.append("Missing required parameter with id \"");
                                          paramsError.append(id);
                                          paramsError.append("\"\n");
                                       }
                                    } else {
                                       IParameter param = command.getParameter(id);
                                       if (param != null) {
                                          ParameterType parameterType = command.getParameterType(id);
                                          if (parameterType != null) {
                                             AbstractParameterValueConverter valueConverter = parameterType.getValueConverter();
                                             if (valueConverter != null) {
                                                try {
                                                   Object obj = valueConverter.convertToObject(val);
                                                   paramsMap.put(id, obj);
                                                } catch (ParameterValueConversionException var20) {
                                                   paramsError.append("Cannot convert \"");
                                                   paramsError.append(val);
                                                   paramsError.append("\".");
                                                }
                                             }
                                          }
                                       }
                                    }
                                 } else {
                                    paramsError.append("Missing required parameter id.");
                                 }
                              }

                              if (paramsError.length() > 0) {
                                 throw new ToolException(paramsError.toString());
                              }
                           }
                        } catch (NotDefinedException var21) {
                        }

                        ParameterizedCommand parameterizedCommand = ParameterizedCommand.generateCommand(command, paramsMap);
                        if (parameterizedCommand == null) {
                           throw new ToolException("Invalid command parameter format.");
                        } else {
                           IHandlerService handlerService = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);
                           return (ToolCallMessage)this.dispatcher.dispatch((Supplier)(() -> this.executeCommand(handlerService, call, parameterizedCommand, details))).orElseThrow(() -> new ToolException("Cannot execute the command."));
                        }
                     }
                  }
               });
            } else {
               throw new ToolException("The command_id cannot be empty.");
            }
         }
      }
   }

   private ToolCallMessage executeCommand(IHandlerService handlerService, McpToolCall call, ParameterizedCommand parameterizedCommand, ToolCallMessageDetails details) {
      // $FF: Couldn't be decompiled
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "ExecuteCommand";
      StringBuilder description = new StringBuilder();
      description.append("Executes an IDE command by id.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Use this tool for IDE actions that require the IDE context.");
      description.append("\n- Provide required parameters for the command.");
      description.append("\n- Add a short description of what will be done.");
      description.append("\n\nRelated tools:");
      description.append("\n- Discover commands: `GetCommands`.");
      description.append("\n- Discover categories: `GetCommandCategories`.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty commandIdProp = new McpToolCallProperty();
      commandIdProp.type = "string";
      commandIdProp.description = "Command id.";
      properties.put("command_id", commandIdProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("command_id");
      spec.function.parameters = parameters;
      return spec;
   }
}
