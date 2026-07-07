package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.CompatibilityMode;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.ScriptVariant;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public class ConfigurationParametersProvider implements IConfigurationParametersProvider, IProjectDetailsProvider {
   private static final String DT_INF_PROJECT_PMF = "DT-INF/PROJECT.PMF";
   private static final String RUNTIME_VERSION = "Runtime-Version:";
   private final IV8ProjectManager v8ProjectManager;

   @Inject
   public ConfigurationParametersProvider(IV8ProjectManager v8ProjectManager) {
      Preconditions.checkNotNull(v8ProjectManager);
      this.v8ProjectManager = v8ProjectManager;
   }

   public Optional<ConfigurationParameters> getParameters(ProjectId projectId) {
      return this.getParameters(projectId.project);
   }

   public void fill(IProject project, Map<String, Object> details) {
      this.getParameters(project).ifPresent((params) -> details.put("1C project details", params));
   }

   private Optional<ConfigurationParameters> getParameters(IProject project) {
      if (project == null) {
         return Optional.empty();
      } else {
         IV8Project v8Project = this.v8ProjectManager.getProject(project);
         if (v8Project != null) {
            ConfigurationParameters parameters = new ConfigurationParameters();
            this.getRuntimeVersion(project).ifPresent((runtimeVersion) -> parameters.platformVersion = runtimeVersion);
            ScriptVariant scriptVariant = v8Project.getScriptVariant();
            if (scriptVariant != null) {
               parameters.scriptLanguage = scriptVariant.getName();
            }

            if (v8Project instanceof IConfigurationProject) {
               IConfigurationProject configurationProject = (IConfigurationProject)v8Project;
               parameters.type = "Configuration";
               this.fillProjectData(parameters, configurationProject.getDtProject());
               this.fillConfigData(parameters, configurationProject.getConfiguration());
            } else if (v8Project instanceof IExtensionProject) {
               IExtensionProject extensionProject = (IExtensionProject)v8Project;
               parameters.type = "Extension";
               IProject parentProject = extensionProject.getParentProject();
               if (parentProject != null) {
                  parameters.parentProject = parentProject.getName();
               }

               this.fillProjectData(parameters, extensionProject.getDtProject());
               this.fillConfigData(parameters, extensionProject.getConfiguration());
            }

            return Optional.of(parameters);
         } else {
            return Optional.empty();
         }
      }
   }

   private void fillProjectData(ConfigurationParameters parameters, IDtProject dtProject) {
      if (dtProject != null) {
         parameters.name = dtProject.getName();
      }
   }

   private void fillConfigData(ConfigurationParameters parameters, Configuration config) {
      if (config != null) {
         parameters.vendor = config.getVendor();
         parameters.version = config.getVersion();
         parameters.comment = config.getComment();
         parameters.briefInformation = config.getBriefInformation().map();
         CompatibilityMode compatibilityMode = config.getCompatibilityMode();
         if (compatibilityMode != null) {
            parameters.compatibility = compatibilityMode.getLiteral();
         }

      }
   }

   public Optional<String> getRuntimeVersion(IProject project) {
      IFile pmfFile = project.getFile("DT-INF/PROJECT.PMF");
      if (!pmfFile.exists()) {
         return Optional.empty();
      } else {
         try {
            Throwable var3 = null;
            Object var4 = null;

            try {
               InputStream inputStream = pmfFile.getContents();

               label359: {
                  Optional var10000;
                  try {
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                     try {
                        while(true) {
                           String line;
                           if ((line = reader.readLine()) == null) {
                              break label359;
                           }

                           if (line.startsWith("Runtime-Version:")) {
                              var10000 = Optional.of(line.substring("Runtime-Version:".length()).trim());
                              break;
                           }
                        }
                     } finally {
                        if (reader != null) {
                           reader.close();
                        }

                     }
                  } catch (Throwable var21) {
                     if (var3 == null) {
                        var3 = var21;
                     } else if (var3 != var21) {
                        var3.addSuppressed(var21);
                     }

                     if (inputStream != null) {
                        inputStream.close();
                     }

                     throw var3;
                  }

                  if (inputStream != null) {
                     inputStream.close();
                  }

                  return var10000;
               }

               if (inputStream != null) {
                  inputStream.close();
               }
            } catch (Throwable var22) {
               if (var3 == null) {
                  var3 = var22;
               } else if (var3 != var22) {
                  var3.addSuppressed(var22);
               }

               throw var3;
            }
         } catch (Exception var23) {
         }

         return Optional.empty();
      }
   }
}
