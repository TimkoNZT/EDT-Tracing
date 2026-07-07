package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.model.EntityValue;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

interface IGlobalContextSync {
   CompletableFuture<Boolean> sync(AIContext var1, int var2, ICancellationToken var3);

   CompletableFuture<Boolean> syncUpdates(AIContext var1, List<GlobalContextUpdate> var2, int var3, IStatistics var4, ICancellationToken var5);

   CompletableFuture<Boolean> syncUnknown(AIContext var1, List<EntityValue> var2, int var3, ICancellationToken var4);
}
