package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Method;
import com.e1c.edt.ai.CodePart;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.Range;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.TerminalRule;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.impl.CompositeNodeWithSemanticElement;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

class CodePartsProvider implements ICodePartsProvider {
   public boolean isMethod(INode node) {
      Preconditions.checkNotNull(node);
      EObject semantic = NodeModelUtils.findActualSemanticObjectFor(node);
      if (semantic == null) {
         return false;
      } else {
         return EcoreUtil2.getContainerOfType(semantic, Method.class) != null;
      }
   }

   public Stream<CodePart> getParts(ICompositeNode rootNode) {
      Preconditions.checkNotNull(rootNode);
      Stream<ILeafNode> leafNodes = StreamSupport.stream(Spliterators.spliteratorUnknownSize(rootNode.getLeafNodes().iterator(), 1024), false);
      Stream<CodeMarker> markers = this.getMarkers(leafNodes);
      CodePartIterator codePartIterator = new CodePartIterator(markers);
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(codePartIterator, 1024), false);
   }

   private Stream<CodeMarker> getMarkers(Stream<ILeafNode> leafNodes) {
      return leafNodes.map((leafNode) -> {
         String text = leafNode.getText();
         Range range = new Range(leafNode.getTotalOffset(), leafNode.getLength());
         EObject semantic = NodeModelUtils.findActualSemanticObjectFor(leafNode);
         if (semantic == null) {
            return new CodeMarker((Method)null, range, CodePartsProvider.MarkerType.Unknown, text);
         } else {
            Method method = (Method)EcoreUtil2.getContainerOfType(semantic, Method.class);
            if (method == null) {
               return new CodeMarker(method, range, CodePartsProvider.MarkerType.Unknown, text);
            } else {
               EObject grammar = leafNode.getGrammarElement();
               if (grammar instanceof TerminalRule) {
                  TerminalRule terminalRule = (TerminalRule)grammar;
                  String name = terminalRule.getName();
                  if ("SL_COMMENT".equals(name)) {
                     return new CodeMarker(method, range, CodePartsProvider.MarkerType.Comment, text);
                  }
               }

               if (grammar instanceof Keyword) {
                  Keyword keyword = (Keyword)grammar;
                  if (this.getAlternatives(grammar).filter((i) -> i instanceof Keyword).map((i) -> (Keyword)i).anyMatch((i) -> "Процедура".equalsIgnoreCase(i.getValue()) || "Функция".equalsIgnoreCase(i.getValue()))) {
                     return new CodeMarker(method, range, CodePartsProvider.MarkerType.MethodStart, text);
                  }

                  if (this.getAlternatives(grammar).filter((i) -> i instanceof Keyword).map((i) -> (Keyword)i).anyMatch((i) -> "КонецПроцедуры".equalsIgnoreCase(i.getValue()) || "КонецФункции".equalsIgnoreCase(i.getValue()))) {
                     return new CodeMarker(method, range, CodePartsProvider.MarkerType.MethodFinish, text);
                  }

                  if (this.isArgRelated(leafNode) && "(".equals(keyword.getValue())) {
                     return new CodeMarker(method, range, CodePartsProvider.MarkerType.MethodArgStart, text);
                  }

                  if (this.isArgRelated(leafNode) && ")".equals(keyword.getValue())) {
                     return new CodeMarker(method, range, CodePartsProvider.MarkerType.MethodArgFinish, text);
                  }
               }

               return new CodeMarker(method, range, CodePartsProvider.MarkerType.Unknown, text);
            }
         }
      });
   }

   private boolean isArgRelated(ILeafNode node) {
      ICompositeNode parent = node.getParent();
      if (parent == null) {
         return false;
      } else if (!(parent instanceof CompositeNodeWithSemanticElement)) {
         return false;
      } else {
         CompositeNodeWithSemanticElement parentWithSemantic = (CompositeNodeWithSemanticElement)parent;
         EObject semanticElement = parentWithSemantic.getSemanticElement();
         return semanticElement instanceof Method || semanticElement instanceof Function;
      }
   }

   private Stream<AbstractElement> getAlternatives(EObject obj) {
      Preconditions.checkNotNull(obj);
      EObject container = obj.eContainer();
      return container instanceof Alternatives ? ((Alternatives)container).getElements().stream() : Stream.empty();
   }

   private class CodePartIterator implements Iterator<CodePart> {
      private final Iterator<CodeMarker> markers;
      private final Stack<CursorLocation> locations = new Stack();
      private final HashSet<Method> methods = new HashSet();
      private boolean beforeArgs;
      private int lastMethodId;
      // $FF: synthetic field
      private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$context$CodePartsProvider$MarkerType;

      public CodePartIterator(Stream<CodeMarker> markers) {
         Preconditions.checkNotNull(markers);
         this.markers = markers.iterator();
      }

      public boolean hasNext() {
         return this.markers.hasNext();
      }

      public CodePart next() {
         CodeMarker marker = (CodeMarker)this.markers.next();
         Method method = marker.method;
         Integer methodId = null;
         if (method != null) {
            if (this.methods.add(method)) {
               ++this.lastMethodId;
               this.beforeArgs = true;
            }

            methodId = this.lastMethodId;
         }

         CodePart codePart = new CodePart(methodId, marker.range, CursorLocation.OutsideFunction, marker.text);
         switch (marker.type) {
            case Unknown:
               CursorLocation lastLocation = this.getLastLocation();
               if (lastLocation == CursorLocation.FunctionBody && this.beforeArgs) {
                  lastLocation = CursorLocation.FunctionName;
               }

               codePart = new CodePart(methodId, marker.range, lastLocation, marker.text);
               break;
            case Comment:
               if (this.getLastLocation() == CursorLocation.FunctionBody) {
                  codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionBody, marker.text);
               } else {
                  codePart = new CodePart(methodId, marker.range, CursorLocation.Comment, marker.text);
               }
               break;
            case MethodStart:
               this.locations.push(CursorLocation.FunctionBody);
               codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionBody, marker.text);
               break;
            case MethodFinish:
               this.popLocation();
               codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionBody, marker.text);
               break;
            case MethodArgStart:
               if (this.getLastLocation() == CursorLocation.FunctionBody) {
                  this.locations.push(CursorLocation.FunctionArguments);
                  codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionArguments, marker.text);
                  this.beforeArgs = false;
               }
               break;
            case MethodArgFinish:
               this.popLocation();
               codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionArguments, marker.text);
         }

         return codePart;
      }

      private void popLocation() {
         if (this.locations.size() > 0) {
            this.locations.pop();
         }

      }

      private CursorLocation getLastLocation() {
         return this.locations.size() > 0 ? (CursorLocation)this.locations.peek() : CursorLocation.OutsideFunction;
      }

      // $FF: synthetic method
      static int[] $SWITCH_TABLE$com$e1c$edt$ai$context$CodePartsProvider$MarkerType() {
         int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$context$CodePartsProvider$MarkerType;
         if (var10000 != null) {
            return var10000;
         } else {
            int[] var0 = new int[CodePartsProvider.MarkerType.values().length];

            try {
               var0[CodePartsProvider.MarkerType.Comment.ordinal()] = 2;
            } catch (NoSuchFieldError var6) {
            }

            try {
               var0[CodePartsProvider.MarkerType.MethodArgFinish.ordinal()] = 6;
            } catch (NoSuchFieldError var5) {
            }

            try {
               var0[CodePartsProvider.MarkerType.MethodArgStart.ordinal()] = 5;
            } catch (NoSuchFieldError var4) {
            }

            try {
               var0[CodePartsProvider.MarkerType.MethodFinish.ordinal()] = 4;
            } catch (NoSuchFieldError var3) {
            }

            try {
               var0[CodePartsProvider.MarkerType.MethodStart.ordinal()] = 3;
            } catch (NoSuchFieldError var2) {
            }

            try {
               var0[CodePartsProvider.MarkerType.Unknown.ordinal()] = 1;
            } catch (NoSuchFieldError var1) {
            }

            $SWITCH_TABLE$com$e1c$edt$ai$context$CodePartsProvider$MarkerType = var0;
            return var0;
         }
      }
   }

   private class CodeMarker {
      private final Method method;
      public final Range range;
      public final MarkerType type;
      public final String text;

      public CodeMarker(Method method, Range range, MarkerType type, String text) {
         Preconditions.checkNotNull(range);
         Preconditions.checkNotNull(type);
         Preconditions.checkNotNull(text);
         this.method = method;
         this.range = range;
         this.type = type;
         this.text = text;
      }

      public String toString() {
         return this.range.toString() + ": " + this.type;
      }
   }

   private static enum MarkerType {
      Unknown,
      Comment,
      MethodStart,
      MethodFinish,
      MethodArgStart,
      MethodArgFinish;
   }
}
