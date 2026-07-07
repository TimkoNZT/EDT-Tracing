package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIModule;
import com.e1c.edt.ai.CodeCompletionActionHandler;
import com.e1c.edt.ai.CodeCompletionSession;
import com.e1c.edt.ai.ICodeCompletionActionHandler;
import com.e1c.edt.ai.ICodeCompletionSession;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.tools.MCPToolsModule;
import com.e1c.edt.ai.ui.handlers.CodeTools;
import com.e1c.edt.ai.ui.handlers.FixDialog;
import com.e1c.edt.ai.ui.handlers.ICodeTools;
import com.e1c.edt.ai.ui.handlers.IFixDialog;
import com.e1c.edt.ai.ui.preferences.DiagnosticReportDialogProvider;
import com.e1c.edt.ai.ui.preferences.IDiagnosticReportDialogProvider;
import com.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class AIUICommonModule extends AbstractModule {
   public static final String PARAMETERS = "Parameters";
   public static final String URL = "URL";

   protected void configure() {
      this.install(new AIModule());
      this.install(new MCPToolsModule());
      Multibinder<IInitializable> initializableBinder = Multibinder.newSetBinder(this.binder(), IInitializable.class);
      initializableBinder.addBinding().to(UI.class);
      initializableBinder.addBinding().to(ContextMenuInterceptor.class);
      initializableBinder.addBinding().to(ClipboardManager.class);
      initializableBinder.addBinding().to(DialogsEnhancer.class);
      initializableBinder.addBinding().to(ResourceListener.class);
      initializableBinder.addBinding().to(UpdateService.class);
      initializableBinder.addBinding().to(Notificator.class);
      initializableBinder.addBinding().to(ActiveProjectTracker.class);
      this.bind(UI.class).in(Singleton.class);
      this.bind(IUI.class).to(UI.class);
      this.bind(ContextMenuInterceptor.class).in(Singleton.class);
      this.bind(ClipboardManager.class).in(Singleton.class);
      this.bind(IClipboard.class).to(ClipboardManager.class);
      this.bind(DialogsEnhancer.class).in(Singleton.class);
      this.bind(ResourceListener.class).in(Singleton.class);
      this.bind(UpdateService.class).in(Singleton.class);
      this.bind(Notificator.class).in(Singleton.class);
      this.bind(ActiveProjectTracker.class).in(Singleton.class);
      Multibinder<IViewEnhancer> viewEnhancerBinder = Multibinder.newSetBinder(this.binder(), IViewEnhancer.class);
      viewEnhancerBinder.addBinding().to(StagingViewEnhancer.class);
      this.bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
      this.bind(ISettingsStore.class).to(PreferenceStoreToSettingsStoreAdapter.class).in(Singleton.class);
      this.bind(IdeApiHandler.class);
      this.bind(Chat.class).in(Singleton.class);
      this.bind(IChat.class).to(Chat.class);
      this.bind(IChatDialog.class).to(Chat.class);
      this.bind(IAIContextProvider.class).to(AIContextProvider.class).in(Singleton.class);
      this.bind(Settings.class).in(Singleton.class);
      this.bind(ISettings.class).to(Settings.class);
      this.bind(ISettingsSetter.class).to(Settings.class);
      this.bind(new TypeLiteral<ICodeCompletionViewModel<CodeCompletionContext>>() {
      }).to(CodeCompletionViewModel.class).in(Singleton.class);
      this.bind(IHintPainter.class).to(HintPainter.class).in(Singleton.class);
      this.bind(IVerticalRulerPainter.class).to(VerticalRulerPainter.class).in(Singleton.class);
      this.bind(IHotKeys.class).to(HotKeys.class).in(Singleton.class);
      this.bind(IUserActions.class).to(UserActions.class).in(Singleton.class);
      this.bind(new TypeLiteral<ICodeCompletionSession<CodeCompletionContext>>() {
      }).to(new TypeLiteral<CodeCompletionSession<CodeCompletionContext>>() {
      });
      this.bind(new TypeLiteral<ICodeCompletionActionHandler<CodeCompletionContext>>() {
      }).to(new TypeLiteral<CodeCompletionActionHandler<CodeCompletionContext>>() {
      });
      this.bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
      this.bind(IFeedbackDialog.class).to(FeedbackDialog.class);
      this.bind(IIssueFeedbackViewModel.class).to(IssueFeedbackViewModel.class);
      this.bind(IFixDialog.class).to(FixDialog.class).in(Singleton.class);
      this.bind(IContentProvider.class).to(ContentProvider.class).in(Singleton.class);
      this.bind(IJavaScript.class).to(JavaScript.class).in(Singleton.class);
      this.bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
      this.bind(IGlobalContextManager.class).to(GlobalContextManager.class).in(Singleton.class);
      this.bind(ISyntaxVaidator.class).to(SyntaxVaidator.class).in(Singleton.class);
      this.bind(IGlobalContextTracker.class).to(GlobalContextTracker.class).in(Singleton.class);
      this.bind(IProjectTrackingWorkflow.class).to(ProjectTrackingWorkflow.class);
      this.bind(IGlobalContextSync.class).to(GlobalContextSync.class).in(Singleton.class);
      this.bind(IProposalsProvider.class).to(ProposalsProvider.class).in(Singleton.class);
      this.bind(TextWidgetInfo.class).in(Singleton.class);
      this.bind(ITextWidgetInfoUpdater.class).to(TextWidgetInfo.class);
      this.bind(ITextWidgetInfoProvider.class).to(TextWidgetInfo.class);
      this.bind(ICodeTools.class).to(CodeTools.class).in(Singleton.class);
      this.bind(IVerticalRulerManager.class).to(VerticalRulerManager.class).in(Singleton.class);
      this.bind(IGCTools.class).to(GCTools.class).in(Singleton.class);
      this.bind(IUINotificationService.class).to(UINotificationService.class);
      this.bind(IFileScaner.class).to(FileScaner.class).in(Singleton.class);
      this.bind(IPluginUpdateService.class).to(PluginUpdateService.class);
      this.bind(IReflection.class).to(Reflection.class).in(Singleton.class);
      this.bind(IReflection.class).to(Reflection.class).in(Singleton.class);
      this.bind(IWidgets.class).to(Widgets.class).in(Singleton.class);
      this.bind(IGitTools.class).to(GitTools.class).in(Singleton.class);
      this.bind(IGitActions.class).to(GitActions.class).in(Singleton.class);
      this.bind(IResourceProvider.class).to(ResourceProvider.class).in(Singleton.class);
      this.bind(IFileSystem.class).to(FileSystem.class).in(Singleton.class);
      this.bind(IProjectTrackingDeltaVisitor.class).to(ProjectTrackingDeltaVisitor.class).in(Singleton.class);
      this.bind(ITextActions.class).to(TextActions.class).in(Singleton.class);
      this.bind(IStateService.class).to(StateService.class).in(Singleton.class);
      this.bind(IContentSourceProvider.class).to(ContentSourceProvider.class).in(Singleton.class);
      this.bind(IEdtLinkHandler.class).to(EdtLinkHandler.class).in(Singleton.class);
      this.bind(IEditorPositionManager.class).to(EditorPositionManager.class).in(Singleton.class);
      this.bind(IDiagnosticReportDialogProvider.class).to(DiagnosticReportDialogProvider.class).in(Singleton.class);
      this.bind(IThemeManager.class).to(ThemeManager.class).in(Singleton.class);
      this.bind(INotifications.class).to(Notifications.class).in(Singleton.class);
      this.bind(IWeb.class).to(Web.class).in(Singleton.class);
      this.bind(IPreferences.class).to(Preferences.class).in(Singleton.class);
   }
}
