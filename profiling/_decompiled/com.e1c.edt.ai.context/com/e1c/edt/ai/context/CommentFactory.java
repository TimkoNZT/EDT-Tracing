package com.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.documentation.comment.IDescriptionPart;
import com._1c.g5.v8.dt.bsl.documentation.comment.LinkPart;
import com._1c.g5.v8.dt.bsl.documentation.comment.TextPart;
import com._1c.g5.v8.dt.bsl.documentation.comment.TypeSection;
import com.e1c.edt.ai.context.DTO.Comment;
import com.e1c.edt.ai.context.DTO.CommentDescriptionPart;
import com.e1c.edt.ai.context.DTO.CommentFieldDefinition;
import com.e1c.edt.ai.context.DTO.CommentParameter;
import com.e1c.edt.ai.context.DTO.CommentParameters;
import com.e1c.edt.ai.context.DTO.CommentReturn;
import com.e1c.edt.ai.context.DTO.CommentType;
import com.e1c.edt.ai.context.DTO.CommentTypeDefinition;
import java.util.ArrayList;
import java.util.List;

class CommentFactory implements ICommentFactory {
   public Comment create(BslDocumentationComment bslComment) {
      Comment comment = new Comment();
      comment.description = this.createDescription(bslComment.getDescription());
      comment.parameters = this.createParameters(bslComment.getParametersSection());
      BslDocumentationComment.Section exampleSection = bslComment.getExampleSection();
      if (exampleSection != null) {
         comment.exampleDescription = this.createDescription(exampleSection.getDescription());
      }

      BslDocumentationComment.Section callOptionsSection = bslComment.getCallOptionsSection();
      if (callOptionsSection != null) {
         comment.callOptionsDescription = this.createDescription(callOptionsSection.getDescription());
      }

      comment.returnInfo = this.createReturn(bslComment.getReturnSection());
      return comment;
   }

   private CommentReturn createReturn(BslDocumentationComment.ReturnSection returnSection) {
      if (returnSection == null) {
         return null;
      } else {
         CommentReturn returnInfo = new CommentReturn();
         returnInfo.returnDescription = this.createDescription(returnSection.getDescription());
         List<TypeSection> returnTypes = returnSection.getReturnTypes();
         if (!returnTypes.isEmpty()) {
            returnInfo.returnTypes = new ArrayList();

            for(TypeSection returnType : returnSection.getReturnTypes()) {
               returnInfo.returnTypes.add(this.createType(returnType));
            }
         }

         return returnInfo;
      }
   }

   private CommentParameters createParameters(BslDocumentationComment.ParametersSection parametersSection) {
      if (parametersSection == null) {
         return null;
      } else {
         CommentParameters commentParameters = new CommentParameters();
         List<TypeSection.FieldDefinition> params = parametersSection.getParameterDefinitions();
         if (!params.isEmpty()) {
            commentParameters.parameters = new ArrayList();

            for(TypeSection.FieldDefinition param : params) {
               CommentParameter commentParameter = new CommentParameter();
               commentParameters.parameters.add(commentParameter);
               commentParameter.description = this.createDescription(param.getDescription());
               commentParameter.name = param.getName();
               List<TypeSection> types = param.getTypeSections();
               if (!types.isEmpty()) {
                  commentParameter.types = new ArrayList();

                  for(TypeSection type : types) {
                     commentParameter.types.add(this.createType(type));
                  }
               }
            }
         }

         commentParameters.parametersDescription = this.createDescription(parametersSection.getDescription());
         commentParameters.sourceDescription = this.createDescription(parametersSection.getSourceDescription());
         List<TypeSection.FieldDefinition> parameterFieldDefinitions = parametersSection.getParameterDefinitions();
         if (!parameterFieldDefinitions.isEmpty()) {
            commentParameters.parametersFieldDefinitions = new ArrayList();

            for(TypeSection.FieldDefinition parameterFieldDefinition : parameterFieldDefinitions) {
               commentParameters.parametersFieldDefinitions.add(this.createFieldDefinition(parameterFieldDefinition));
            }
         }

         return commentParameters;
      }
   }

   private CommentType createType(TypeSection type) {
      CommentType commentType = new CommentType();
      commentType.description = this.createDescription(type.getDescription());
      commentType.sourceDescription = this.createDescription(type.getSourceDescription());
      commentType.sourceExtensionDescription = this.createDescription(type.getSourceExtensionDescription());
      List<TypeSection.TypeDefinition> typeDefinitions = type.getTypeDefinitions();
      if (!typeDefinitions.isEmpty()) {
         commentType.typeDefinitions = new ArrayList();

         for(TypeSection.TypeDefinition typeDefinition : typeDefinitions) {
            commentType.typeDefinitions.add(this.createTypeDefenition(typeDefinition));
         }
      }

      return commentType;
   }

   private CommentTypeDefinition createTypeDefenition(TypeSection.TypeDefinition typeDefinition) {
      CommentTypeDefinition commentTypeDefinition = new CommentTypeDefinition();
      commentTypeDefinition.name = typeDefinition.getTypeName();
      List<TypeSection.FieldDefinition> fieldDefenitions = typeDefinition.getFieldDefinitionExtension();
      if (!fieldDefenitions.isEmpty()) {
         commentTypeDefinition.fieldDefinitions = new ArrayList();

         for(TypeSection.FieldDefinition fieldDefinition : fieldDefenitions) {
            commentTypeDefinition.fieldDefinitions.add(this.createFieldDefinition(fieldDefinition));
         }
      }

      return commentTypeDefinition;
   }

   private CommentFieldDefinition createFieldDefinition(TypeSection.FieldDefinition fieldDefinition) {
      CommentFieldDefinition commentFieldDefinition = new CommentFieldDefinition();
      commentFieldDefinition.name = fieldDefinition.getName();
      commentFieldDefinition.description = this.createDescription(fieldDefinition.getDescription());
      List<TypeSection> fieldDefenitions = fieldDefinition.getTypeSections();
      if (!fieldDefenitions.isEmpty()) {
         commentFieldDefinition.types = new ArrayList();

         for(TypeSection fieldType : fieldDefenitions) {
            commentFieldDefinition.types.add(this.createType(fieldType));
         }
      }

      return commentFieldDefinition;
   }

   private List<CommentDescriptionPart> createDescription(BslDocumentationComment.Description description) {
      if (description == null) {
         return null;
      } else {
         List<IDescriptionPart> parts = description.getParts();
         if (parts.isEmpty()) {
            return null;
         } else {
            ArrayList<CommentDescriptionPart> descrition = new ArrayList();

            for(IDescriptionPart part : parts) {
               CommentDescriptionPart descriptionPart = new CommentDescriptionPart();
               descrition.add(descriptionPart);
               if (part instanceof TextPart) {
                  TextPart textPart = (TextPart)part;
                  descriptionPart.kind = "text";
                  descriptionPart.text = textPart.getText();
               } else if (part instanceof LinkPart) {
                  LinkPart linkPart = (LinkPart)part;
                  descriptionPart.kind = "link";
                  descriptionPart.text = linkPart.getInitialContent();
                  descriptionPart.link = linkPart.getLinkText();
               } else if (part instanceof TypeSection) {
                  TypeSection typeSection = (TypeSection)part;
                  descriptionPart.kind = "type";
                  descriptionPart.type = this.createType(typeSection);
               } else if (part instanceof BslDocumentationComment.ParametersSection) {
                  BslDocumentationComment.ParametersSection parametersSection = (BslDocumentationComment.ParametersSection)part;
                  descriptionPart.kind = "parameters";
                  descriptionPart.parameters = this.createParameters(parametersSection);
               } else if (part instanceof BslDocumentationComment.ReturnSection) {
                  BslDocumentationComment.ReturnSection returnSection = (BslDocumentationComment.ReturnSection)part;
                  descriptionPart.kind = "return";
                  descriptionPart.returnInfo = this.createReturn(returnSection);
               } else if (part instanceof TypeSection.FieldDefinition) {
                  TypeSection.FieldDefinition fieldDefinition = (TypeSection.FieldDefinition)part;
                  descriptionPart.kind = "field";
                  descriptionPart.field = this.createFieldDefinition(fieldDefinition);
               } else if (!(part instanceof TypeSection.LinkContainsTypeDefinition)) {
                  descriptionPart.kind = "unknown";
               } else {
                  TypeSection.LinkContainsTypeDefinition linkContainsTypeDefinition = (TypeSection.LinkContainsTypeDefinition)part;
                  descriptionPart.kind = "linkWithType";
                  descriptionPart.link = linkContainsTypeDefinition.getLink().getLinkText();
                  descriptionPart.linkToExtensionFields = linkContainsTypeDefinition.getLinkToExtensionFields().getLinkText();
                  descriptionPart.typeName = linkContainsTypeDefinition.getTypeName();
                  List<TypeSection.TypeDefinition> typeDefinitions = linkContainsTypeDefinition.getContainTypes();
                  if (!typeDefinitions.isEmpty()) {
                     descriptionPart.containingTypeDefinitions = new ArrayList();

                     for(TypeSection.TypeDefinition typeDefinition : typeDefinitions) {
                        descriptionPart.containingTypeDefinitions.add(this.createTypeDefenition(typeDefinition));
                     }
                  }

                  List<TypeSection.FieldDefinition> fieldDefinitions = linkContainsTypeDefinition.getFieldDefinitionExtension();
                  if (!fieldDefinitions.isEmpty()) {
                     descriptionPart.fieldDefinitions = new ArrayList();

                     for(TypeSection.FieldDefinition fieldDefinition : fieldDefinitions) {
                        descriptionPart.fieldDefinitions.add(this.createFieldDefinition(fieldDefinition));
                     }
                  }
               }
            }

            return descrition;
         }
      }
   }
}
