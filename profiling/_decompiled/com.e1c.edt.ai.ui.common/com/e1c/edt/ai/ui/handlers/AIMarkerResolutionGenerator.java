package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.tools.SetMarkersMcpTool;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

public class AIMarkerResolutionGenerator implements IMarkerResolutionGenerator {
   public IMarkerResolution[] getResolutions(IMarker marker) {
      try {
         Object call = marker.getAttribute("action_call");
         Object details = marker.getAttribute("action_details");
         if (call != null && call instanceof McpToolCall && details != null && details instanceof SetMarkersMcpTool.MarkerRequest) {
            IMarkerResolution[] resolutions = new IMarkerResolution[1];
            resolutions[0] = new AIMarkerResolution((McpToolCall)call, (SetMarkersMcpTool.MarkerRequest)details);
            return resolutions;
         }
      } catch (CoreException var5) {
      }

      return new IMarkerResolution[0];
   }
}
