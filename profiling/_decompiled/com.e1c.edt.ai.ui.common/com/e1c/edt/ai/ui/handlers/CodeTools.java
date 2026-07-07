package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.CodePart;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.Range;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.e1c.edt.ai.ui.AITarget;
import com.e1c.edt.ai.ui.Content;
import com.e1c.edt.ai.ui.IAIContextProvider;
import com.e1c.edt.ai.ui.ICodeParser;
import com.e1c.edt.ai.ui.IContentProvider;
import com.e1c.edt.ai.ui.ITextWidgetInfoProvider;
import com.e1c.edt.ai.ui.IUI;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.eclipse.egit.ui.internal.commit.DiffDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

public class CodeTools implements ICodeTools {
   private final ILog log;
   private final IUI ui;
   private final ITextWidgetInfoProvider textWidgetInfoProvider;
   private final IContentProvider contentProvider;
   private final ICodeParser codeParser;
   private final ICodePartsProvider codePartsProvider;
   private final IAIContextProvider aiContextProvider;
   private final Cache<SourceViewer, Optional<TargetMethod>> targetMethodCache;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$ui$handlers$CodeAction;

   @Inject
   public CodeTools(ILog log, IUI ui, ITextWidgetInfoProvider textWidgetInfoProvider, IContentProvider contentProvider, ICodeParser codeParser, ICodePartsProvider codePartsProvider, IAIContextProvider aiContextProvider) {
      this.targetMethodCache = CacheBuilder.newBuilder().maximumSize(1L).expireAfterAccess(50L, TimeUnit.MILLISECONDS).build();
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(textWidgetInfoProvider);
      Preconditions.checkNotNull(contentProvider);
      Preconditions.checkNotNull(codeParser);
      Preconditions.checkNotNull(codePartsProvider);
      Preconditions.checkNotNull(aiContextProvider);
      this.log = log;
      this.ui = ui;
      this.textWidgetInfoProvider = textWidgetInfoProvider;
      this.contentProvider = contentProvider;
      this.codeParser = codeParser;
      this.codePartsProvider = codePartsProvider;
      this.aiContextProvider = aiContextProvider;
   }

   public boolean hasTarget(CodeAction action) {
      return (Boolean)this.ui.getLastSourceViewer().map((sourceViewer) -> this.isTarget(action, sourceViewer)).orElse(false);
   }

   public Optional<AIContext> createContextForTarget(SourceViewer sourceViewer, CodeAction action) {
      StyledText textWidget = sourceViewer.getTextWidget();
      if (this.isDiff(sourceViewer)) {
         return this.aiContextProvider.create(sourceViewer, new AITarget(textWidget, false, false), CancellationTokens.NONE);
      } else if (!textWidget.getSelectionText().isBlank()) {
         return this.aiContextProvider.create(sourceViewer, new AITarget(textWidget, false, true), CancellationTokens.NONE);
      } else {
         Optional<TargetMethod> optionalTargetMethod = this.getTargetMethod();
         return optionalTargetMethod.isPresent() ? Optional.ofNullable(((TargetMethod)optionalTargetMethod.get()).ctx) : Optional.empty();
      }
   }

   public Optional<TargetMethod> getTargetMethod() {
      return this.ui.getLastSourceViewer().flatMap((sourceViewer) -> this.getTargetMethod(sourceViewer));
   }

   private boolean isTarget(CodeAction action, SourceViewer sourceViewer) {
      if (sourceViewer == null) {
         return false;
      } else {
         switch (action) {
            case EXLPLAIN:
               if (!this.isDiff(sourceViewer) && !this.hasSelection(sourceViewer) && !this.isCodeEditor(sourceViewer)) {
                  return false;
               }

               return true;
            case CRITICISE:
               if (!this.isDiff(sourceViewer) && !this.hasSelection(sourceViewer) && !this.isCodeEditor(sourceViewer)) {
                  return false;
               }

               return true;
            case FIX:
               if ((this.hasSelection(sourceViewer) || this.isCodeEditor(sourceViewer)) && !this.isDiff(sourceViewer)) {
                  return true;
               }

               return false;
            case GENERATE_COMMENT:
               if ((this.hasSelection(sourceViewer) || this.isCodeEditor(sourceViewer)) && !this.isDiff(sourceViewer)) {
                  return true;
               }

               return false;
            case ADD:
               if (!this.hasSelection(sourceViewer) && !this.isDiff(sourceViewer) && !this.isCodeEditor(sourceViewer)) {
                  return false;
               }

               return true;
            default:
               return (this.hasSelection(sourceViewer) || this.isCodeEditor(sourceViewer)) && !this.isDiff(sourceViewer);
         }
      }
   }

   private boolean isDiff(SourceViewer sourceViewer) {
      return sourceViewer.getDocument() instanceof DiffDocument;
   }

   private boolean hasSelection(SourceViewer sourceViewer) {
      return !sourceViewer.getTextWidget().getSelectionText().isBlank();
   }

   private boolean isCodeEditor(SourceViewer sourceViewer) {
      return this.getTargetMethod().isPresent();
   }

   private Optional<Content> getContent(SourceViewer sourceViewer) {
      return this.textWidgetInfoProvider.getLastMouseOffset(sourceViewer.getTextWidget()).map((offset) -> this.contentProvider.get(sourceViewer.getTextWidget(), offset));
   }

   private Optional<TargetMethod> getTargetMethod(SourceViewer sourceViewer) {
      Optional<TargetMethod> result = (Optional)this.targetMethodCache.getIfPresent(sourceViewer);
      if (result != null) {
         return result;
      } else if (sourceViewer.getDocument().getLength() > 1048576) {
         return Optional.empty();
      } else {
         Optional<Content> optionalContent = this.getContent(sourceViewer);
         if (optionalContent.isEmpty()) {
            return Optional.empty();
         } else {
            Content content = (Content)optionalContent.get();
            Optional<ICompositeNode> optionalRootNode = this.codeParser.parse(sourceViewer).map((parseResult) -> parseResult.getRootNode());
            if (optionalRootNode.isEmpty()) {
               return Optional.empty();
            } else {
               ICompositeNode rootNode = (ICompositeNode)optionalRootNode.get();
               ILeafNode cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNode, content.offset);
               if (cursorNode == null) {
                  return Optional.empty();
               } else if (!this.codePartsProvider.isMethod(cursorNode)) {
                  return Optional.empty();
               } else {
                  TargetMethod commentingMethod = new TargetMethod();
                  Integer methodId = null;
                  Range range = Range.EMPTY;
                  HashSet<Integer> methods = new HashSet();
                  CodePart lastMethodPart = null;
                  Iterator<CodePart> partsIterator = this.codePartsProvider.getParts(rootNode).iterator();

                  while(partsIterator.hasNext()) {
                     CodePart part = (CodePart)partsIterator.next();
                     Integer curMethodId = part.getMethodId();
                     if (curMethodId != null) {
                        lastMethodPart = part;
                        if (methods.add(curMethodId)) {
                           if (methodId != null) {
                              break;
                           }

                           range = part.getRange();
                           if (part.getLocation() == CursorLocation.Comment) {
                              commentingMethod.commentRange = range;
                           } else {
                              commentingMethod.commentRange = new Range(range.getStart(), 0);
                           }
                        } else {
                           range = range.merge(part.getRange());
                        }

                        if (part.getLocation() == CursorLocation.Comment) {
                           commentingMethod.commentRange = range;
                        }

                        if (range.contains(content.offset)) {
                           methodId = curMethodId;
                        }
                     }
                  }

                  if (methodId == null && lastMethodPart != null) {
                     methodId = lastMethodPart.getMethodId();
                  }

                  if (methodId != null && commentingMethod.commentRange != null) {
                     commentingMethod.methodText = content.text.substring(range.getStart(), range.getStart() + range.getLength());
                     if (commentingMethod.methodText.isBlank()) {
                        return Optional.empty();
                     } else {
                        commentingMethod.sourceViewer = sourceViewer;
                        AITarget target = new AITarget(sourceViewer.getTextWidget(), false, true);
                        Optional<AIContext> optionalCtx = this.aiContextProvider.create(sourceViewer, target, CancellationTokens.NONE);
                        if (optionalCtx.isPresent()) {
                           AIContext ctx = (AIContext)optionalCtx.get();
                           int start = range.getStart();
                           int sourceOffset = start;
                           int textOffset = 0;
                           int finish = start + range.getLength();
                           String prefix = "";
                           String suffix = commentingMethod.methodText;
                           Optional<Integer> optionalMouseOffset = this.textWidgetInfoProvider.getLastMouseOffset(sourceViewer.getTextWidget());
                           if (optionalMouseOffset.isPresent()) {
                              Integer mouseOffset = (Integer)optionalMouseOffset.get();
                              if (mouseOffset > start && mouseOffset < finish) {
                                 sourceOffset = mouseOffset;
                                 textOffset = mouseOffset - start;
                                 prefix = suffix.substring(0, textOffset);
                                 suffix = suffix.substring(textOffset, suffix.length());
                              }
                           }

                           AIContext methodCtx = new AIContext(ctx.getProjectId(), sourceOffset, ctx.getSource(), sourceOffset, ctx.getPath(), commentingMethod.methodText, textOffset, prefix, suffix, start, finish, sourceViewer.getDocument(), () -> sourceViewer.getTextWidget().isDisposed());
                           commentingMethod.ctx = methodCtx;
                        }

                        result = Optional.of(commentingMethod);
                        this.targetMethodCache.put(sourceViewer, result);
                        return result;
                     }
                  } else {
                     return Optional.empty();
                  }
               }
            }
         }
      }
   }

   public void selectMethodComment(TargetMethod targetMethod) {
      this.getRange(targetMethod.sourceViewer.getTextWidget(), targetMethod.commentRange).ifPresent((range) -> targetMethod.sourceViewer.setSelectedRange(range.getStart(), range.getLength()));
   }

   private Optional<Range> getRange(StyledText widget, Range range) {
      try {
         int start = range.getStart();
         int length = range.getLength();
         String fullText = widget.getText();
         int dif = 0;
         int newLength = 0;
         if (length == 0) {
            if (start > 0) {
               String text = fullText.substring(start);
               dif = text.length() - text.stripLeading().length();
            }
         } else {
            String text = fullText.substring(start, start + length).stripLeading();
            newLength = text.length();
            dif = length - newLength;
         }

         if (dif < 0) {
            dif = 0;
         }

         return Optional.of(new Range(start + dif, newLength));
      } catch (Exception error) {
         this.log.logError(error);
         return Optional.empty();
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$ui$handlers$CodeAction() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$ui$handlers$CodeAction;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[CodeAction.values().length];

         try {
            var0[CodeAction.ADD.ordinal()] = 5;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[CodeAction.CRITICISE.ordinal()] = 2;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[CodeAction.EXLPLAIN.ordinal()] = 1;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[CodeAction.FIX.ordinal()] = 3;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[CodeAction.GENERATE_COMMENT.ordinal()] = 4;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$ui$handlers$CodeAction = var0;
         return var0;
      }
   }
}
