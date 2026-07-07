package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.assistent.model.MarkerInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;

public class CommonMarkersProvider implements IMarkersProvider {
   private final IContentSourceProvider contentSourceProvider;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$tools$MarkerType;

   @Inject
   public CommonMarkersProvider(IContentSourceProvider contentSourceProvider) {
      Preconditions.checkNotNull(contentSourceProvider);
      this.contentSourceProvider = contentSourceProvider;
   }

   public Stream<MarkerInfo> getMarkers(IProject project, IFile file) {
      ArrayList<MarkerInfo> allMarkers = new ArrayList();

      try {
         IMarker[] markers = file == null ? project.findMarkers((String)null, true, 2) : file.findMarkers((String)null, true, 2);

         for(IMarker marker : markers) {
            MarkerType markerType = MarkerType.fromTypeId(marker.getType());
            if (markerType != null) {
               IResource resource = marker.getResource();
               IPath location = resource.getLocation();
               MarkerInfo markerInfo = new MarkerInfo();
               markerInfo.path = location != null ? location.toFile().getAbsolutePath() : "";
               markerInfo.startLine = marker.getAttribute("lineNumber", -1);
               markerInfo.message = marker.getAttribute("message", "");
               markerInfo.type = markerType.getDisplayName();
               this.setMarkerAttributes(project, marker, markerInfo, markerType);
               allMarkers.add(markerInfo);
            }
         }
      } catch (CoreException var13) {
      }

      return allMarkers.stream();
   }

   private void setMarkerAttributes(IProject project, IMarker marker, MarkerInfo markerInfo, MarkerType markerType) throws CoreException {
      markerInfo.location = marker.getAttribute("location", (String)null);
      Integer charStart = null;
      Integer charEnd = null;
      Object charStartObj = marker.getAttribute("charStart");
      if (charStartObj instanceof Integer) {
         charStart = (Integer)charStartObj;
      }

      Object charEndObj = marker.getAttribute("charEnd");
      if (charEndObj instanceof Integer) {
         charEnd = (Integer)charEndObj;
      }

      if (charStart != null && charEnd != null && charEnd > charStart) {
         try {
            IFile file = (IFile)marker.getResource();
            if (file.exists()) {
               markerInfo.markerHighlightedText = this.readContentFromFile(file, charStart, charEnd - charStart);
            }
         } catch (Exception var16) {
         }
      }

      switch (markerType) {
         case TASK:
            Object doneTask = marker.getAttribute("done");
            if (doneTask instanceof Boolean) {
               markerInfo.done = (Boolean)doneTask;
            }

            Object priorityObj = marker.getAttribute("priority");
            if (priorityObj instanceof Integer) {
               int priority = (Integer)priorityObj;
               markerInfo.priority = this.convertPriorityToString(priority);
            }
            break;
         case PROBLEM:
         case AI_MARKER:
            Object severityObj = marker.getAttribute("severity");
            if (severityObj instanceof Integer) {
               int severity = (Integer)severityObj;
               markerInfo.severity = this.convertSeverityToString(severity);
            }

            Object priorityProblem = marker.getAttribute("priority");
            if (priorityProblem instanceof Integer) {
               int priority = (Integer)priorityProblem;
               markerInfo.priority = this.convertPriorityToString(priority);
            }
         case TEXT:
         case M1C:
         default:
            break;
         case BOOKMARK:
            Object doneBookmark = marker.getAttribute("done");
            if (doneBookmark instanceof Boolean) {
               markerInfo.done = (Boolean)doneBookmark;
            }

            Object sourceId = marker.getAttribute("sourceId");
            if (sourceId instanceof String) {
               markerInfo.sourceId = (String)sourceId;
            }
      }

   }

   private String readContentFromFile(IFile file, int charStart, int length) throws BadLocationException {
      Optional<IFileDocument> optionalDocument = this.contentSourceProvider.getFileDocument(file);
      if (optionalDocument.isEmpty()) {
         return null;
      } else {
         IFileDocument document = (IFileDocument)optionalDocument.get();
         return document.getDocument().get(charStart, length);
      }
   }

   private String convertSeverityToString(int severity) {
      switch (severity) {
         case 1:
            return "warning";
         case 2:
            return "error";
         default:
            return "info";
      }
   }

   private String convertPriorityToString(int priority) {
      switch (priority) {
         case 0:
            return "low";
         case 1:
         default:
            return "normal";
         case 2:
            return "high";
      }
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
}
