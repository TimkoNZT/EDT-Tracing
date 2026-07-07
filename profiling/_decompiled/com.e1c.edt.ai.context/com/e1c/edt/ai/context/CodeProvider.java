package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.model.Method;
import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.ICodeProvider;
import java.util.Optional;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parser.IParseResult;

class CodeProvider implements ICodeProvider {
   public Optional<CodeMethod> getMethod(IParseResult parseResult, int offset) {
      if (parseResult == null) {
         return Optional.empty();
      } else {
         for(INode error : parseResult.getSyntaxErrors()) {
            if (error.getTotalEndOffset() < offset) {
               return Optional.empty();
            }
         }

         ICompositeNode rootNode = parseResult.getRootNode();
         if (rootNode == null) {
            return Optional.empty();
         } else {
            ILeafNode cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNode, offset);
            if (cursorNode == null) {
               return Optional.empty();
            } else {
               EObject semantic = NodeModelUtils.findActualSemanticObjectFor(cursorNode);
               if (semantic == null) {
                  return Optional.empty();
               } else {
                  Method method = (Method)EcoreUtil2.getContainerOfType(semantic, Method.class);
                  if (method == null) {
                     return Optional.empty();
                  } else {
                     ICompositeNode methodNode = NodeModelUtils.getNode(method);
                     int startOffest = methodNode.getTotalOffset();
                     int endOffest = methodNode.getTotalEndOffset();
                     return Optional.of(new CodeMethod(method.getUniqueName(), startOffest, endOffest, Optional.of(parseResult)));
                  }
               }
            }
         }
      }
   }

   public Optional<String> getMethodBody(IParseResult parseResult, CodeMethod method) {
      ICompositeNode rootNode = parseResult.getRootNode();
      if (rootNode == null) {
         return Optional.empty();
      } else {
         EObject rootSemantic = NodeModelUtils.findActualSemanticObjectFor(rootNode);
         if (rootSemantic == null) {
            return Optional.empty();
         } else {
            for(Method curMethod : EcoreUtil2.getAllContentsOfType(rootSemantic, Method.class)) {
               if (curMethod.getUniqueName().equals(method.getUniqueName())) {
                  ICompositeNode methodNode = NodeModelUtils.getNode(curMethod);
                  if (methodNode != null) {
                     StringBuilder sb = new StringBuilder();

                     for(ILeafNode leafNode : methodNode.getLeafNodes()) {
                        sb.append(leafNode.getText());
                     }

                     return Optional.of(sb.toString());
                  }
                  break;
               }
            }

            return Optional.empty();
         }
      }
   }
}
