package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IClientTokenValidator;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.IIdProvider;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IParser;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.ParametersParser;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

public class Settings implements ISettings, ISettingsSetter {
   private final ILog log;
   private final ISettingsStore settingsStore;
   private final IParser<String, Parameters> parametersParser;
   private final IIdProvider idProvider;
   private final IDefaultSettings defaultSettings;
   private final Parameters defaultParameters;
   private final IClientTokenValidator clientTokenValidator;
   private final Cache<Object, Optional<Parameters>> parametersCache = CacheBuilder.newBuilder().maximumSize(128L).build();
   private Optional<Parameters> defaultSessionParameters = Optional.empty();

   @Inject
   public Settings(ILog log, ISettingsStore settingsStore, IParser<String, Parameters> parametersParser, IIdProvider idProvider, IDefaultSettings defaultSettings, IClientTokenValidator clientTokenValidator) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settingsStore);
      Preconditions.checkNotNull(parametersParser);
      Preconditions.checkNotNull(idProvider);
      Preconditions.checkNotNull(defaultSettings);
      Preconditions.checkNotNull(clientTokenValidator);
      this.log = log;
      this.settingsStore = settingsStore;
      this.parametersParser = parametersParser;
      this.idProvider = idProvider;
      this.defaultSettings = defaultSettings;
      this.defaultParameters = new Parameters(defaultSettings);
      this.clientTokenValidator = clientTokenValidator;
   }

   public boolean isEnabled() {
      return this.hasClientToken() && CodeCompletionPolicy.MANUAL.isMeet(this.getCodeCompletionPolicy());
   }

   public boolean hasClientToken() {
      return this.clientTokenValidator.isValid(this.getClientToken());
   }

   public String getClientToken() {
      String clientToken = (String)this.settingsStore.getString("stringPreferenceClientID").orElse((Object)null);
      if (clientToken != null) {
         clientToken = clientToken.trim();
      } else {
         clientToken = "";
      }

      return clientToken;
   }

   public String getClientUniqueId() {
      return this.idProvider.getId();
   }

   public Optional<String> getInstanceType() {
      String instanceType = (String)this.getParameterValue((ProjectId)null, (parameters) -> parameters.instanceType, () -> null);
      return instanceType != null && !instanceType.isBlank() ? Optional.of(instanceType) : Optional.empty();
   }

   public CodeCompletionPolicy getCodeCompletionPolicy() {
      String id = (String)this.settingsStore.getString("stringPreferenceCodeCompletionPolicy").orElse((Object)null);
      return CodeCompletionPolicy.parse(id);
   }

   public int getTabWidth() {
      return EditorsUI.getPreferenceStore().getInt("tabWidth");
   }

   public int getCodeCompletionLinesCount() {
      return (Integer)this.settingsStore.getInt("stringPreferenceCodeCompletionLinesCount").orElse(5);
   }

   public Duration getMinRequestDelay() {
      return Duration.ofMillis((long)(Integer)this.getParameterValue((ProjectId)null, (parameters) -> parameters.minDelay, () -> 300));
   }

   public Duration getTimeout() {
      return Duration.ofMillis((long)(Integer)this.getParameterValue((ProjectId)null, (parameters) -> parameters.timeout, () -> 15000));
   }

   public String getLineSeparator() {
      return System.lineSeparator();
   }

   public int getPrefixLength(ProjectId projectId) {
      return (Integer)this.getParameterValue(projectId, (parameters) -> parameters.prefixLength, () -> 1000);
   }

   public int getSuffixLength(ProjectId projectId) {
      return (Integer)this.getParameterValue(projectId, (parameters) -> parameters.suffixLength, () -> 500);
   }

   public boolean isExperimental() {
      return (Boolean)this.getParameterValue((ProjectId)null, (parameters) -> parameters.experimental, () -> false);
   }

   public boolean sendGlobalContext(ProjectId projectId) {
      return (Boolean)this.getParameterValue(projectId, (parameters) -> parameters.globalContext, () -> false);
   }

   public String getLanguage() {
      return (String)this.settingsStore.getString("stringPreferenceLanguage").map((i) -> i.isBlank() ? null : i).orElse(Platform.getNL().startsWith("ru_") ? "Russian" : "English");
   }

   public String getTheme() {
      IThemeEngine engine = (IThemeEngine)PlatformUI.getWorkbench().getService(IThemeEngine.class);
      if (engine != null) {
         ITheme activeTheme = engine.getActiveTheme();
         if (activeTheme != null && activeTheme.getId().toLowerCase().contains("dark")) {
            return "Dark";
         }
      }

      return "Default";
   }

   public Verbosity getVerbosity() {
      return (Verbosity)this.getParameterValue((ProjectId)null, (parameters) -> Optional.ofNullable(parameters.verbosity), () -> ParametersParser.DEFAULT_VERBOSITY);
   }

   public Optional<String> getResources() {
      String resource = (String)this.getParameterValue((ProjectId)null, (parameters) -> parameters.resources, () -> null);
      return resource != null && !resource.isBlank() ? Optional.of(resource) : Optional.empty();
   }

   public int getGitDiffContextLines(ProjectId projectId) {
      return (Integer)this.getParameterValue(projectId, (parameters) -> parameters.gitDiffContextLines, () -> 8);
   }

   public URL getUrl() {
      return this.getUserParameters().url;
   }

   public URL getChatUrl() {
      return (URL)this.getParameterValue((ProjectId)null, (parameters) -> parameters.chatUrl, () -> {
         try {
            return new URL(this.getUrl(), "chat/");
         } catch (MalformedURLException var2) {
            return null;
         }
      });
   }

   public String getHomePage() {
      return this.defaultSettings.getHomePage();
   }

   public String getUpdateUrl() {
      return (String)this.getParameterValue((ProjectId)null, (parameters) -> parameters.updateUrl, () -> this.defaultSettings.getUpdateUrl());
   }

   public String getPluginFeature() {
      return this.defaultSettings.getPluginFeature();
   }

   public Parameters getUserParameters() {
      return (Parameters)this.getOptionalUserParameters().orElse(this.defaultParameters);
   }

   public synchronized void applySessionParameters(ProjectId projectId, Parameters sessionParameters) {
      this.parametersCache.put(projectId, Optional.ofNullable(sessionParameters));
      if (this.defaultSessionParameters.isEmpty()) {
         this.defaultSessionParameters = Optional.ofNullable(sessionParameters);
      }

   }

   public void setCodeCompletionPolicy(CodeCompletionPolicy codeCompletionPolicy) {
      this.settingsStore.setString("stringPreferenceCodeCompletionPolicy", codeCompletionPolicy.getId());
   }

   private synchronized Optional<Parameters> getOptionalUserParameters() {
      String parametersStr = (String)this.settingsStore.getString("stringPreferenceLLMParameters").orElse((Object)null);
      if (parametersStr != null && !parametersStr.isBlank()) {
         try {
            return (Optional)this.parametersCache.get(parametersStr, () -> this.parametersParser.parse(parametersStr));
         } catch (ExecutionException error) {
            this.log.logError(error);
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private synchronized Optional<Parameters> getOptionalSessionParameters(ProjectId projectId) {
      if (projectId == null) {
         return this.defaultSessionParameters;
      } else {
         Optional<Parameters> projectParameters = (Optional)this.parametersCache.getIfPresent(projectId);
         return projectParameters != null && projectParameters.isPresent() ? projectParameters : this.defaultSessionParameters;
      }
   }

   private <T> T getParameterValue(ProjectId projectId, Function<Parameters, Optional<T>> valueSelector, Supplier<T> defaultValueProvider) {
      return (T)this.getOptionalUserParameters().flatMap((i) -> Optional.ofNullable((Optional)valueSelector.apply(i))).flatMap((i) -> i).orElseGet(() -> this.getOptionalSessionParameters(projectId).flatMap((i) -> Optional.ofNullable((Optional)valueSelector.apply(i))).flatMap((i) -> i).orElseGet(() -> defaultValueProvider.get()));
   }
}
