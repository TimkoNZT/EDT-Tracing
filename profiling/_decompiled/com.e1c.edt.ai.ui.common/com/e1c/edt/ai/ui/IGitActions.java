package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IGitActions {
   void reviewGitChanges(List<GitDiff> var1, ICancellationToken var2);

   IObservable<CommitMessage> ceateGitCommitMessageSource(String var1, List<GitDiff> var2, ICancellationToken var3);

   CompletableFuture<Optional<String>> feedbackAsync(CommitMessage var1, String var2, ICancellationToken var3);
}
