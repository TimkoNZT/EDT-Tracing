package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;

public class SetMarkersMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "SetMarkers";
   public static final String ACTION_CALL_ATTRIBUTE = "action_call";
   public static final String ACTION_DETAILS_ATTRIBUTE = "action_details";
   private static String QuestionExample = "{\n  // project_name is optional - can be determined from absolute file paths\n  // \"project_name\": \"MyProject\",\n  \"markers\": [\n    {\n      \"type\": \"bookmark\",\n      \"path\": \"C:/Projects/MyProject/Forms/MyForm/Module.bsl\",\n      \"marker_line\": 10,\n      \"marker_highlighted_text\": \"Important code section\",\n      \"message\": \"Important code section that requires attention\"\n    },\n    {\n      \"type\": \"task\",\n      \"path\": \"C:/Projects/MyProject/CommonModules/MyModule/Module.bsl\",\n      \"marker_line\": 25,\n      \"marker_highlighted_text\": \"RefactorThisCode()\",\n      \"message\": \"TODO: Refactor this code\",\n      \"priority\": \"normal\"\n    },\n    {\n      \"type\": \"problem\",\n      \"path\": \"C:/Projects/MyProject/CommonModules/AnotherModule/Module.bsl\",\n      \"marker_line\": 42,\n      \"marker_highlighted_text\": \"a = 1 / 0\",\n      \"message\": \"Syntax error\",\n      \"severity\": \"error\"\n    },\n    {\n      \"type\": \"text\",\n      \"path\": \"C:/Projects/MyProject/CommonModules/TextModule/Module.bsl\",\n      \"marker_line\": 15,\n      \"marker_highlighted_text\": \"Important text note\",\n      \"message\": \"Important text note\"\n    },\n    {\n      \"type\": \"ai_marker\",\n      \"path\": \"C:/Projects/MyProject/CommonModules/AIModule/Module.bsl\",\n      \"marker_line\": 30,\n      \"marker_highlighted_text\": \"calculateTotal(items)\",\n      \"message\": \"AI warning (AIWarning)\",\n      \"severity\": \"warning\",\n      \"action_prompt\": \"Please refactor this code to use modern patterns\",\n      \"action_title\": \"Refactor code\",\n      \"action_description\": \"Update code to use modern design patterns\"\n    },\n    {\n      \"type\": \"ai_marker\",\n      \"path\": \"C:/Projects/MyProject/CommonModules/AIModule/Module.bsl\",\n      \"marker_line\": 45,\n      \"marker_highlighted_text\": \"calculateTotal(items)\",\n      \"message\": \"AI error (AIError)\",\n      \"severity\": \"error\",\n      \"action_prompt\": \"Fix the error in calculateTotal\",\n      \"action_title\": \"Fix error\",\n      \"action_description\": \"Correct the issue to prevent runtime failure\"\n    },\n    {\n      \"type\": \"ai_marker\",\n      \"path\": \"C:/Projects/MyProject/CommonModules/AIModule/Module.bsl\",\n      \"marker_line\": 60,\n      \"marker_highlighted_text\": \"calculateTotal(items)\",\n      \"message\": \"AI info (AIInfo)\",\n      \"severity\": \"info\",\n      \"action_prompt\": \"Consider adding a null check\",\n      \"action_title\": \"Improve safety\",\n      \"action_description\": \"Add a null check to make the code safer\"\n    }\n  ]\n}";
   private static String AnswerExample = "Successfully created 5 markers";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IContentSourceProvider contentSourceProvider;
   private final IFileSystem fileSystem;
   private final IProjectTools projectTools;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$tools$MarkerType;

   @Inject
   public SetMarkersMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IContentSourceProvider contentSourceProvider, IFileSystem fileSystem, IProjectTools projectTools) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(contentSourceProvider);
      Preconditions.checkNotNull(fileSystem);
      Preconditions.checkNotNull(projectTools);
      this.json = json;
      this.messageFactory = messageFactory;
      this.contentSourceProvider = contentSourceProvider;
      this.fileSystem = fileSystem;
      this.projectTools = projectTools;
      this.spec = createSpecification();
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = true;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Invalid request format. Example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         if (request.markers != null && !request.markers.isEmpty()) {
            String projectName = request.projectName;
            if (call.callKind != ToolCallKind.RENDER) {
               return CompletableFuture.supplyAsync(() -> {
                  IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                  String deteminedProjectName = projectName;
                  if (projectName == null || projectName.isBlank()) {
                     MarkerRequest firstMarker = (MarkerRequest)request.markers.get(0);
                     deteminedProjectName = this.projectTools.determineProjectName(firstMarker.absoluteFilePath);
                     if (deteminedProjectName == null) {
                        throw new ToolException("Cannot determine project from file path: " + firstMarker.absoluteFilePath + ". Please specify project_name explicitly.");
                     }
                  }

                  IProject project = root.getProject(deteminedProjectName);
                  if (!project.exists()) {
                     throw new ToolException("Project not found: " + deteminedProjectName);
                  } else if (!project.isOpen()) {
                     throw new ToolException("Project is closed: " + deteminedProjectName);
                  } else {
                     int markersSet = 0;
                     ArrayList<IMarker> createdMarkers = new ArrayList();
                     StringBuilder errors = new StringBuilder();

                     for(int i = 0; i < request.markers.size(); ++i) {
                        if (cancellationToken.isCanceled()) {
                           throw new ToolException("Operation cancelled after setting " + markersSet + " markers");
                        }

                        MarkerRequest markerReq = (MarkerRequest)request.markers.get(i);
                        markerReq.projectName = project.getName();

                        try {
                           createdMarkers.add(this.createMarker(project, call, markerReq));
                           ++markersSet;
                        } catch (IllegalArgumentException | BadLocationException | CoreException error) {
                           errors.append("Marker [").append(i).append("] error: ").append(((Exception)error).getMessage()).append("; ");
                        }
                     }

                     if (errors.length() <= 0) {
                        details.responseMarkdown = Messages.MarkersCreatedMessage;
                        return this.messageFactory.createMessage(this, call, "Successfully created " + markersSet + " markers", details);
                     } else {
                        for(IMarker marker : createdMarkers) {
                           try {
                              marker.delete();
                           } catch (CoreException deleteError) {
                              errors.append("Rollback error: ").append(deleteError.getMessage()).append("; ");
                           }
                        }

                        throw new ToolException("Completed with errors: " + errors + ". Markers set: " + markersSet);
                     }
                  }
               });
            } else {
               if (projectName != null && !projectName.isBlank()) {
                  details.requestMarkdown = MessageFormat.format(Messages.CreateMarkersTitleTemplate, projectName);
               } else {
                  details.requestMarkdown = Messages.CreateMarkersTitle;
               }

               return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
            }
         } else {
            throw new ToolException("At least one marker is required");
         }
      }
   }

   private IMarker createMarker(IProject project, McpToolCall call, MarkerRequest markerReq) throws CoreException, IllegalArgumentException, BadLocationException {
      if (markerReq.absoluteFilePath != null && !markerReq.absoluteFilePath.isBlank()) {
         if (markerReq.message != null && !markerReq.message.isBlank()) {
            if (markerReq.type != null && !markerReq.type.isBlank()) {
               if (markerReq.startLine != null && markerReq.startLine >= 1) {
                  if (markerReq.markerHighlightedText != null && !markerReq.markerHighlightedText.isBlank()) {
                     if ("ai_marker".equalsIgnoreCase(markerReq.type)) {
                        boolean hasActionPrompt = markerReq.actionPrompt != null && !markerReq.actionPrompt.isBlank();
                        boolean hasActionTitle = markerReq.actionTitle != null && !markerReq.actionTitle.isBlank();
                        boolean hasActionDescription = markerReq.actionDescription != null && !markerReq.actionDescription.isBlank();
                        boolean hasAnyAction = hasActionPrompt || hasActionTitle || hasActionDescription;
                        if (hasAnyAction && (!hasActionPrompt || !hasActionTitle || !hasActionDescription)) {
                           throw new IllegalArgumentException("action_prompt, action_title, and action_description must be provided together for ai_marker");
                        }
                     }

                     Optional<IFile> file = this.projectTools.getProjectFile(project, markerReq.absoluteFilePath);
                     if (!file.isPresent()) {
                        throw new IllegalArgumentException("File not found: " + markerReq.absoluteFilePath);
                     } else {
                        IFile actualFile = (IFile)file.get();
                        int[] positions = this.calculateCharPositions(actualFile, markerReq.startLine, markerReq.markerHighlightedText);
                        markerReq.lineOffset = positions[0];
                        markerReq.charStart = positions[1];
                        markerReq.charEnd = positions[2];
                        MarkerType markerType = MarkerType.fromDisplayName(markerReq.type);
                        if (markerType == null) {
                           throw new IllegalArgumentException("Unknown marker type: " + markerReq.type);
                        } else {
                           String markerTypeId = markerType.getTypeId();
                           if (markerType == MarkerType.AI_MARKER) {
                              markerTypeId = convertAISeverity(markerReq.severity);
                           }

                           IMarker marker = actualFile.createMarker(markerTypeId);
                           markerReq.id = marker.getId();
                           this.setMarkerAttributes(marker, call, markerReq, markerType);
                           return marker;
                        }
                     }
                  } else {
                     throw new IllegalArgumentException("marker_highlighted_text is required");
                  }
               } else {
                  throw new IllegalArgumentException("marker_line must be a positive integer");
               }
            } else {
               throw new IllegalArgumentException("type is required");
            }
         } else {
            throw new IllegalArgumentException("message is required");
         }
      } else {
         throw new IllegalArgumentException("path is required");
      }
   }

   private int[] calculateCharPositions(IFile file, int startLine, String markedText) throws CoreException, IllegalArgumentException, BadLocationException {
      Optional<IFileDocument> optionalDocument = this.contentSourceProvider.getFileDocument(file);
      if (optionalDocument.isEmpty()) {
         throw new IllegalArgumentException("File content not available: " + file.getFullPath());
      } else {
         IFileDocument document = (IFileDocument)optionalDocument.get();
         StringBuilder content = new StringBuilder();
         long maxLinesCount = markedText.lines().count() + 1L;
         int charStart = -1;

         for(String line : this.fileSystem.getLines(document, startLine - 1, (int)maxLinesCount)) {
            content.append(line);
            charStart = content.indexOf(markedText);
            if (charStart >= 0) {
               break;
            }
         }

         if (charStart == -1) {
            throw new IllegalArgumentException("Target content not found in line " + startLine);
         } else {
            int charEnd = charStart + markedText.length();
            return new int[]{document.getDocument().getLineOffset(startLine - 1), charStart, charEnd};
         }
      }
   }

   private void setMarkerAttributes(IMarker marker, McpToolCall call, MarkerRequest markerReq, MarkerType markerType) throws CoreException {
      marker.setAttribute("message", markerReq.message);
      marker.setAttribute("transient", true);
      StringBuilder location = new StringBuilder();
      if (markerReq.startLine != null && markerReq.startLine > 0) {
         marker.setAttribute("lineNumber", markerReq.startLine);
         location.append("Line ");
         location.append(markerReq.startLine);
      }

      if (markerReq.charStart != null && markerReq.charEnd != null) {
         marker.setAttribute("charStart", markerReq.lineOffset + markerReq.charStart);
         marker.setAttribute("charEnd", markerReq.lineOffset + markerReq.charEnd);
         location.append(" [");
         location.append(markerReq.charStart + 1);
         location.append(':');
         location.append(markerReq.charEnd + 1);
         location.append(']');
      }

      if (location.length() > 0) {
         marker.setAttribute("location", location.toString());
      }

      if (markerType == MarkerType.AI_MARKER && markerReq.actionPrompt != null && !markerReq.actionPrompt.isBlank()) {
         marker.setAttribute("action_call", call);
         marker.setAttribute("action_details", markerReq);
      }

      switch (markerType) {
         case TASK:
            this.setBooleanAttribute(marker, "done", markerReq.done);
            this.setBooleanAttribute(marker, "userEditable", true);
            if (markerReq.priority != null) {
               marker.setAttribute("priority", this.convertPriority(markerReq.priority));
            }
            break;
         case PROBLEM:
         case AI_MARKER:
            marker.setAttribute("severity", this.convertSeverity(markerReq.severity));
            if (markerReq.priority != null) {
               marker.setAttribute("priority", this.convertPriority(markerReq.priority));
            }
         case TEXT:
         case M1C:
         default:
            break;
         case BOOKMARK:
            this.setBooleanAttribute(marker, "done", markerReq.done);
            this.setBooleanAttribute(marker, "userEditable", true);
      }

   }

   private void setBooleanAttribute(IMarker marker, String attr, Boolean value) throws CoreException {
      if (value != null) {
         marker.setAttribute(attr, value);
      }

   }

   private int convertSeverity(String severity) {
      if (severity == null) {
         return 0;
      } else {
         String normalized = severity.trim().toLowerCase();
         switch (normalized.hashCode()) {
            case 3237038:
               if (!normalized.equals("info")) {
               }
               break;
            case 3641990:
               if (normalized.equals("warn")) {
                  return 1;
               }
               break;
            case 96784904:
               if (normalized.equals("error")) {
                  return 2;
               }
               break;
            case 1124446108:
               if (normalized.equals("warning")) {
                  return 1;
               }
               break;
            case 1968600364:
               if (!normalized.equals("information")) {
               }
         }

         return 0;
      }
   }

   private static String convertAISeverity(String severity) {
      if (severity == null) {
         return "com.e1c.edt.ai.AIInfo";
      } else {
         String normalized = severity.trim().toLowerCase();
         switch (normalized.hashCode()) {
            case 3237038:
               if (!normalized.equals("info")) {
               }
               break;
            case 3641990:
               if (normalized.equals("warn")) {
                  return "com.e1c.edt.ai.AIWarning";
               }
               break;
            case 96784904:
               if (normalized.equals("error")) {
                  return "com.e1c.edt.ai.AIError";
               }
               break;
            case 1124446108:
               if (normalized.equals("warning")) {
                  return "com.e1c.edt.ai.AIWarning";
               }
               break;
            case 1968600364:
               if (!normalized.equals("information")) {
               }
         }

         return "com.e1c.edt.ai.AIInfo";
      }
   }

   private int convertPriority(String priority) {
      if (priority == null) {
         return 1;
      } else {
         switch (priority.toLowerCase()) {
            case "low":
               return 0;
            case "high":
               return 2;
         }

         return 1;
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "SetMarkers";
      StringBuilder description = new StringBuilder();
      description.append("Creates markers in project files (issues, tasks, bookmarks, etc.).");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Do NOT wrap JSON in Markdown or send arrays; no trailing commas or comments.");
      description.append("\n- `markers` must be an array of marker objects (see required fields below).");
      description.append("\n- Use `" + MarkerType.AI_MARKER.getDisplayName() + "` for issues, problems, errors, warnings.");
      description.append("\n- For `ai_marker`, severity maps to marker types: `AIError`, `AIWarning`, `AIInfo`.");
      description.append("\n- Use `" + MarkerType.TASK.getDisplayName() + "` for plans, schedules, proposals, tasks, TODO.");
      description.append("\n- Use `" + MarkerType.BOOKMARK.getDisplayName() + "` for summaries or reports.");
      description.append("\n- To update a marker, delete it with `DeleteMarkers` and create a new one.");
      description.append("\n\nRelated tools:");
      description.append("\n- Inspect existing markers: `GetMarkers`.");
      description.append("\n- Remove markers: `DeleteMarkers`.");
      description.append("\n\nSupported marker types:");

      MarkerType[] var5;
      for(MarkerType type : var5 = MarkerType.values()) {
         description.append("\n- ").append(type.getDisplayName()).append(": ").append(type.getDescription());
      }

      description.append("\n\nCommon properties for all markers:");
      description.append("\n- type: Marker type (required)");
      description.append("\n- path: File path relative to project root (required)");
      description.append("\n- message: Marker description (required)");
      description.append("\n- marker_line: Line number (required). An integer value indicating the line number for a marker. It is 1-relative. Take the line number from the line prefix using the `Read` tool. Alias: start_line");
      description.append("\n- marker_highlighted_text: Code fragment associated with the marker (required). ALWAYS minimize to the smallest possible size that maintains context. ALWAYS exclude extra suffix and prefix.");
      description.append("\n- action_prompt: AI prompt to execute when marker is activated (optional for ai_marker type)");
      description.append("\n- action_title: Short title for the quick fix action (optional for ai_marker type)");
      description.append("\n- action_description: Detailed description of the quick fix action (optional for ai_marker type)");
      description.append("\n\nType-specific properties:");
      description.append("\n- bookmark: done");
      description.append("\n- task: done, priority");
      description.append("\n- problem: severity, priority");
      description.append("\n- ai_marker: severity, priority");
      description.append("\n\nQuick fix actions:");
      description.append("\n- For `ai_marker` with action fields: adds quick fix action prompt");
      description.append("\n- IMPORTANT: if you provide any action_* fields, you must provide all three action fields");
      description.append("\n- If type is not `ai_marker`, omit action_* fields.");
      description.append("\n\nExample request:\n").append(QuestionExample);
      description.append("\nExample response:\n").append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty projectNameProp = new McpToolCallProperty();
      projectNameProp.type = "string";
      projectNameProp.description = "Target project name. Optional - can be determined from absolute file paths.";
      properties.put("project_name", projectNameProp);
      McpToolCallProperty markersProp = new McpToolCallProperty();
      markersProp.type = "array";
      markersProp.description = "List of marker objects. Each marker must include: type, path, marker_line, marker_highlighted_text, message. If you provide any action_* fields for ai_marker, all three are required.";
      properties.put("markers", markersProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("markers");
      spec.function.parameters = parameters;
      return spec;
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$tools$MarkerType() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$tools$MarkerType;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[MarkerType.values().length];

         try {
            var0[MarkerType.AI_MARKER.ordinal()] = 8;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[MarkerType.BOOKMARK.ordinal()] = 6;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[MarkerType.M1C.ordinal()] = 7;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[MarkerType.MARKER.ordinal()] = 2;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[MarkerType.PROBLEM.ordinal()] = 4;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[MarkerType.TASK.ordinal()] = 3;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[MarkerType.TEXT.ordinal()] = 5;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[MarkerType.UNKNOWN.ordinal()] = 1;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$tools$MarkerType = var0;
         return var0;
      }
   }

   private static class Request {
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("markers")
      public List<MarkerRequest> markers;
   }

   public class MarkerRequest {
      @SerializedName("type")
      public String type;
      @SerializedName("project_name")
      public String projectName;
      @SerializedName("path")
      public String absoluteFilePath;
      @SerializedName("message")
      public String message;
      @SerializedName("marker_line")
      public Integer startLine;
      @SerializedName("marker_highlighted_text")
      public String markerHighlightedText;
      @SerializedName("severity")
      public String severity;
      @SerializedName("priority")
      public String priority;
      @SerializedName("done")
      public Boolean done;
      @SerializedName("action_prompt")
      public String actionPrompt;
      @SerializedName("action_title")
      public String actionTitle;
      @SerializedName("action_description")
      public String actionDescription;
      public transient long id;
      public transient Integer lineOffset;
      public transient Integer charStart;
      public transient Integer charEnd;
   }
}
