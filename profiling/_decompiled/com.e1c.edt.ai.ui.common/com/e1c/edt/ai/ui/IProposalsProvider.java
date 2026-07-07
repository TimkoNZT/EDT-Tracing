package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.assistent.model.Proposal;
import java.util.List;
import java.util.Optional;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;

public interface IProposalsProvider {
   Optional<Proposal> getProposal(ICompletionProposal var1, int var2, String var3);

   Optional<List<Proposal>> getProposals(AIContext var1, SourceViewer var2, int var3, ICancellationToken var4);
}
