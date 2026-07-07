package com.e1c.edt.ai.context.tools;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.bm.integration.IBmTask;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupport;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.validation.marker.BmObjectMarker;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.MarkerFilter;
import com._1c.g5.v8.dt.validation.marker.MarkerSeverity;
import com._1c.g5.v8.dt.validation.marker.v2.IMarkerManagerV2;
import com._1c.g5.v8.dt.validation.marker.v2.IMarkerReader;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.assistent.model.MarkerInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class MarkersProvider implements IMarkersProvider {
   private final IMarkerManagerV2 markerManager;
   private final IContentSourceProvider contentSourceProvider;
   private final IBmModelManager modelManager;
   private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$_1c$g5$v8$dt$validation$marker$MarkerSeverity;

   @Inject
   public MarkersProvider(IMarkerManagerV2 markerManager, IContentSourceProvider contentSourceProvider, IBmModelManager modelManager, IProjectFileSystemSupportProvider projectFileSystemSupportProvider) {
      Preconditions.checkNotNull(markerManager);
      Preconditions.checkNotNull(contentSourceProvider);
      Preconditions.checkNotNull(modelManager);
      Preconditions.checkNotNull(projectFileSystemSupportProvider);
      this.markerManager = markerManager;
      this.contentSourceProvider = contentSourceProvider;
      this.modelManager = modelManager;
      this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
   }

   public Stream<MarkerInfo> getMarkers(IProject project, IFile file) {
      IMarkerReader reader = this.markerManager.createReader(project);
      return reader.markers(new MarkerFilter[0]).map((marker) -> this.createMarkerInfo(project, file, marker)).filter((marker) -> marker.isPresent()).map((marker) -> (MarkerInfo)marker.get());
   }

   private Optional<MarkerInfo> createMarkerInfo(IProject project, IFile targetFile, Marker marker) {
      MarkerInfo info = new MarkerInfo();
      info.type = "1c";
      info.message = marker.getMessage();
      info.sourceId = marker.getSourceType();
      HashMap<String, Object> details = new HashMap();
      info.details = details;
      Integer offset = null;
      Integer length = null;
      Map<String, String> extraInfos = null;

      try {
         Method getExtraInfoMethod = marker.getClass().getMethod("getExtraInfo");
         Object result = getExtraInfoMethod.invoke(marker);
         if (result instanceof Map) {
            extraInfos = (Map)result;
         }
      } catch (Exception var15) {
         extraInfos = null;
      }

      if (extraInfos != null) {
         Iterator file = extraInfos.entrySet().iterator();

         label129:
         while(true) {
            String key;
            String value;
            while(true) {
               while(true) {
                  while(true) {
                     if (!file.hasNext()) {
                        break label129;
                     }

                     Map.Entry<String, String> extraInfo = (Map.Entry)file.next();
                     key = (String)extraInfo.getKey();
                     if (key != null && !key.isBlank()) {
                        value = (String)extraInfo.getValue();
                        if (value != null && !value.isBlank()) {
                           if (!"line".equalsIgnoreCase(key)) {
                              break;
                           }

                           try {
                              info.startLine = Integer.parseInt(value);
                           } catch (NumberFormatException var16) {
                              break;
                           }
                        }
                     }
                  }

                  if (!"offset".equalsIgnoreCase(key)) {
                     break;
                  }

                  try {
                     offset = Integer.parseInt(value);
                  } catch (NumberFormatException var17) {
                     break;
                  }
               }

               if (!"length".equalsIgnoreCase(key)) {
                  break;
               }

               try {
                  length = Integer.parseInt(value);
               } catch (NumberFormatException var18) {
                  break;
               }
            }

            details.put(key, value);
         }
      }

      details.put("1c_check_id", marker.getCheckId());
      details.put("1c_top_object_id", marker.getTopObjectId());
      switch (marker.getSeverity()) {
         case ERRORS:
            info.severity = "error";
            info.priority = "normal";
            break;
         case BLOCKER:
            info.severity = "error";
            info.priority = "high";
            break;
         case CRITICAL:
            info.severity = "error";
            info.priority = "high";
            break;
         case MAJOR:
            info.severity = "error";
            info.priority = "low";
            break;
         case MINOR:
            info.severity = "warning";
            info.priority = "normal";
            break;
         case TRIVIAL:
            info.severity = "info";
            info.priority = "high";
            break;
         case NONE:
            info.severity = "info";
            info.priority = "normal";
            break;
         default:
            info.severity = "info";
            info.priority = "low";
      }

      if (marker instanceof BmObjectMarker) {
         BmObjectMarker objectMarker = (BmObjectMarker)marker;
         details.put("1c_object_id", objectMarker.getObjectId());
      }

      Object topObjectId = marker.getTopObjectId();
      IFile file = null;
      if (topObjectId instanceof String) {
         file = this.getFileByPath(project, (String)topObjectId);
      } else if (topObjectId instanceof Long) {
         file = this.getFileByObjectId(project, (Long)topObjectId);
      }

      if (file != null) {
         if (targetFile != null && !targetFile.equals(file)) {
            return Optional.empty();
         }

         IPath location = file.getLocation();
         if (location != null) {
            info.path = location.toOSString();
         }

         if (info.startLine != null) {
            info.location = "line: " + info.startLine;
            if (info.path != null) {
               info.location = info.location + " " + info.path;
            }

            if (offset != null && length != null && file.exists()) {
               try {
                  info.markerHighlightedText = this.readContentFromFile(file, offset, length);
               } catch (Exception var14) {
               }
            }
         }
      }

      return Optional.ofNullable(info);
   }

   private String readContentFromFile(IFile file, int offset, int length) throws BadLocationException {
      Optional<IFileDocument> optionalDocument = this.contentSourceProvider.getFileDocument(file);
      if (optionalDocument.isEmpty()) {
         return null;
      } else {
         IFileDocument document = (IFileDocument)optionalDocument.get();
         IDocument doc = document.getDocument();
         return doc.get(offset, length);
      }
   }

   private IFile getFileByPath(IProject project, String path) {
      if (path.startsWith("/" + project.getName() + "/")) {
         path = path.substring(project.getName().length() + 2);
      }

      return project.getFile(path);
   }

   private IFile getFileByObjectId(IProject project, final Long objectId) {
      IBmModel model = this.modelManager.getModel(project);
      if (model == null) {
         return null;
      } else {
         try {
            IBmObject bmObject = (IBmObject)model.executeReadonlyTask(new IBmTask<IBmObject>() {
               public IBmObject execute(IBmTransaction transaction, IProgressMonitor progressMonitor) {
                  return transaction.getObjectById(objectId);
               }

               public Object getId() {
                  return "MarkersProvider/" + objectId;
               }

               public String getName() {
                  return "Get object by id: " + objectId;
               }

               public Object getServiceId() {
                  return "MarkersProvider";
               }
            });
            if (bmObject != null) {
               IProjectFileSystemSupport fileSystem = this.projectFileSystemSupportProvider.getProjectFileSystemSupport(project);
               if (fileSystem != null) {
                  IFile file = fileSystem.getFile(bmObject);
                  if (file != null) {
                     return file;
                  }
               }
            }
         } catch (Exception var7) {
         }

         return null;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$_1c$g5$v8$dt$validation$marker$MarkerSeverity() {
      int[] var10000 = $SWITCH_TABLE$com$_1c$g5$v8$dt$validation$marker$MarkerSeverity;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[MarkerSeverity.values().length];

         try {
            var0[MarkerSeverity.BLOCKER.ordinal()] = 2;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[MarkerSeverity.CRITICAL.ordinal()] = 3;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[MarkerSeverity.ERRORS.ordinal()] = 1;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[MarkerSeverity.MAJOR.ordinal()] = 4;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[MarkerSeverity.MINOR.ordinal()] = 5;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[MarkerSeverity.NONE.ordinal()] = 7;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[MarkerSeverity.TRIVIAL.ordinal()] = 6;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$_1c$g5$v8$dt$validation$marker$MarkerSeverity = var0;
         return var0;
      }
   }
}
