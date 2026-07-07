package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.assistent.ITools;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequestContent;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponseContent;
import com.e1c.edt.ai.assistent.model.VisualContext;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.core.runtime.jobs.Job;

public class TextActions implements ITextActions {
   private final ILog log;
   private final IDispatcher dispatcher;
   private final ISettings settings;
   private final ITools tools;
   private final IResourceProvider resourceProvider;
   private final IJson json;
   private Job currentJob;

   @Inject
   public TextActions(ILog log, IDispatcher dispatcher, ISettings settings, ITools tools, IResourceProvider resourceProvider, IJson json) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(tools);
      Preconditions.checkNotNull(resourceProvider);
      Preconditions.checkNotNull(json);
      this.log = log;
      this.dispatcher = dispatcher;
      this.settings = settings;
      this.tools = tools;
      this.resourceProvider = resourceProvider;
      this.json = json;
   }

   public IObservable<TextImprovements> ceateTextImprovementsSource(VisualContext context, TextAction action, ICancellationToken cancellationToken) {
      return Observables.create((observer) -> {
         Job job = this.dispatcher.createJob(Messages.BackgroundJobName, (jobCtx) -> {
            try {
               this.ceateTextImprovements(context, action, observer, cancellationToken);
            } catch (Exception error) {
               this.log.logError(error);
               observer.onError(error);
            }

         }, false, cancellationToken);
         this.runJob(job);
         return Closeables.Empty;
      });
   }

   private void ceateTextImprovements(VisualContext context, TextAction action, final IObserver<TextImprovements> observer, ICancellationToken cancellationToken) {
      String contextJson = this.json.serialize(context);
      ToolInvokeRequest toolInvokeRequest = new ToolInvokeRequest();
      toolInvokeRequest.toolName = "raw";
      toolInvokeRequest.uiLanguage = this.settings.getLanguage();
      ToolInvokeRequestContent content = new ToolInvokeRequestContent();
      toolInvokeRequest.content = content;
      content.instruction = ((String)this.resourceProvider.getTextResource(action.resourceName).orElse("")).replace("${language}", this.settings.getLanguage()).replace("${context}", contextJson);
      this.log.trace("api_calls", "Prompt", () -> content.instruction);
      final StringBuilder message = new StringBuilder();
      final StringBuilder uudi = new StringBuilder();
      IObservable<ToolInvokeResponse> invokeSource = this.tools.createInvokeSource(ProjectId.Default, toolInvokeRequest, cancellationToken);
      invokeSource.subscribe(new IObserver<ToolInvokeResponse>() {
         public void onNext(ToolInvokeResponse value) {
            ToolInvokeResponseContent content = value.content;
            if (value.uuid != null) {
               uudi.setLength(0);
               uudi.append(value.uuid);
            }

            if (content != null) {
               String text = content.text;
               if (text != null) {
                  if (value.finished) {
                     text = text.trim();
                  } else {
                     synchronized(message) {
                        message.append(text);
                        text = message.toString().trim();
                     }
                  }

                  if (!text.isBlank()) {
                     observer.onNext(new TextImprovements(uudi.toString(), text));
                  }
               }
            }

         }

         public void onError(Throwable error) {
            uudi.setLength(0);
            observer.onError(error);
         }

         public void onCompleted() {
            uudi.setLength(0);
            observer.onCompleted();
         }
      });
   }

   private synchronized void runJob(Job job) {
      if (this.currentJob != null) {
         this.currentJob.cancel();
         this.currentJob = null;
      }

      this.currentJob = job;
      this.currentJob.setPriority(50);
      job.schedule();
   }
}
