package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

public interface IGitTools {
   void getDiff(Repository var1, int var2, OutputStream var3) throws GitAPIException, IOException;

   String getDiffText(Repository var1, int var2) throws GitAPIException, IOException;

   String getUncommittedDiffText(Repository var1, int var2) throws GitAPIException, IOException;

   String getDiffText(Repository var1, String var2, String var3, int var4) throws GitAPIException, IOException;

   List<GitCommitInfo> getCommitHistory(Repository var1, int var2) throws GitAPIException, IOException;
}
