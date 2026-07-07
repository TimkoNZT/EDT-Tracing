package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationToken;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IProject;

public interface IBuildWaiter {
   CompletableFuture<Void> waitForBuilds(IProject var1, ICancellationToken var2);
}
