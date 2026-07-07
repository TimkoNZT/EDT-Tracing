package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIModule;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectBuilder;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.context.IModuleProvider;
import com.e1c.edt.ai.context.ModuleProvider;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.xtext.builder.IXtextBuilderParticipant;

public class AIUIModule extends AbstractModule {
   public static final String PARAMETERS = "Parameters";
   public static final String URL = "URL";
   private BaseActivator activator;

   public AIUIModule(BaseActivator activator) {
      Preconditions.checkNotNull(activator);
      this.activator = activator;
   }

   protected void configure() {
      this.install(new AIModule());
      this.bind(ILog.class).toInstance(this.activator);
      this.bind(IDefaultSettings.class).to(DefaultSettings.class).in(Singleton.class);
      this.bind(IVersionProvider.class).toInstance(this.activator);
      this.bind(IPreferenceStore.class).toInstance(this.activator.getPreferenceStore());
      this.bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
      this.bind(IModuleProvider.class).annotatedWith(Names.named("BaseModuleProvider")).to(ModuleProvider.class).in(Singleton.class);
      this.bind(IModuleProvider.class).to(CurrentEditorModuleProvider.class);
      this.bind(IProjectIdProvider.class).to(ModuleProvider.class);
      this.bind(IProjectProvider.class).to(ModuleProvider.class);
      this.bind(IXtextBuilderParticipant.class).to(BuildTrackingParticipant.class).in(Singleton.class);
      this.bind(ICodeParser.class).to(CodeParser.class).in(Singleton.class);
      this.bind(IModuleNameProvider.class).to(ModuleNameProvider.class).in(Singleton.class);
      this.bind(IProjectBuilder.class).to(ProjectBuilder.class).in(Singleton.class);
      this.bind(ISpecializedEditorOpener.class).to(EdtSpecializedEditorOpener.class).in(Singleton.class);
   }
}
