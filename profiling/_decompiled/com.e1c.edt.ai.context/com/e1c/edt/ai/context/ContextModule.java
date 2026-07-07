package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.model.IModelEditingSupport;
import com._1c.g5.v8.dt.core.model.IModelObjectFactory;
import com._1c.g5.v8.dt.core.naming.ITopObjectFqnGenerator;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.core.platform.management.IDtHostResourceManager;
import com._1c.g5.v8.dt.form.service.datasourceinfo.IDataSourceInfoAssociationService;
import com._1c.g5.v8.dt.md.IExternalPropertyManagerRegistry;
import com._1c.g5.v8.dt.search.core.text.ITextSearchIndexProvider;
import com._1c.g5.v8.dt.validation.marker.v2.IMarkerManagerV2;
import com._1c.g5.wiring.AbstractServiceAwareModule;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.ICodeProvider;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.MarkdownUtils;
import com.e1c.edt.ai.context.tools.FindMcpTool;
import com.e1c.edt.ai.context.tools.GetObjectMcpTool;
import com.e1c.edt.ai.context.tools.IMethodListProvider;
import com.e1c.edt.ai.context.tools.MarkersProvider;
import com.e1c.edt.ai.context.tools.MetadataBindingProvider;
import com.e1c.edt.ai.context.tools.MethodListProvider;
import com.e1c.edt.ai.tools.IJShellBindingProvider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.eclipse.core.runtime.Plugin;

class ContextModule extends AbstractServiceAwareModule {
   public ContextModule(Plugin plugin) {
      super(plugin);
   }

   protected void doConfigure() {
      this.bind(IEntitiesWalker.class).to(EntitiesWalker.class).in(Singleton.class);
      this.bind(IRelatedEntities.class).to(RelatedEntities.class).in(Singleton.class);
      this.bind(IEntityInfo.class).to(EntityInfo.class).in(Singleton.class);
      this.bind(IContextEntities.class).to(EntityInfo.class).in(Singleton.class);
      this.bind(IV8Model.class).to(V8Model.class).in(Singleton.class);
      this.bind(IIdFactory.class).to(IdFactory.class).in(Singleton.class);
      this.bind(BslMultiLineCommentDocumentationProvider.class).toInstance(new BslMultiLineCommentDocumentationProvider());
      this.bind(ICommentFactory.class).to(CommentFactory.class).in(Singleton.class);
      this.bind(IEntityFactory.class).to(EntityFactory.class).in(Singleton.class);
      this.bind(IFormWalker.class).to(FormWalker.class).in(Singleton.class);
      this.bind(ICodePartsProvider.class).to(CodePartsProvider.class).in(Singleton.class);
      this.bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
      this.bind(ICodeProvider.class).to(CodeProvider.class).in(Singleton.class);
      this.bind(IBmPovider.class).to(BmPovider.class).in(Singleton.class);
      this.bind(IBmObjectProvider.class).to(BmObjectProvider.class).in(Singleton.class);
      this.bind(IFiles.class).to(Files.class).in(Singleton.class);
      this.bind(IConfigurationParametersProvider.class).to(ConfigurationParametersProvider.class).in(Singleton.class);
      this.bind(IEditingSupport.class).to(EditingSupport.class).in(Singleton.class);
      this.bind(IMarkdownUtils.class).to(MarkdownUtils.class).in(Singleton.class);
      this.bind(IMethodListProvider.class).to(MethodListProvider.class).in(Singleton.class);
      Multibinder<IProjectDetailsProvider> projectDetailsProviderBinder = Multibinder.newSetBinder(this.binder(), IProjectDetailsProvider.class);
      projectDetailsProviderBinder.addBinding().to(ConfigurationParametersProvider.class);
      this.bind(MessageDigest.class).toProvider(() -> {
         try {
            return MessageDigest.getInstance("MD5");
         } catch (NoSuchAlgorithmException var1) {
            return null;
         }
      });
      this.bind(IModuleProvider.class).annotatedWith(Names.named("BaseModuleProvider")).to(ModuleProvider.class).in(Singleton.class);
      this.bind(IVisualContextProvider.class).to(VisualContextProvider.class).in(Singleton.class);
      Multibinder<IMcpTool> toolBinder = Multibinder.newSetBinder(this.binder(), IMcpTool.class);
      toolBinder.addBinding().to(FindMcpTool.class);
      toolBinder.addBinding().to(GetObjectMcpTool.class);
      Multibinder<IMarkersProvider> markersProviderBinder = Multibinder.newSetBinder(this.binder(), IMarkersProvider.class);
      markersProviderBinder.addBinding().to(MarkersProvider.class);
      Multibinder<IJShellBindingProvider> jshellBindingProviderBinder = Multibinder.newSetBinder(this.binder(), IJShellBindingProvider.class);
      jshellBindingProviderBinder.addBinding().to(MetadataBindingProvider.class);
      this.bind(IExternalPropertyManagerRegistry.class).toService();
      this.bind(IBmModelManager.class).toService();
      this.bind(IResourceLookup.class).toService();
      this.bind(IDataSourceInfoAssociationService.class).toService();
      this.bind(IV8ProjectManager.class).toService();
      this.bind(IProjectFileSystemSupportProvider.class).toService();
      this.bind(IQualifiedNameFilePathConverter.class).toService();
      this.bind(IDtHostResourceManager.class).toService();
      this.bind(ITextSearchIndexProvider.class).toService();
      this.bind(IModelEditingSupport.class).toService();
      this.bind(IMarkerManagerV2.class).toService();
      this.bind(ITopObjectFqnGenerator.class).toService();
      this.bind(IModelObjectFactory.class).toService();
   }
}
