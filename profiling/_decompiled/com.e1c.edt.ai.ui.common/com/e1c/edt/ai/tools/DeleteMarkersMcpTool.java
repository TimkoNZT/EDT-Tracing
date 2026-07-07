package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
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
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class DeleteMarkersMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "DeleteMarkers";
   private static String QuestionExample = "{\n  \"project_name\": \"MyProject\",\n  // \"marker_type\": \"ai_marker\", // Optional: bookmark, task, text, ai_marker (AIError/AIWarning/AIInfo)\n  // \"path\": \"C:/Projects/MyProject/optional/file/path.bsl\",\n  // \"id\": 12345 // Optional: specific marker ID\n}";
   private static String AnswerExample = "Markers cleared successfully";
   private final ILog log;
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;

   @Inject
   public DeleteMarkersMcpTool(ILog log, IJson json, IMcpToolsCallMessageFactory messageFactory) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      this.log = log;
      this.json = json;
      this.messageFactory = messageFactory;
      this.spec = this.createSpecification();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = true;
      details.hideAfter = true;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         String projectName = request.projectName;
         if (projectName != null && !projectName.isBlank()) {
            if (call.callKind == ToolCallKind.RENDER) {
               details.requestMarkdown = MessageFormat.format(Messages.RemoveMarkersTitleTemplate, ((Request)optionalRequest.get()).projectName);
               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            } else {
               return CompletableFuture.supplyAsync(() -> {
                  IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                  IProject project = root.getProject(projectName);
                  if (!project.exists()) {
                     throw new ToolException("Project not found: " + projectName);
                  } else if (!project.isOpen()) {
                     throw new ToolException("Project is closed: " + projectName);
                  } else {
                     try {
                        if (request.id != null) {
                           return this.deleteMarkerById(project, request.id, call, details);
                        } else {
                           MarkerType markerType = this.getMarkerType(request.markerType);
                           if (request.relativeFilePath != null && !request.relativeFilePath.isBlank()) {
                              IFile file = root.getFile(new Path(request.relativeFilePath));
                              if (file == null || !file.exists()) {
                                 throw new ToolException("File not found: " + request.relativeFilePath);
                              }

                              this.deleteMarkers(file, markerType);
                           } else {
                              this.deleteMarkers(project, markerType);
                           }

                           details.responseMarkdown = Messages.MarkersRemovedMessage;
                           return this.messageFactory.createMessage(this, call, "Markers cleared successfully", details);
                        }
                     } catch (IllegalArgumentException | CoreException error) {
                        this.log.logError(error);
                        throw new ToolException("Failed to clear markers", error, ToolErrorType.RETRYABLE);
                     }
                  }
               });
            }
         } else {
            throw new ToolException("Project name is required");
         }
      }
   }

   private ToolCallMessage deleteMarkerById(IProject project, long markerId, McpToolCall call, ToolCallMessageDetails details) {
      try {
         IMarker marker = this.findMarkerById(project, markerId);
         if (marker == null) {
            throw new ToolException("Marker not found with id: " + markerId);
         } else {
            marker.delete();
            details.responseMarkdown = Messages.MarkersRemovedMessage;
            return this.messageFactory.createMessage(this, call, "Marker with id " + markerId + " deleted successfully", details);
         }
      } catch (CoreException e) {
         this.log.logError(e);
         throw new ToolException("Failed to delete marker", e, ToolErrorType.RETRYABLE);
      }
   }

   private IMarker findMarkerById(IProject project, long markerId) throws CoreException {
      IMarker[] markers = project.findMarkers((String)null, true, 2);

      for(IMarker marker : markers) {
         if (marker.getId() == markerId) {
            return marker;
         }
      }

      return null;
   }

   private void deleteMarkers(IResource resource, MarkerType markerType) throws CoreException {
      if (markerType != null) {
         if (markerType == MarkerType.AI_MARKER) {
            String[] var6;
            for(String typeId : var6 = MarkerType.getAiMarkerTypeIds()) {
               resource.deleteMarkers(typeId, true, 2);
            }
         } else {
            resource.deleteMarkers(markerType.getTypeId(), true, 2);
         }
      } else {
         MarkerType[] var14;
         for(MarkerType type : var14 = MarkerType.values()) {
            if (type != MarkerType.PROBLEM) {
               if (type == MarkerType.AI_MARKER) {
                  String[] var10;
                  for(String typeId : var10 = MarkerType.getAiMarkerTypeIds()) {
                     resource.deleteMarkers(typeId, true, 2);
                  }
               } else {
                  resource.deleteMarkers(type.getTypeId(), true, 2);
               }
            }
         }
      }

   }

   private MarkerType getMarkerType(String markerType) throws IllegalArgumentException {
      if (markerType != null && !markerType.isBlank()) {
         MarkerType type = MarkerType.fromDisplayName(markerType);
         if (type == null) {
            throw new IllegalArgumentException("Unknown marker type: " + markerType);
         } else {
            return type;
         }
      } else {
         return null;
      }
   }

   private McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "DeleteMarkers";
      StringBuilder description = new StringBuilder();
      description.append("Removes markers from a project or file.");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- By default removes all non-problem markers.");
      description.append("\n- Use `marker_type` to remove specific marker types.");
      description.append("\n- Use `id` to remove a single marker.");
      description.append("\n- `ai_marker` removes `AIError`, `AIWarning`, `AIInfo` marker types.");
      description.append("\n\nRelated tools:");
      description.append("\n- List markers: `GetMarkers`.");
      description.append("\n- Recreate markers: `SetMarkers`.");
      description.append("\n\nParameters:\n");
      description.append("- project_name: Name of target project (required)\n");
      description.append("- marker_type: Optional type of markers to remove:\n");

      MarkerType[] var6;
      for(MarkerType type : var6 = MarkerType.values()) {
         if (type != MarkerType.PROBLEM) {
            description.append("  - ").append(type.getDisplayName()).append("\n");
         }
      }

      description.append("- path: Optional relative path to file\n");
      description.append("- id: Optional ID of specific marker to delete\n\n");
      description.append("Example requests:\n");
      description.append("- Clear all non-problem markers in project:\n").append(QuestionExample);
      description.append("- Clear AI markers in specific file:\n");
      description.append("  {\"project_name\":\"MyProject\",\"marker_type\":\"ai_marker\",\"path\":\"src/Module.bsl\"} // removes AIError, AIWarning, AIInfo");
      description.append("\n- Delete specific marker by ID:\n");
      description.append("  {\"project_name\":\"MyProject\",\"id\":12345}");
      description.append("\n\nExample response:\n").append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty projectNameProp = new McpToolCallProperty();
      projectNameProp.type = "string";
      projectNameProp.description = "Name of the target project";
      properties.put("project_name", projectNameProp);
      McpToolCallProperty markerTypeProp = new McpToolCallProperty();
      markerTypeProp.type = "string";
      markerTypeProp.description = "Type of markers to remove";
      properties.put("marker_type", markerTypeProp);
      McpToolCallProperty filePathProp = new McpToolCallProperty();
      filePathProp.type = "string";
      filePathProp.description = "Relative path to target file from project root";
      properties.put("path", filePathProp);
      McpToolCallProperty idProp = new McpToolCallProperty();
      idProp.type = "number";
      idProp.description = "ID of specific marker to delete";
      properties.put("id", idProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("project_name");
      spec.function.parameters = parameters;
      return spec;
   }

   private static class Request {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("marker_type")
      public String markerType;
      @SerializedName("path")
      public String relativeFilePath;
      @SerializedName("id")
      public Long id;
   }
}
