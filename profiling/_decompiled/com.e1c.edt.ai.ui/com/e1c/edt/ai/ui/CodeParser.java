package com.e1c.edt.ai.ui;

import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

class CodeParser implements ICodeParser {
   private final ILog log;
   private final IDispatcher dispatcher;
   private final IClock clock;

   @Inject
   public CodeParser(ILog log, IDispatcher dispatcher, IClock clock) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(clock);
      this.log = log;
      this.dispatcher = dispatcher;
      this.clock = clock;
   }

   public Optional<IParseResult> parse(SourceViewer sourceViewer) {
      Preconditions.checkNotNull(sourceViewer);
      this.dispatcher.checkThread(false, false);
      IDocument document = sourceViewer.getDocument();
      LocalDateTime startTime = this.clock.now();
      if (document instanceof BslXtextDocument) {
         BslXtextDocument bslDocument = (BslXtextDocument)document;
         IParseResult pareseResult = (IParseResult)bslDocument.readOnlyDataModelWithoutSync(new IUnitOfWork<IParseResult, XtextResource>() {
            public IParseResult exec(XtextResource state) throws Exception {
               IParseResult result = state.getParseResult();
               return result != null && result.getRootASTElement() != null ? result : null;
            }
         });
         this.log.trace("common", "Code parser", () -> "The duration of the parsing is " + Duration.between(startTime, this.clock.now()));
         return Optional.ofNullable(pareseResult);
      } else {
         return Optional.empty();
      }
   }
}
