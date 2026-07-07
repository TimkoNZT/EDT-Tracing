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
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public class GetCommandCategoriesMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "GetCommandCategories";
   public static final CommandCategory UNCategorized = createUncategorizedCategory();
   private static String QuestionExample = "{}";
   private static String AnswerExample = "[\n  {\n    \"id\": \"system\",\n    \"name\": \"System Commands\",\n    \"description\": \"Commands for system management and control\"\n  },\n  {\n    \"id\": \"network\",\n    \"name\": \"Network\",\n    \"description\": \"Commands related to network configuration\"\n  },\n  {\n    \"id\": \"security\",\n    \"name\": \"Security Tools\",\n    \"description\": \"Security-related commands\"\n  },\n  {\n    \"id\": \"uncategorized\",\n    \"name\": \"Uncategorized\",\n    \"description\": \"Commands without category\"\n  }\n]";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IMarkdownUtils markdownUtils;

   @Inject
   public GetCommandCategoriesMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils) {
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
      if (call.callKind == ToolCallKind.RENDER) {
         details.requestMarkdown = Messages.CommandCategoriesTitle;
         return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
      } else {
         return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled()) {
               throw new ToolException("Operation was cancelled before execution.");
            } else {
               try {
                  ICommandService commandService = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
                  Category[] definedCategories = commandService.getDefinedCategories();
                  ArrayList<CommandCategory> categories = new ArrayList(definedCategories.length + 1);

                  for(Category src : definedCategories) {
                     CommandCategory dst = new CommandCategory();
                     dst.id = src.getId();

                     try {
                        dst.name = src.getName();
                     } catch (NotDefinedException var14) {
                        dst.name = "Undefined name";
                     }

                     try {
                        dst.description = src.getDescription();
                     } catch (NotDefinedException var13) {
                        dst.description = "No description available";
                     }

                     categories.add(dst);
                  }

                  boolean hasUncategorized = categories.stream().anyMatch((c) -> "uncategorized".equalsIgnoreCase(c.id));
                  if (!hasUncategorized) {
                     categories.add(UNCategorized);
                  }

                  String content = this.json.serialize(categories);
                  int categoryCount = categories.size();
                  String styledCategoryCount = this.markdownUtils.createStyledText(String.valueOf(categoryCount), TextColor.GREEN, FontWeight.BOLD, false);
                  details.responseMarkdown = MessageFormat.format(Messages.CommandCategoriesLoadedTemplate, styledCategoryCount);
                  return this.messageFactory.createMessage(this, call, content, details);
               } catch (Exception e) {
                  throw new ToolException("Failed to retrieve command categories", e, ToolErrorType.RETRYABLE);
               }
            }
         });
      }
   }

   private static CommandCategory createUncategorizedCategory() {
      CommandCategory category = new CommandCategory();
      category.id = "uncategorized";
      category.name = "Uncategorized";
      category.description = "Commands without specific category";
      return category;
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "GetCommandCategories";
      StringBuilder description = new StringBuilder();
      description.append("Lists IDE command categories.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Returns category id, name, and description.");
      description.append("\n\nRelated tools:");
      description.append("\n- List commands: `GetCommands`.");
      description.append("\n- Execute command: `ExecuteCommand`.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      parameters.properties = new HashMap();
      parameters.required = Collections.emptyList();
      spec.function.parameters = parameters;
      return spec;
   }
}
