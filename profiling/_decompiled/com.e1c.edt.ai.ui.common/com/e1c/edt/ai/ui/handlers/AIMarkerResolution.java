package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

public class AIMarkerResolution implements IMarkerResolution2 {
   private final McpToolCall call;
   private final SetMarkersMcpTool.MarkerRequest markerRequest;
   @Inject
   IChat chat;
   @Inject
   IJson json;

   public AIMarkerResolution(McpToolCall call, SetMarkersMcpTool.MarkerRequest markerRequest) {
      Preconditions.checkNotNull(call);
      Preconditions.checkNotNull(markerRequest);
      this.call = call;
      this.markerRequest = markerRequest;
      BaseActivator.injectMembers(this);
   }

   public String getLabel() {
      return this.markerRequest.actionTitle != null ? this.markerRequest.actionTitle : "Apply AI Suggestion";
   }

   public void run(IMarker marker) {
      StringBuilder prompt = new StringBuilder();
      prompt.append(this.markerRequest.actionPrompt);
      prompt.append("\n\nDetails:\n```\\n");
      prompt.append(this.json.serialize(this.markerRequest));
      prompt.append("\n```\nDo ONLY what is asked.");
      prompt.append("\nDelete markers with a specific ID if they have been fixed, for example `" + this.markerRequest.id + "`.");
      this.chat.continueChat(this.call.sourceChatId, prompt.toString());
   }

   public String getDescription() {
      return this.markerRequest.actionDescription != null ? this.markerRequest.actionDescription : "Execute AI-assisted code transformation";
   }

   public Image getImage() {
      return BaseActivator.getImage("AI");
   }
}
