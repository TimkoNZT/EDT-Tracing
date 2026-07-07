# EDT AI Plugin (декомпилированные исходники)

Декомпиляция 3 JAR-ов EDT AI (v1.0.4) через IntelliJ Fernflower.
Четвёртый бандл `com.e1c.edt.ai` (core) — НЕ декомпилирован (нет в plugins/).

## Bundle structure

| Bundle | SNA | Экспорт | Роль |
|--------|-----|---------|------|
| `ai.ui` | `com.e1c.edt.ai.ui` | (none) | Entry point, Activator, ChatView, handlers |
| `ai.ui.common` | `com.e1c.edt.ai.ui.common` | `.ui`, `.handlers`, `.preferences`, `.quickaccess`, `.tools` | Chat/Dispatcher/Web, MCP tools, UI services |
| `ai.context` | `com.e1c.edt.ai.context` | `.context`, `.DTO`, `.tools` | Configuration model, entities, metadata walking |
| *(core)* | `com.e1c.edt.ai` | *(не декомпилирован)* | `ISettings`, `IMcpTools`, `ISessionService`, `ILog`, `IJson` etc. |

## DI: Guice, не OSGi

Плагин использует **Google Guice**, не Declarative Services. Инжектор создаётся в `Activator`:

```
ContextModuleFactory.create(this)     // ai.context bindings
  .with(AIUICommonModule())           // ai.ui.common bindings
  .with(AIUIModule(this))             // ai.ui bindings
```

Multibinders:
- `IInitializable` set — стартуют при инициализации: `UI`, `ContextMenuInterceptor`, `ClipboardManager`, `DialogsEnhancer`, `ResourceListener`, `UpdateService`, `Notificator`, `ActiveProjectTracker`
- `IViewEnhancer` set — `StagingViewEnhancer`
- `IMcpTool` set — 23 MCP-инструмента
- `IMarkersProvider`, `IJShellBindingProvider`, `IReplacementStrategy`, `IProjectDetailsProvider`

## Chat/AI communication flow — WebView bridge

**HTTP-клиента в Java нет.** Вся связь с AI-бэкендом — через JavaFX WebView:

```
Пользователь → ExplainAIHandler
  → IChat.explainCode(AIContext, codeSnippet)
    → Chat.java:
      1. ui.showView("com.e1c.edt.ai.ui.views.ChatView")
      2. Создаёт Job через IDispatcher.createJob()
      3. В Job:
         a. settings.getChatUrl() — URL из настроек
         b. settings.getClientToken() — токен
         c. Если URL/токен изменился:
            - WebEngine.load(chatUrl) — загружает HTML-страницу чата
            - Ждёт загрузки
         d. wink(): window.ideApi = this.handler (IdeApiHandler)
            window.chatApi.wink({client_id, client_uid}, language, theme)
            window.chatApi.set_tools(toolsJson) — MCP-спецификации
         e. window.chatApi.explain_code(...) — конкретный вызов
    → JavaScript в WebView делает HTTP-запросы к AI-бэкенду
    → Ответ приходит через window.ideApi.*:
       - paste_code(code) — вставка кода в редактор
       - callTools(chatId, msgId, callToolsJson) — запрос MCP-инструментов
       - renderTools(...) — превью результата MCP
       - link(title, href) — открыть файл
       - trace(message) — отладочный лог
```

**Session flow:**
```
Chat.getSessionId(AIContext)
  1. ProjectId из контекста (или Default)
  2. ISessionService.getSessionAsync(projectId).get()
  3. → sessionId (строка)
```
`ISessionService` — в core-бандле (не декомпилирован), вероятно делает HTTP-запрос к AI-бэкенду для аутентификации.

## Интерфейсы по бандлам

### ai.ui.common (экспортируется)

| Interface | Package | Key methods |
|-----------|---------|-------------|
| `IChat` | `com.e1c.edt.ai.ui` | `reviewCode()`, `explainCode()`, `fixCode()`, `generateDocComments()`, `askQuestion()`, `addCode()`, `addFiles()`, `addToolsResult()`, `continueChat()` |
| `IChatDialog` | `.ui` | `show(ScrollPane)`, `hide()` |
| `IDispatcher` | `.ui` | `dispatch(Supplier/Runnable)`, `dispatchAsync(Runnable)`, `createJob()`, `checkThread()` |
| `IUI` | `.ui` | `getShell()`, `getTextWidget()`, `getLastSourceViewer()`, `getSourceViewer()`, `getFile()`, `showView()` |
| `IWeb` | `.ui` | `browse(String url)` |
| `IAIContextProvider` | `.ui` | `create(SourceViewer, AITarget, ICancellationToken)` |
| `ICodeTools` | `.ui.handlers` | `hasTarget(CodeAction)`, `createContextForTarget()`, `getTargetMethod()`, `selectMethodComment()` |
| `IFixDialog` | `.ui.handlers` | `show()`, `getDetails()` |
| `IEdtLinkHandler` | `.ui` | `formatInsertCodePath()`, `getFullPathForInsertCode()`, `extractFilePath()`, `extractCursorPosition()`, `extractSelection()` |
| `IPreferences` | `.ui` | `show(String)` |
| `IInitializable` | `.ui` | `initialize()` |
| `IViewEnhancer` | `.ui` | `getViewId()`, `setup(IWorkbenchPart)` |
| `IThemeManager` | `.ui` | — |

### ai.context (экспортируется)

| Interface | Package | Key methods |
|-----------|---------|-------------|
| `IDispatcher` | `.context` | `dispatch(Supplier<T>, Duration)` — executor-based |
| `IV8Model` | `.context` | Доступ к V8-модели EDT |
| `IModuleProvider` | `.context` | — |
| `IEntityInfo` | `.context` | implements `IContextEntities` |
| `IEntityFactory` | `.context` | — |
| `IBmPovider` | `.context` | — |
| `IBmObjectProvider` | `.context` | — |
| `IRelatedEntities` | `.context` | — |
| `IEntityVisitor` | `.context` | — |
| `IFormWalker` | `.context` | — |
| `IFormVisitor` | `.context` | — |
| `IEntitiesWalker` | `.context` | — |
| `ICommentFactory` | `.context` | — |
| `IIdFactory` | `.context` | — |
| `IMethodListProvider` | `.context.tools` | — |

### Core `com.e1c.edt.ai` (НЕ декомпилирован, inferred from imports)

| Interface | Key methods (inferred) |
|-----------|-----------------------|
| `ISettings` | `getChatUrl()`, `getClientToken()`, `getClientUniqueId()`, `getLanguage()`, `getTheme()`, `getTimeout()`, `getMinRequestDelay()`, `isEnabled()`, `hasClientToken()`, `getVerbosity()`, `getCodeCompletionPolicy()`, `getUrl()`, `getHomePage()`, `getUpdateUrl()` |
| `ISettingsSetter` | `applySessionParameters()`, `setCodeCompletionPolicy()` |
| `ISettingsStore` | `getString()`, `setString()`, `getInt()` |
| `ILog` | `trace()`, `warning()`, `logError()` |
| `IJson` | `serialize()`, `deserialize()`, `formatJson()` |
| `IMcpTools` | `getSpecifications()`, `callTools()` |
| `IMcpTool` | `getSpecification()`, `call(McpToolCall, ICancellationToken)` |
| `IMcpToolsCallMessageFactory` | `createMessage()`, `createError()`, `createRawMessage()` |
| `ISessionService` | `getSessionAsync(ProjectId)` → sessionId |
| `IStateService` | `addListener()`, `busy()`, фабрика `ICancellationToken` |
| `IAssistantStateService` | События смены состояния (`ServiceState`, `ActionState`) |
| `ICancellationToken` | `isCanceled()` |
| `IContextEntities` | `fill(AIContext, ChatContext, ...)` |
| `ILocalContext` | `create(AIContext, ...)` |
| `IProjectTools` | `determineProjectName()`, `getProjectFile()` |
| `IContentSourceProvider` | `getFileDocument(IFile)` |
| `IMarkdownUtils` | `formatFilePath()`, `createStyledText()`, `decodeUrl()` |
| `IHashTools` | — |
| `IFileDocument` | `getDocument()`, `getCharset()`, `getFile()` |
| `IProjectDetailsProvider` | — |
| `IMarkersProvider` | — |

## MCP Tools (23 шт.)

Multibinder `IMcpTool` — Guice:

| Tool | Class | Назначение |
|------|-------|------------|
| `Read` | `ReadMcpTool` | Чтение файла |
| `Write` | `WriteMcpTool` | Запись файла |
| `Edit` | `EditMcpTool` | Редактирование (targeted edits) |
| `Delete` | `DeleteMcpTool` | Удаление файла |
| `Find` | `FindMcpTool` | Поиск файлов |
| `Glob` | `GlobMcpTool` | Glob-поиск |
| `List` | `ListMcpTool` | Содержимое каталога |
| `SearchFiles` | `SearchFilesMcpTool` | Поиск файлов по имени |
| `SearchText` | `SearchTextMcpTool` | Поиск текста в workspace |
| `GetMarkers` | `GetMarkersMcpTool` | Проблемы (маркеры) |
| `SetMarkers` | `SetMarkersMcpTool` | Установка маркеров |
| `DeleteMarkers` | `DeleteMarkersMcpTool` | Удаление маркеров |
| `GetProjects` | `GetProjectsMcpTool` | Список проектов |
| `GetCommands` | `GetCommandsMcpTool` | Команды EDT |
| `GetCommandCategories` | `GetCommandCategoriesMcpTool` | Категории команд |
| `ExecuteCommand` | `ExecuteCommandMcpTool` | Выполнение команды EDT |
| `Execute` | `ExecuteMcpTool` | Выполнение OS-команды |
| `Git` | `GitMcpTool` | Git-операции |
| `GitDiff` | `GitDiffMcpTool` | Git diff |
| `GitCommits` | `GitCommitsMcpTool` | Git commit history |
| `LocalChanges` | `LocalChangesMcpTool` | Локальные изменения |
| `LocalHistory` | `LocalHistoryMcpTool` | Локальная история |
| `NavigationHistory` | `NavigationHistoryMcpTool` | История навигации |
| `JShell` | `JShellMcpTool` | Выполнение Java-кода |
| `JShellSession` | `JShellSessionMcpTool` | Управление JShell-сессией |
| `GetObject` | `GetObjectMcpTool` (ai.context) | Метаданные 1С |

```java
public interface IMcpTool {
    McpToolCallSpecification getSpecification();
    CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken);
}
```

## Extension points (plugin.xml)

| Extension point | Что регистрирует |
|----------------|------------------|
| `org.eclipse.ui.views` | **ChatView** (`com.e1c.edt.ai.ui.views.ChatView`, icon `icons/obj16/ai.png`) |
| `org.eclipse.ui.perspectiveExtensions` | ChatView stacked with PropertySheet |
| `org.eclipse.ui.quickAccess` | `AskAIQuickAccessComputer` |
| `org.eclipse.ui.commands` | 25 команд (explain, criticise, fixcode, addcode…) |
| `org.eclipse.ui.bindings` | Ctrl+Alt+Space, Alt+I E/R/C/G/A/F/B… |
| `org.eclipse.ui.handlers` | 15 handler-классов |
| `org.eclipse.ui.menus` | Контекстное меню, главное меню AI, статус-бар, toolbar |
| `org.eclipse.ui.startup` | `PluginStartup` |
| `org.eclipse.ui.preferencePages` | `ClientAIPreferencePage` |
| `org.eclipse.core.runtime.preferences` | `ClientAIPreferencePageInitializer` |
| `org.eclipse.core.resources.markers` | 3 marker type: `AIError`, `AIWarning`, `AIInfo` |
| `org.eclipse.xtext.builder.participant` | `BuildTrackingParticipant` |
| `org.eclipse.ui.ide.markerResolution` | `AIMarkerResolutionGenerator` |
| `org.eclipse.e4.ui.css.swt.theme` | Dark theme stylesheet |

## Settings

Хранятся в `IPreferenceStore` (Eclipse preferences):
- `stringPreferenceClientID` — токен / API key
- `stringPreferenceLLMParameters` — JSON c `url`, `chatUrl`, `timeout`, `minDelay`
- `stringPreferenceCodeCompletionPolicy` — политика автодополнения
- `stringPreferenceLanguage` — язык (ru/en)
- `stringPreferenceCodeCompletionLinesCount`

Chat URL по умолчанию: `<baseUrl>/chat/` (из параметров).

## Протокол Java↔JS bridge (WebView)

### URL загрузки

- **Default**: `https://code.1c.ai/chat/` (из `DefaultSettings.java`)
- Кастомизируется через preference `stringPreferenceLLMParameters` → JSON с полем `chatUrl`
- Fallback: `new URL(baseUrl, "chat/")`

### Java → JS (window.chatApi.*)

| Java-метод | topic в JS | Параметры |
|-----------|------------|-----------|
| `explainCode()` | `comment_code` | `(subject, scriptLang, progLang, path, sessionId, title, contextJson)` |
| `reviewCode()` | `review_code` | same |
| `fixCode()` | `fix_code` | `(subject, scriptLang, progLang, details, path, sessionId, title, contextJson)` |
| `generateDocComments()` | `document_code` | same as explain |
| `askQuestion()` | `plain_message` | same |
| `addCode()` | `insert_code` | same + startLine, endLine |
| инициализация | `wink` | `({client_id, client_uid}, language, theme)` |
| инициализация | `set_tools` | `(toolsJson)` — MCP-спецификации |
| результат MCP | `add_tool_calls_result` | `(chatId, messageId, callsMessages, unknownMessages)` |

### JS → Java (window.ideApi.*)

| JS-вызов | Java-метод | Действие |
|---------|-----------|----------|
| `ideApi.wink(param)` | `IdeApiHandler.wink()` | Подтверждение инициализации |
| `ideApi.paste_code(code)` | `IdeApiHandler.paste_code()` | Вставка кода в редактор |
| `ideApi.callTools(chatId, msgId, json)` | `IdeApiHandler.callTools()` | Вызов MCP-инструментов (асинхронно) |
| `ideApi.renderTools(chatId, msgId, json)` | `IdeApiHandler.renderTools()` | Вызов MCP-инструментов (синхронно, preview) |
| `ideApi.link(title, href)` | `IdeApiHandler.link()` | Открыть файл/URL |
| `ideApi.trace(message)` | `IdeApiHandler.trace()` | Логирование |

### Полный flow

```
1. ChatView открывается → BaseChatView.createPartControl()
2. Chat.show(ScrollPane) → создаёт WebView → грузит loading.html
3. Chat.chat(topic, subject, details, ctx):
   a. settings.getChatUrl() → "https://code.1c.ai/chat/"
   b. webEngine.load(chatUrl) → ждёт SUCCEEDED
   c. wink():
      - window.ideApi = handler (IdeApiHandler)
      - window.chatApi.wink({client_id, client_uid}, language, theme)
      - JS → ideApi.wink(param) → isReady = true
      - window.chatApi.set_tools(toolsJson) — MCP-спецификации
   d. window.chatApi.{topic}(subject, lang, progLang, ..., sessionId, title, contextJson)
   e. JS делает HTTP-запросы к AI-бэкенду (URL неизвестен — внутри JS-кода code.1c.ai)
   f. JS отвечает через ideApi.paste_code() / ideApi.callTools() / ideApi.link()
4. MCP результаты возвращаются:
   window.chatApi.add_tool_calls_result(chatId, messageId, callsMessages, unknownMessages)
```

## Как повторить обращение к API

**Проблема**: HTTP-эндпоинты, куда JS отправляет запросы, находятся внутри JavaScript-кода, загружаемого с `https://code.1c.ai/chat/`. В декомпилированных Java-исходниках их нет.

**Варианты**:
1. **Проинспектировать JavaScript** — загрузить `https://code.1c.ai/chat/` в браузере/curl, посмотреть какие JS-файлы грузятся, найти в них URL эндпоинтов и формат запросов
2. **Перехватить трафик WebView** — запустить EDT с прокси (mitmproxy/Fiddler), посмотреть какие HTTP-запросы делает WebView при вызове explain/review и т.д.
3. **Напрямую вызвать JS bridge из своего плагина** — если мы загрузим ту же HTML-страницу в WebView и вызовем `window.chatApi.*`, JS выполнит HTTP-запросы сам, а ответы придут в `ideApi.*` (нужно поднять свой IdeApiHandler)
4. **ISessionService** (core, не декомпилирован) — может делать HTTP-запрос напрямую, но код недоступен

## Где находятся ключевые файлы

| Файл | Путь |
|------|------|
| Chat.java | `ai.ui.common/com/e1c/edt/ai/ui/Chat.java` |
| IdeApiHandler.java | `ai.ui.common/com/e1c/edt/ai/ui/IdeApiHandler.java` |
| Settings.java | `ai.ui.common/com/e1c/edt/ai/ui/Settings.java` |
| BaseChatView.java | `ai.ui.common/com/e1c/edt/ai/ui/BaseChatView.java` |
| DefaultSettings.java | `ai.ui/com/e1c/edt/ai/ui/DefaultSettings.java` |
| ClientAIPreferencePage.java | `ai.ui.common/.../preferences/ClientAIPreferencePage.java` |
| JavaScript.java (escapes) | `ai.ui.common/com/e1c/edt/ai/ui/JavaScript.java` |
| plugin.xml | `ai.ui/plugin.xml` |
