package com.e1c.edt.ai.context.tools;

import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.management.IDtHostResourceManager;
import com._1c.g5.v8.dt.md.IExternalPropertyManagerRegistry;
import com._1c.g5.v8.dt.search.core.BmObjectMatch;
import com._1c.g5.v8.dt.search.core.Match;
import com._1c.g5.v8.dt.search.core.SearchFor;
import com._1c.g5.v8.dt.search.core.SearchIn;
import com._1c.g5.v8.dt.search.core.SearchScope;
import com._1c.g5.v8.dt.search.core.SimpleSearchResultCollector;
import com._1c.g5.v8.dt.search.core.TextSearchScopeSettings;
import com._1c.g5.v8.dt.search.core.TextSearcher;
import com._1c.g5.v8.dt.search.core.refs.BmRelatedObjectMatch;
import com._1c.g5.v8.dt.search.core.refs.BslReferenceMatch;
import com._1c.g5.v8.dt.search.core.text.ITextSearchIndexProvider;
import com._1c.g5.v8.dt.search.core.text.TextSearchFileMatch;
import com._1c.g5.v8.dt.search.core.text.TextSearchModelMatch;
import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

public class FindMcpTool implements IMcpTool {
   public static final String TOOL_NAME = "1C_Find";
   private static final int DEFAULT_MAX_ELEMENTS = 16;
   private static String QuestionExample = "{\n  \"search_query\": \"*список?контрактов*\",\n  \"project_names\": [\"Проект_1\", \"Проект_2\"],\n  \"in\": [\n    \"metadata\",\n    \"attributes\",\n    \"forms\"\n  ],\n  \"for\": [\n    \"language_elements\",\n    \"comments\"\n  ],\n  \"scopes\": [\n    \"catalogs\",\n    \"documents\",\n    \"constants\"\n  ],\n  \"match_case\": true,\n  \"first_index\": 0,\n  \"max_count\": 64\n}";
   private static String AnswerExample = "[\n  {\n    \"type\": \"Text file\",\n    \"project_name\": \"MyProject\",\n    \"path\": \"C:/Projects/MyProject/src/CommonModule/Module.bsl\",\n    \"text_fragment\": \"Процедура ОбработкаПроведения(Отказ, Режим)\\n    Если Не Режим = РежимПроведения.Проведение Тогда\\n        Возврат;\\n    КонецЕсли;\",\n    \"fragment_offset\": 45,\n    \"match_length\": 8,\n    \"file_offset\": 234,\n    \"line_number\": 2\n  }\n]";
   private final IJson json;
   private final McpToolCallSpecification spec;
   private final IMcpToolsCallMessageFactory messageFactory;
   private final IBmModelManager searchModelManager;
   private final ITextSearchIndexProvider textSearchIndexProvider;
   private final IExternalPropertyManagerRegistry externalPropertyManagerRegistry;
   private final IDtHostResourceManager hostResourceManager;
   private final IBmModelManager projectModelManager;
   private final IMarkdownUtils markdownUtils;

   @Inject
   public FindMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IBmModelManager searchModelManager, ITextSearchIndexProvider textSearchIndexProvider, IExternalPropertyManagerRegistry externalPropertyManagerRegistry, IDtHostResourceManager hostResourceManager, IBmModelManager projectModelManager, IMarkdownUtils markdownUtils) {
      Preconditions.checkNotNull(json);
      Preconditions.checkNotNull(messageFactory);
      Preconditions.checkNotNull(searchModelManager);
      Preconditions.checkNotNull(textSearchIndexProvider);
      Preconditions.checkNotNull(externalPropertyManagerRegistry);
      Preconditions.checkNotNull(markdownUtils);
      Preconditions.checkNotNull(hostResourceManager);
      Preconditions.checkNotNull(projectModelManager);
      Preconditions.checkNotNull(markdownUtils);
      this.json = json;
      this.messageFactory = messageFactory;
      this.searchModelManager = searchModelManager;
      this.textSearchIndexProvider = textSearchIndexProvider;
      this.externalPropertyManagerRegistry = externalPropertyManagerRegistry;
      this.hostResourceManager = hostResourceManager;
      this.projectModelManager = projectModelManager;
      this.markdownUtils = markdownUtils;
      this.spec = createSpecification();
   }

   public boolean isExperimental() {
      return false;
   }

   public McpToolCallSpecification getSpecification() {
      return this.spec;
   }

   public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken) {
      ToolCallMessageDetails details = new ToolCallMessageDetails();
      details.autoCall = true;
      details.hideAfter = true;
      Optional<Request> optionalRequest = this.json.deserialize(call.function.arguments, Request.class);
      if (optionalRequest.isEmpty()) {
         throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
      } else {
         Request request = (Request)optionalRequest.get();
         TextSearchScopeSettings searchSettings = new TextSearchScopeSettings();
         if (request.searchQuery != null && !request.searchQuery.isBlank()) {
            if (request.projectNames != null && !request.projectNames.isEmpty()) {
               EnumConversionResult<SearchIn> searchInResult = convertEnums(request.searchIn, "in", SearchIn.class);
               if (!searchInResult.errorMessage.isEmpty()) {
                  throw new ToolException(searchInResult.errorMessage);
               } else {
                  EnumConversionResult<SearchFor> searchForResult = convertEnums(request.searchFor, "for", SearchFor.class);
                  if (!searchForResult.errorMessage.isEmpty()) {
                     throw new ToolException(searchForResult.errorMessage);
                  } else {
                     EnumConversionResult<SearchScope> searchScopesResult = convertEnums(request.searchScopes, "scopes", SearchScope.class);
                     if (!searchScopesResult.errorMessage.isEmpty()) {
                        throw new ToolException(searchScopesResult.errorMessage);
                     } else if (call.callKind == ToolCallKind.RENDER) {
                        details.requestMarkdown = Messages.Find1CObjectsTitle;
                        return CompletableFuture.completedFuture(this.messageFactory.createMessage(this, call, (String)null, details));
                     } else {
                        searchInResult.values.forEach((var1) -> searchSettings.addSearchIn(new SearchIn[]{var1}));
                        searchForResult.values.forEach((var1) -> searchSettings.addSearchFor(new SearchFor[]{var1}));
                        searchScopesResult.values.forEach((var1) -> searchSettings.addSearchScope(new SearchScope[]{var1}));
                        return CompletableFuture.supplyAsync(() -> {
                           if (cancellationToken.isCanceled()) {
                              throw new ToolException("Operation was cancelled before execution.");
                           } else {
                              try {
                                 IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                                 List<IProject> projects = new ArrayList();

                                 for(String projectName : request.projectNames) {
                                    IProject project = root.getProject(projectName);
                                    if (project == null || !project.exists()) {
                                       throw new ToolException("Project not found: " + projectName);
                                    }

                                    if (!project.isOpen()) {
                                       try {
                                          project.open(new NullProgressMonitor());
                                       } catch (CoreException error) {
                                          throw new ToolException("Cannot open project \"" + projectName + "\"", error, ToolErrorType.RETRYABLE);
                                       }
                                    }

                                    projects.add(project);
                                 }

                                 projects.forEach((var1) -> searchSettings.addProjects(new IProject[]{var1}));
                                 SimpleSearchResultCollector resultCollector = new SimpleSearchResultCollector();
                                 NullProgressMonitor monitor = new NullProgressMonitor();
                                 TextSearcher searcher = this.createTextSearcher(request.searchQuery, request.matchCase, searchSettings, resultCollector, this.searchModelManager, this.textSearchIndexProvider, this.externalPropertyManagerRegistry, this.hostResourceManager);
                                 searcher.search(monitor);
                                 if (cancellationToken.isCanceled()) {
                                    throw new ToolException("Operation was cancelled during search.");
                                 } else {
                                    List<Element> response = this.createResponse(resultCollector);
                                    int effectiveMaxElements = request.maxCount > 0 ? request.maxCount : 16;
                                    int firstIndex = Math.max(0, request.firstIndex);
                                    if (response.size() > firstIndex + effectiveMaxElements) {
                                       for(int i = firstIndex + effectiveMaxElements; i < response.size(); ++i) {
                                          response.remove(i);
                                       }

                                       for(int i = 0; i < firstIndex && i < response.size(); ++i) {
                                          response.remove(0);
                                       }
                                    } else if (firstIndex > 0 && response.size() > firstIndex) {
                                       for(int i = 0; i < firstIndex && i < response.size(); ++i) {
                                          response.remove(0);
                                       }
                                    }

                                    String content = this.json.serialize(response);
                                    int objectCount = response.size();
                                    String styledObjectCount = this.markdownUtils.createStyledText(String.valueOf(objectCount), TextColor.GREEN, FontWeight.BOLD, false);
                                    details.responseMarkdown = MessageFormat.format(Messages.Found1CObjectsTemplate, styledObjectCount);
                                    details.hideAfter = response.size() == 0;
                                    return this.messageFactory.createMessage(this, call, content, details);
                                 }
                              } catch (OperationCanceledException error) {
                                 throw new ToolException("Search failed", error, ToolErrorType.RETRYABLE);
                              } catch (CoreException error) {
                                 throw new ToolException("Search failed", error, ToolErrorType.RETRYABLE);
                              }
                           }
                        });
                     }
                  }
               }
            } else {
               throw new ToolException("At least one project must be specified in `project_names`.");
            }
         } else {
            throw new ToolException("`search_query` cannot be empty.");
         }
      }
   }

   private List<Element> createResponse(SimpleSearchResultCollector collector) {
      ArrayList<Element> elements = new ArrayList();

      for(Match match : collector.getMatches()) {
         IBmModel model = match.getModel();
         IProject project = this.projectModelManager.getProject(model);
         String projectName = project != null ? project.getName() : "Unknown";
         if (match instanceof TextSearchFileMatch) {
            TextSearchFileMatch src = (TextSearchFileMatch)match;
            TextSearchFile dst = new TextSearchFile();
            dst.type = "Text file";
            dst.projectName = projectName;
            IFile file = src.getFile();
            if (file != null) {
               IPath location = file.getRawLocation();
               if (location != null) {
                  dst.path = location.toOSString();
               }
            }

            dst.textFragment = src.getText();
            dst.fragmentOffset = src.getTextOffset();
            dst.matchLength = src.getTextLength();
            dst.fileOffset = src.getFileOffset();
            dst.lineNumber = src.getLineNumber();
            elements.add(dst);
         } else if (match instanceof TextSearchModelMatch) {
            TextSearchModelMatch src = (TextSearchModelMatch)match;
            TextSearchModelElement dst = new TextSearchModelElement();
            dst.type = "Text in 1C model";
            dst.projectName = projectName;
            dst.propertyValue = src.getText();
            dst.topObjectId = src.getTopObjectId();
            dst.objectId = src.getObjectId();
            dst.valueOffset = src.getTextOffset();
            dst.matchLength = src.getTextLength();
            elements.add(dst);
         } else if (match instanceof BslReferenceMatch) {
            BslReferenceMatch src = (BslReferenceMatch)match;
            BslReferenceElement dst = new BslReferenceElement();
            dst.type = "Reference to 1C code";
            dst.projectName = projectName;
            Optional<BmObjectMatch> optionalTarget = src.getTarget();
            if (optionalTarget.isPresent()) {
               BmObjectMatch target = (BmObjectMatch)optionalTarget.get();
               dst.targetMetadataTopObjectId = target.getMetadataTopObjectId();
               dst.targetObjectId = target.getObjectId();
            }

            elements.add(dst);
         } else if (match instanceof BmRelatedObjectMatch) {
            BmRelatedObjectMatch src = (BmRelatedObjectMatch)match;
            BmRelatedObjectElement dst = new BmRelatedObjectElement();
            dst.type = "1C related object";
            dst.projectName = projectName;
            dst.objectId = src.getObjectId();
            Optional<BmObjectMatch> optionalTarget = src.getTarget();
            if (optionalTarget.isPresent()) {
               BmObjectMatch target = (BmObjectMatch)optionalTarget.get();
               dst.targetMetadataTopObjectId = target.getMetadataTopObjectId();
               dst.targetObjectId = target.getObjectId();
            }

            elements.add(dst);
         } else if (match instanceof BmObjectMatch) {
            BmObjectMatch src = (BmObjectMatch)match;
            BmObjectElement dst = new BmObjectElement();
            dst.type = "1C object";
            dst.projectName = projectName;
            dst.objectId = src.getObjectId();
            elements.add(dst);
         }
      }

      return elements;
   }

   private TextSearcher createTextSearcher(String searchQuery, boolean matchCase, TextSearchScopeSettings searchSettings, SimpleSearchResultCollector resultCollector, IBmModelManager searchModelManager, ITextSearchIndexProvider textSearchIndexProvider, IExternalPropertyManagerRegistry externalPropertyManagerRegistry, IDtHostResourceManager hostResourceManager) {
      Constructor<TextSearcher> constructor = null;
      Object[] args = null;

      Constructor[] var14;
      for(Constructor<?> ctor : var14 = TextSearcher.class.getConstructors()) {
         switch (ctor.getParameterCount()) {
            case 7:
               constructor = ctor;
               args = new Object[]{searchQuery, searchSettings, resultCollector, searchModelManager, textSearchIndexProvider, externalPropertyManagerRegistry, hostResourceManager};
               break;
            case 8:
               constructor = ctor;
               args = new Object[]{searchQuery, matchCase, searchSettings, resultCollector, searchModelManager, textSearchIndexProvider, externalPropertyManagerRegistry, hostResourceManager};
         }
      }

      if (constructor == null) {
         throw new ToolException("Search failed. Please try again later.", ToolErrorType.RETRYABLE);
      } else {
         try {
            return (TextSearcher)constructor.newInstance(args);
         } catch (ReflectiveOperationException e) {
            throw new ToolException("Search failed. Please try again later.", e, ToolErrorType.RETRYABLE);
         }
      }
   }

   private static McpToolCallSpecification createSpecification() {
      McpToolCallSpecification spec = new McpToolCallSpecification();
      spec.type = "function";
      spec.function = new McpToolCallFunction();
      spec.function.name = "1C_Find";
      StringBuilder description = new StringBuilder();
      description.append("Finds 1C project elements (objects, attributes, forms, code, etc.).");
      description.append("\n\nUsage:");
      description.append("\n- Arguments must be a single JSON object.");
      description.append("\n- Use wildcards in `search_query` for broad search.");
      description.append("\n- Narrow by project and type to reduce noise.");
      description.append("\n\nRelated tools:");
      description.append("\n- Fetch by id: `1C_GetObject`.");
      description.append("\n\nExample:");
      description.append("\n  Q: ");
      description.append(QuestionExample);
      description.append("\n  A: ");
      description.append(AnswerExample);
      spec.function.description = description.toString();
      McpToolCallParameters parameters = new McpToolCallParameters();
      parameters.type = "object";
      HashMap<String, McpToolCallProperty> properties = new HashMap();
      McpToolCallProperty searchQueryProp = new McpToolCallProperty();
      searchQueryProp.type = "string";
      searchQueryProp.description = "Search query. Wildcards are supported.";
      properties.put("search_query", searchQueryProp);
      McpToolCallProperty projectNamesProp = new McpToolCallProperty();
      projectNamesProp.type = "array";
      projectNamesProp.description = "1C project names as a JSON array of strings.";
      properties.put("project_names", projectNamesProp);
      McpToolCallProperty searchInProp = new McpToolCallProperty();
      searchInProp.type = "array";
      searchInProp.description = "Where to search - elements as a JSON array of strings. Valid values: " + getEnumNames(SearchIn.class) + ".";
      properties.put("in", searchInProp);
      McpToolCallProperty searchForProp = new McpToolCallProperty();
      searchForProp.type = "array";
      searchForProp.description = "What to search for - elements as a JSON array of strings. Valid values: " + getEnumNames(SearchFor.class) + ".";
      properties.put("for", searchForProp);
      McpToolCallProperty searchScopesProp = new McpToolCallProperty();
      searchScopesProp.type = "array";
      searchScopesProp.description = "The scope of the search - elements as a JSON array of strings. Valid values: " + getEnumNames(SearchScope.class) + ".";
      properties.put("scopes", searchScopesProp);
      McpToolCallProperty matchCaseProp = new McpToolCallProperty();
      matchCaseProp.type = "boolean";
      matchCaseProp.description = "Case-sensitive search. Default: false";
      properties.put("match_case", matchCaseProp);
      McpToolCallProperty firstIndexProp = new McpToolCallProperty();
      firstIndexProp.type = "integer";
      firstIndexProp.description = "Index of first element to return (0-based). Default: 0";
      properties.put("first_index", firstIndexProp);
      McpToolCallProperty maxCountProp = new McpToolCallProperty();
      maxCountProp.type = "integer";
      maxCountProp.description = "Maximum number of elements to return. Default: 64";
      properties.put("max_count", maxCountProp);
      parameters.properties = properties;
      parameters.required = Arrays.asList("search_query", "project_names", "in", "for", "scopes");
      spec.function.parameters = parameters;
      return spec;
   }

   private static <TEnum extends Enum<TEnum>> EnumConversionResult<TEnum> convertEnums(List<String> source, String paramName, Class<TEnum> targetType) {
      if (source != null && !source.isEmpty()) {
         ArrayList<TEnum> convertedValues = new ArrayList();
         ArrayList<String> invalidValues = new ArrayList();

         for(String value : source) {
            Optional<TEnum> converted = convertStringToEnum(value, targetType);
            if (converted.isPresent()) {
               convertedValues.add((Enum)converted.get());
            } else {
               invalidValues.add(value);
            }
         }

         if (!invalidValues.isEmpty()) {
            String errorMsg = String.format("Invalid values for parameter `%s`: %s. Valid values: %s.", paramName, String.join(", ", invalidValues), getEnumNames(targetType));
            return new EnumConversionResult<TEnum>(Collections.emptyList(), errorMsg);
         } else {
            return new EnumConversionResult<TEnum>(convertedValues, "");
         }
      } else {
         return new EnumConversionResult<TEnum>(Collections.emptyList(), "");
      }
   }

   private static <E extends Enum<E>> Optional<E> convertStringToEnum(String source, Class<E> targetType) {
      if (source == null) {
         return Optional.empty();
      } else {
         try {
            return Optional.of(Enum.valueOf(targetType, source.toUpperCase()));
         } catch (IllegalArgumentException var3) {
            return Optional.empty();
         }
      }
   }

   private static <TEnum extends Enum<TEnum>> String getEnumNames(Class<TEnum> targetType) {
      return (String)Arrays.stream((Enum[])targetType.getEnumConstants()).map(Enum::name).map(String::toLowerCase).collect(Collectors.joining(", "));
   }

   private static class EnumConversionResult<T> {
      public final List<T> values;
      public final String errorMessage;

      public EnumConversionResult(List<T> values, String errorMessage) {
         this.values = values;
         this.errorMessage = errorMessage;
      }
   }

   private static class Request {
      @SerializedName("search_query")
      public String searchQuery;
      @SerializedName("project_names")
      public List<String> projectNames;
      @SerializedName("in")
      public List<String> searchIn;
      @SerializedName("for")
      public List<String> searchFor;
      @SerializedName("scopes")
      public List<String> searchScopes;
      @SerializedName("match_case")
      public boolean matchCase = false;
      @SerializedName("first_index")
      public int firstIndex = 0;
      @SerializedName("max_count")
      public int maxCount = 16;
   }

   private static class Element {
      @SerializedName("type")
      public String type;
      @SerializedName("project_name")
      public String projectName;
   }

   private static class TextSearchFile extends Element {
      @SerializedName("path")
      public String path;
      @SerializedName("text_fragment")
      public String textFragment;
      @SerializedName("fragment_offset")
      public int fragmentOffset;
      @SerializedName("match_length")
      public int matchLength;
      @SerializedName("file_offset")
      public int fileOffset;
      @SerializedName("line_number")
      public long lineNumber;
   }

   private static class TextSearchModelElement extends Element {
      @SerializedName("property_value")
      public String propertyValue;
      @SerializedName("top_object_id")
      public long topObjectId;
      @SerializedName("object_id")
      public long objectId;
      @SerializedName("value_offset")
      public int valueOffset;
      @SerializedName("match_length")
      public int matchLength;
   }

   private static class BslReferenceElement extends Element {
      @SerializedName("target_top_object_id")
      public long targetMetadataTopObjectId;
      @SerializedName("target_object_id")
      public long targetObjectId;
   }

   private static class BmRelatedObjectElement extends Element {
      @SerializedName("object_id")
      public long objectId;
      @SerializedName("target_top_object_id")
      public long targetMetadataTopObjectId;
      @SerializedName("target_object_id")
      public long targetObjectId;
   }

   private static class BmObjectElement extends Element {
      @SerializedName("object_id")
      public long objectId;
   }
}
