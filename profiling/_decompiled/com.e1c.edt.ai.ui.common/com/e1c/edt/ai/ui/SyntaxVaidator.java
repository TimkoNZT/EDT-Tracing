package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.resource.IResourceServiceProvider.Registry;
import org.eclipse.xtext.util.LazyStringInputStream;

class SyntaxVaidator implements ISyntaxVaidator {
   private final ILog log;
   private static final Map<String, String> PARSE_OPTIONS = Maps.newHashMap();
   private final Provider<XtextResourceSet> resourceSetProvider;

   static {
      PARSE_OPTIONS.put(XtextResource.OPTION_ENCODING, StandardCharsets.UTF_8.name());
   }

   @Inject
   public SyntaxVaidator(ILog log, Provider<XtextResourceSet> resourceSetProvider) {
      Preconditions.checkNotNull(log);
      Preconditions.checkNotNull(resourceSetProvider);
      this.log = log;
      this.resourceSetProvider = resourceSetProvider;
   }

   public String getValidHint(CodeMethod method, String sourceCode, int offset, String hintText, ICancellationToken cancellationToken) {
      int end = method.getEndOffest();
      int len = sourceCode.length();
      if (len == 0) {
         return hintText;
      } else {
         if (end >= len) {
            end = len - 1;
         }

         String code = sourceCode.substring(method.getStartOffest(), end);
         int validCodeSize = this.getValidHintSize(code, hintText, offset - method.getStartOffest(), cancellationToken);
         String validHintLines = hintText.substring(0, validCodeSize);
         if (hintText.length() != validHintLines.length()) {
            this.log.warning("Syntax check " + cancellationToken, () -> {
               StringBuilder message = new StringBuilder();
               message.append("Original hint: [");
               message.append(hintText);
               message.append(']');
               message.append(System.lineSeparator());
               message.append(System.lineSeparator());
               message.append("Valid hint:    [");
               message.append(validHintLines);
               message.append(']');
               message.append(System.lineSeparator());
               message.append(System.lineSeparator());
               message.append("Method: ");
               message.append(method.getUniqueName());
               return message.toString();
            });
         }

         return validHintLines;
      }
   }

   private int getValidHintSize(String code, String hint, int offset, ICancellationToken cancellationToken) {
      Optional<IParseResult> paseResult = this.parse(code, hint, offset);
      if (paseResult.isEmpty()) {
         return hint.length();
      } else {
         int errorOffset = this.getMinErrorOffset((IParseResult)paseResult.get(), cancellationToken) - offset;
         return errorOffset >= 0 && errorOffset <= hint.length() ? errorOffset : hint.length();
      }
   }

   private int getMinErrorOffset(IParseResult parseResult, ICancellationToken cancellationToken) {
      Iterable<INode> errors = parseResult.getSyntaxErrors();
      int minErrorOffset = -1;

      for(INode error : errors) {
         if (cancellationToken.isCanceled()) {
            break;
         }

         int offset = error.getOffset();
         if (minErrorOffset == -1 || offset < minErrorOffset) {
            minErrorOffset = offset;
         }
      }

      return minErrorOffset;
   }

   private Optional<IParseResult> parse(String code, String hint, int offset) {
      StringBuilder fullCode = new StringBuilder(code);
      if (fullCode.length() < offset) {
         return Optional.empty();
      } else {
         fullCode.insert(offset, hint);
         String fileExtension = ".bsl";
         if (fileExtension != null && !fileExtension.isBlank()) {
            InputStream codeStream = this.getAsStream(fullCode.toString());
            XtextResourceSet resourceSet = (XtextResourceSet)this.resourceSetProvider.get();
            URI uriToUse = computeUnusedUri(resourceSet, fileExtension);

            try {
               return this.parse(codeStream, uriToUse, PARSE_OPTIONS, resourceSet);
            } catch (Exception error) {
               this.log.logError(error);
            }
         }

         return Optional.empty();
      }
   }

   private static URI computeUnusedUri(ResourceSet resourceSet, String fileExtension) {
      for(int i = 0; i < Integer.MAX_VALUE; ++i) {
         URI syntheticUri = URI.createURI("__synthetic" + i + "." + fileExtension);
         if (resourceSet.getResource(syntheticUri, false) == null) {
            return syntheticUri;
         }
      }

      throw new IllegalStateException();
   }

   private Optional<Resource> createResource(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet) throws IOException {
      IResourceServiceProvider resourceServiceProvider = Registry.INSTANCE.getResourceServiceProvider(uriToUse);
      if (resourceServiceProvider == null) {
         return Optional.empty();
      } else {
         IResourceFactory resourceFactory = (IResourceFactory)resourceServiceProvider.get(IResourceFactory.class);
         if (resourceFactory == null) {
            return Optional.empty();
         } else {
            Resource resource = resourceFactory.createResource(uriToUse);
            if (resource == null) {
               return Optional.empty();
            } else {
               resourceSet.getResources().add(resource);
               resource.load(in, options);
               return Optional.of(resource);
            }
         }
      }
   }

   private Optional<IParseResult> parse(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet) throws IOException {
      return this.createResource(in, uriToUse, options, resourceSet).map((resource) -> resource instanceof XtextResource ? ((XtextResource)resource).getParseResult() : null);
   }

   private InputStream getAsStream(CharSequence text) {
      return new LazyStringInputStream(text == null ? "" : text.toString());
   }
}
