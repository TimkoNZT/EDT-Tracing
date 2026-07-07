package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IGlobalContext;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.IGlobalContextService;
import com.e1c.edt.ai.assistent.model.EntityValue;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class GlobalContextSync implements IGlobalContextSync {
   private final ILog log;
   private final ISettings settings;
   private final Provider<IStatistics> statisticsProvider;
   private final IJson json;
   private final IGlobalContext globalContext;
   private final IGlobalContextService globalContextService;

   @Inject
   public GlobalContextSync(ILog log, ISettings settings, Provider<IStatistics> statisticsProvider, IJson json, IGlobalContext globalContext, IGlobalContextService globalContextService) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(statisticsProvider);
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(globalContext);
      Preconditions.checkNotNull(globalContextService);
      this.log = log;
      this.settings = settings;
      this.statisticsProvider = statisticsProvider;
      this.json = json;
      this.globalContext = globalContext;
      this.globalContextService = globalContextService;
   }

   public CompletableFuture<Boolean> sync(AIContext aiContext, int maxDept, ICancellationToken cancellationToken) {
      try {
         IStatistics statistics = (IStatistics)this.statisticsProvider.get();
         List<GlobalContextUpdate> updates = this.globalContext.getUpdates(aiContext, statistics, cancellationToken);
         return this.syncUpdates(aiContext, updates, maxDept, statistics, cancellationToken);
      } catch (Exception error) {
         this.log.logError(error);
         return CompletableFuture.completedFuture(false);
      }
   }

   public CompletableFuture<Boolean> syncUpdates(AIContext aiContext, List<GlobalContextUpdate> updates, int maxDept, IStatistics statistics, ICancellationToken cancellationToken) {
      try {
         if (updates.isEmpty()) {
            return CompletableFuture.completedFuture(true);
         } else {
            return cancellationToken.isCanceled() ? CompletableFuture.completedFuture(false) : this.globalContextService.update(aiContext.getProjectId(), updates, 200, statistics, cancellationToken).thenCompose((optionalResult) -> {
               if (optionalResult.isEmpty()) {
                  return CompletableFuture.completedFuture(false);
               } else {
                  GlobalContextUpdateResponse result = (GlobalContextUpdateResponse)optionalResult.get();
                  return result.isEmpty() ? CompletableFuture.completedFuture(true) : this.syncUnknown(aiContext, result.unknownValues, maxDept, cancellationToken);
               }
            });
         }
      } catch (Exception error) {
         this.log.logError(error);
         return CompletableFuture.completedFuture(false);
      }
   }

   public CompletableFuture<Boolean> syncUnknown(AIContext aiContext, List<EntityValue> unknownValues, int maxDept, ICancellationToken cancellationToken) {
      CompletableFuture<Boolean> feature = CompletableFuture.completedFuture(true);
      if (unknownValues != null && !unknownValues.isEmpty()) {
         try {
            if (cancellationToken.isCanceled()) {
               return CompletableFuture.completedFuture(false);
            } else {
               IStatistics statistics = (IStatistics)this.statisticsProvider.get();
               GlobalContextUpdateResponse response = new GlobalContextUpdateResponse();
               response.unknownValues = unknownValues;
               Optional<GlobalContextUpdateResponse> optionalResult = Optional.ofNullable(response);

               while(maxDept-- > 0 && optionalResult.isPresent()) {
                  GlobalContextUpdateResponse result = (GlobalContextUpdateResponse)optionalResult.get();
                  if (result.isEmpty()) {
                     return feature;
                  }

                  List<EntityValue> vals = result.unknownValues;
                  boolean hasUnknownValues = vals != null && !vals.isEmpty();
                  this.log.trace("sync", "AI global context is needed " + cancellationToken.toString(), () -> {
                     StringBuilder trace = new StringBuilder();
                     if (hasUnknownValues) {
                        trace.append("Unknown values:");
                        trace.append(System.lineSeparator());
                        trace.append(this.json.serialize(vals));
                     }

                     return trace.toString();
                  });
                  HashMap<String, FileUpdates> fileUpdates = new HashMap();
                  if (vals != null) {
                     for(EntityValue val : vals) {
                        String fileHash = null;
                        String field = val.field;
                        if ("local_functions".equalsIgnoreCase(field) || "form".equalsIgnoreCase(field) || "meta".equalsIgnoreCase(field)) {
                           fileHash = val.hash;
                        }

                        FileUpdates fileUpdate = (FileUpdates)fileUpdates.computeIfAbsent(val.path, (path) -> new FileUpdates(path, fileHash));
                        fileUpdate.hashes.add(val.hash);
                        fileUpdate.fields.add(val.field);
                     }
                  }

                  if (fileUpdates.isEmpty()) {
                     return feature;
                  }

                  ArrayList<GlobalContextUpdate> updates = new ArrayList();

                  for(FileUpdates fileUpdate : fileUpdates.values()) {
                     List<GlobalContextUpdate> newUpdates = this.globalContext.getUpdates(new AIContext(aiContext.getProjectId(), fileUpdate.filePath, aiContext.getDocument()), fileUpdate.fileHash, fileUpdate.hashes, fileUpdate.fields, statistics, cancellationToken);
                     if (!newUpdates.isEmpty()) {
                        synchronized(updates) {
                           updates.addAll(newUpdates);
                        }

                        feature = feature.thenCompose((i) -> {
                           ArrayList<GlobalContextUpdate> latestUpdates;
                           synchronized(updates) {
                              latestUpdates = new ArrayList(updates);
                              updates.clear();
                           }

                           return !cancellationToken.isCanceled() && !latestUpdates.isEmpty() ? this.syncUpdates(aiContext, latestUpdates, 5, statistics, cancellationToken) : CompletableFuture.completedFuture(true);
                        });

                        try {
                           Duration timeout = this.settings.getTimeout();
                           optionalResult = (Optional)this.globalContextService.update(aiContext.getProjectId(), updates, 10, statistics, cancellationToken).get(timeout.toNanos(), TimeUnit.NANOSECONDS);
                        } catch (TimeoutException var19) {
                           this.log.warning("sync", () -> "Global context update timed out after " + this.settings.getTimeout());
                           return CompletableFuture.completedFuture(false);
                        }
                     }
                  }

                  if (cancellationToken.isCanceled()) {
                     return feature;
                  }
               }

               return feature;
            }
         } catch (Exception error) {
            this.log.logError(error);
            return CompletableFuture.completedFuture(false);
         }
      } else {
         return feature;
      }
   }

   private static class FileUpdates {
      private final String filePath;
      private final String fileHash;
      public final HashSet<String> hashes = new HashSet();
      public final HashSet<String> fields = new HashSet();

      public FileUpdates(String filePath, String fileHash) {
         this.filePath = filePath;
         this.fileHash = fileHash;
      }
   }
}
