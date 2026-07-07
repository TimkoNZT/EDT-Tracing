package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CodePart;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.Range;
import com.e1c.edt.ai.assistent.model.CursorInfo;
import com.e1c.edt.ai.assistent.model.RelativeLocation;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import java.util.Iterator;
import java.util.Optional;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

public class CursorInfoProvider implements ICursorInfoProvider {
   private final IDispatcher dispatcher;
   private final IUI ui;
   private final ICodePartsProvider codePartsProvider;
   private final ICodeParser codeParser;

   @Inject
   public CursorInfoProvider(IDispatcher dispatcher, IUI ui, ICodePartsProvider codePartsProvider, ICodeParser codeParser) {
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(codePartsProvider);
      Preconditions.checkNotNull(codeParser);
      this.dispatcher = dispatcher;
      this.ui = ui;
      this.codePartsProvider = codePartsProvider;
      this.codeParser = codeParser;
   }

   public Optional<CursorInfo> getCursorInfo(int cursorOffset) {
      return this.dispatcher.dispatch((Supplier)(() -> this.ui.getTextWidget().flatMap((textWidget) -> this.ui.getSourceViewer(textWidget)))).flatMap((i) -> i).flatMap((sourceViewer) -> this.getCursorInfo(cursorOffset, sourceViewer));
   }

   private Optional<CursorInfo> getCursorInfo(int cursorOffset, SourceViewer sourceViewer) {
      if (sourceViewer.getDocument().getLength() > 1048576) {
         return Optional.empty();
      } else {
         Optional<ICompositeNode> rootNoodeOptional = this.codeParser.parse(sourceViewer).map((parseResult) -> parseResult.getRootNode());
         if (rootNoodeOptional.isEmpty()) {
            return Optional.empty();
         } else {
            ICompositeNode rootNoode = (ICompositeNode)rootNoodeOptional.get();
            ILeafNode cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNoode, cursorOffset);
            if (cursorNode == null) {
               return Optional.empty();
            } else {
               CursorInfo cursorInfo = new CursorInfo();
               Range range = Range.EMPTY;
               boolean found = false;
               Iterator<CodePart> partsIterator = this.codePartsProvider.getParts(rootNoode).iterator();

               while(partsIterator.hasNext()) {
                  CodePart part = (CodePart)partsIterator.next();
                  if (!part.getLocation().equals(cursorInfo.location)) {
                     if (found) {
                        break;
                     }

                     cursorInfo.location = part.getLocation();
                     range = part.getRange();
                  } else {
                     range = range.merge(part.getRange());
                  }

                  if (range.contains(cursorOffset)) {
                     found = true;
                  }
               }

               if (!found) {
                  return Optional.empty();
               } else {
                  int relativeCursorOffset = cursorOffset - range.getStart();
                  double normalizedCursorOffset = (double)relativeCursorOffset / (double)range.getLength();
                  cursorInfo.relativeLocation = RelativeLocation.Middle;
                  if (normalizedCursorOffset <= 0.2) {
                     cursorInfo.relativeLocation = RelativeLocation.Start;
                  } else if (normalizedCursorOffset >= 0.8) {
                     cursorInfo.relativeLocation = RelativeLocation.End;
                  }

                  return Optional.of(cursorInfo);
               }
            }
         }
      }
   }
}
