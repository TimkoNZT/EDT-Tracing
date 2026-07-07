package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProposalExtractor;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Proposal;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal;

public class ProposalsProvider implements IProposalsProvider {
   private final ILog log;
   private final IDispatcher dispatcher;
   private final ISettings uiSettings;
   private final IClock clock;
   private final IProposalExtractor proposalExtractor;
   private final IReflection reflection;

   @Inject
   public ProposalsProvider(ILog log, IDispatcher dispatcher, ISettings uiSettings, IClock clock, IProposalExtractor proposalExtractor, IReflection reflection) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(uiSettings);
      Preconditions.checkNotNull(clock);
      Preconditions.checkNotNull(proposalExtractor);
      Preconditions.checkNotNull(reflection);
      this.log = log;
      this.dispatcher = dispatcher;
      this.uiSettings = uiSettings;
      this.clock = clock;
      this.proposalExtractor = proposalExtractor;
      this.reflection = reflection;
   }

   public Optional<Proposal> getProposal(ICompletionProposal proposal, int minPriority, String prefix) {
      if (!(proposal instanceof ICompletionProposalExtension3)) {
         return Optional.empty();
      } else {
         Proposal prop = new Proposal();
         if (proposal instanceof ConfigurableCompletionProposal) {
            ConfigurableCompletionProposal completionProposal = (ConfigurableCompletionProposal)proposal;
            prop.priority = completionProposal.getPriority();
            if (prop.priority < minPriority) {
               return Optional.empty();
            }

            Object info = completionProposal.getAdditionalProposalInfo(new NullProgressMonitor());
            if (info != null) {
               prop.description = info.toString();
            }
         }

         CharSequence text = ((ICompletionProposalExtension3)proposal).getPrefixCompletionText((IDocument)null, 0);
         prop.prefix = (String)this.proposalExtractor.extract(prefix, text.toString()).orElse((Object)null);
         if (prop.prefix == null) {
            return Optional.empty();
         } else {
            prop.displayString = proposal.getDisplayString();
            prop.text = text.toString();
            return Optional.of(prop);
         }
      }
   }

   public Optional<List<Proposal>> getProposals(AIContext aiCtx, SourceViewer sourceViewer, int minPriority, ICancellationToken cancellationToken) {
      if (!this.uiSettings.isExperimental()) {
         return Optional.empty();
      } else if (sourceViewer.getDocument().getLength() > 1048576) {
         return Optional.empty();
      } else {
         LocalDateTime expirationDate = this.clock.now().plus(this.uiSettings.getMinRequestDelay());
         ICancellationToken ct = CancellationTokens.expiresAt(cancellationToken, this.clock, expirationDate);

         try {
            return this.getContentAssistant(sourceViewer).flatMap((assistant) -> this.getPartitionType(aiCtx, sourceViewer).map((partitionType) -> assistant.getContentAssistProcessor(partitionType))).flatMap((assistProcessor) -> {
               int offset = aiCtx.getSourceOffset();

               try {
                  return this.dispatcher.dispatch((Supplier)(() -> {
                     ICompletionProposal[] result = assistProcessor.computeCompletionProposals(sourceViewer, offset);
                     assistProcessor.computeCompletionProposals(sourceViewer, offset);
                     return result;
                  }));
               } catch (Exception var6) {
                  return Optional.empty();
               }
            }).flatMap((proposals) -> this.getProposals(proposals, minPriority, aiCtx.getPrefix(), ct));
         } catch (Exception var8) {
            return Optional.empty();
         }
      }
   }

   private Optional<IContentAssistant> getContentAssistant(SourceViewer sourceViewer) {
      return this.reflection.getField(SourceViewer.class, sourceViewer, "fContentAssistant", IContentAssistant.class);
   }

   private Optional<String> getPartitionType(AIContext aiCtx, SourceViewer sourceViewer) {
      try {
         IDocument document = sourceViewer.getDocument();
         if (document != null) {
            return Optional.ofNullable(document.getPartition(aiCtx.getSourceOffset()).getType());
         }
      } catch (BadLocationException error) {
         this.log.logError(error);
      }

      return Optional.empty();
   }

   private Optional<List<Proposal>> getProposals(ICompletionProposal[] proposals, int minPriority, String prefix, ICancellationToken cancellationToken) {
      ArrayList<Proposal> result = new ArrayList();

      for(ICompletionProposal proposal : proposals) {
         if (cancellationToken.isCanceled()) {
            break;
         }

         Optional<Proposal> optionalProposal = this.getProposal(proposal, minPriority, prefix);
         if (optionalProposal.isEmpty()) {
            break;
         }

         result.add((Proposal)optionalProposal.get());
      }

      return Optional.of(result);
   }
}
