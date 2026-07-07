package com.e1c.edt.ai.context.tools;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.bm.integration.IBmTask;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupport;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.form.model.Form;
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
import com.e1c.edt.ai.context.IEntityFactory;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

public class GetObjectMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "1C_GetObject";
   private static String QuestionExample = "{\n  \"project_name\": \"MyProject\",\n  \"object_id\": 17239405821723\n}";
   private static String AnswerExample = "{\n  \"resoure_uri\": \"bm:...\",\n  \"fqn\": \"Catalog.MyCatalog\",\n  \"is_top\": true,\n  \"path\": \"C:/Projects/MyProject/Catalogs/MyCatalog/Forms/ItemForm/Ext/Module.bsl\"\n}";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IBmModelManager modelManager;
   private final IEntityFactory entityFactory;
   private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;

   @Inject
   public GetObjectMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IBmModelManager modelManager, IEntityFactory entityFactory, IProjectFileSystemSupportProvider projectFileSystemSupportProvider) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(modelManager);
      Preconditions.checkNotNull(entityFactory);
      Preconditions.checkNotNull(projectFileSystemSupportProvider);
      this.json = json;
      this.messageFactory = messageFactory;
      this.modelManager = modelManager;
      this.entityFactory = entityFactory;
      this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
      this.spec = createSpecification();
   }

   public boolean isExperimental() {
      return false;
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = true;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         String projectName = request.projectName;
         Long objectId = request.objectId;
         if (projectName != null && !projectName.isBlank()) {
            if (objectId == null) {
               throw new ToolException("Object id is required.");
            } else if (call.callKind == ToolCallKind.RENDER) {
               details.requestMarkdown = Messages.Get1CObjectByIdTitle;
               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            } else {
               return CompletableFuture.supplyAsync(() -> {
                  if (cancellationToken.isCanceled()) {
                     throw new ToolException("Operation was cancelled before execution.");
                  } else {
                     IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                     IProject project = root.getProject(projectName);
                     if (project != null && project.exists()) {
                        IBmModel model = this.modelManager.getModel(project);
                        if (model == null) {
                           throw new ToolException("Model not available for project: " + projectName);
                        } else {
                           try {
                              IBmObject bmObject = (IBmObject)model.executeReadonlyTask(new IBmTask<IBmObject>() {
                                 public IBmObject execute(IBmTransaction transaction, IProgressMonitor progressMonitor) {
                                    if (cancellationToken.isCanceled()) {
                                       throw new ToolException("Operation cancelled during BM task execution", (Throwable)null, ToolErrorType.RETRYABLE);
                                    } else {
                                       return transaction.getObjectById(objectId);
                                    }
                                 }

                                 public Object getId() {
                                    return "GetObjectByIdMcpTool/" + objectId;
                                 }

                                 public String getName() {
                                    return "Get object by id: " + objectId;
                                 }

                                 public Object getServiceId() {
                                    return "GetObjectByIdMcpTool";
                                 }
                              });
                              if (bmObject == null) {
                                 throw new ToolException("Object not found: " + objectId);
                              } else {
                                 Response response = new Response();
                                 response.resourceUri = bmObject.bmGetUriAsString();
                                 response.fqn = bmObject.bmGetFqn();
                                 response.isTop = bmObject.bmIsTop();
                                 IBmObject topObject = bmObject.bmGetTopObject();
                                 if (topObject != null) {
                                    response.topObjectId = topObject.bmGetId();
                                 }

                                 IProjectFileSystemSupport fileSystem = this.projectFileSystemSupportProvider.getProjectFileSystemSupport(project);
                                 if (fileSystem != null) {
                                    IFile file = fileSystem.getFile(bmObject);
                                    if (file != null) {
                                       IPath location = file.getRawLocation();
                                       if (location != null) {
                                          response.path = location.toOSString();
                                       }
                                    }
                                 }

                                 if (cancellationToken.isCanceled()) {
                                    throw new ToolException("Operation was cancelled during entity creation.");
                                 } else {
                                    if (bmObject instanceof Form) {
                                       response.objectModel = this.entityFactory.createFormEntity((Form)bmObject, cancellationToken).orElse((Object)null);
                                    } else {
                                       response.objectModel = this.entityFactory.createMetaEntity(bmObject, cancellationToken);
                                    }

                                    String content = this.json.serialize(response);
                                    details.responseMarkdown = MessageFormat.format(Messages.ObjectRetrievedTemplate, response.fqn != null ? response.fqn : "Unknown");
                                    return this.messageFactory.createMessage(this, call, content, details);
                                 }
                              }
                           } catch (OperationCanceledException e) {
                              throw new ToolException("Operation cancelled", e, ToolErrorType.RETRYABLE);
                           } catch (Exception e) {
                              throw new ToolException("Cannot get object by id", e, ToolErrorType.RETRYABLE);
                           }
                        }
                     } else {
                        throw new ToolException("Project not found: " + projectName);
                     }
                  }
               });
            }
         } else {
            throw new ToolException("Project name is required.");
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "1C_GetObject";
      StringBuilder description = new StringBuilder();
      description.append("Returns a 1C configuration object by id.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Requires a valid object id.");
      description.append("\n\nRelated tools:");
      description.append("\n- Find ids: `1C_Find`.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty projectNameProp = new McpToolCallProperty();
      projectNameProp.type = "string";
      projectNameProp.description = "1C project name where the object is located.";
      properties.put("project_name", projectNameProp);
      McpToolCallProperty objectIdProp = new McpToolCallProperty();
      objectIdProp.type = "number";
      objectIdProp.description = "Unique identifier of the 1C configuration object.";
      properties.put("object_id", objectIdProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("project_name", "object_id");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("object_id")
      public Long objectId;
   }

   private static class Response {
      @SerializedName("resource_uri")
      public String resourceUri;
      @SerializedName("fqn")
      public String fqn;
      @SerializedName("is_top")
      public boolean isTop;
      @SerializedName("top_object_id")
      public Long topObjectId;
      @SerializedName("object_model")
      public Object objectModel;
      @SerializedName("path")
      public String path;
   }
}
