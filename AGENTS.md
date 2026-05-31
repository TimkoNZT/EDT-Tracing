**AGENTS.md** — Накапливай новые знания здесь, стирай устаревшее.

- **AGENTS.md rule**: В процессе работы собирай все важные находки (архитектура, баги, команды, подводные камни) в этот файл. Стирай устаревшее. Этот пункт — тоже правило.
- **Logs rule**: Никогда не спрашивать у пользователя «где логи» или «покажи логи». Всегда смотреть `D:\EDT\Workspace\.metadata\.log` самостоятельно.

---

## Структура проекта

```
D:\EDT\EDT_tracing/
├── tracing_plugin/         - основной плагин (src, build)
├── profiling/
│   ├── core/               - декомпилированные классы profiling.core (НЕ ИСПОЛЬЗУЕТСЯ)
│   ├── ui/                 - декомпилированные классы profiling.ui (НЕ ИСПОЛЬЗУЕТСЯ)
│   ├── perfinfo_extract/   - debug.model (EMF) + .xcore модели (НЕ ИСПОЛЬЗУЕТСЯ)
│   ├── _decompiled/        - декомпилированные классы EDT для справки (IBslStackFrame, BslSourceDisplay и др.)
│   └── _extracted/         - бинарные артефакты из JAR (НЕ ИСПОЛЬЗУЕТСЯ)
├── dist/                   - P2 репозиторий
├── AGENTS.md
├── build-minimal.ps1
└── .gitignore
```

## Build (javac, no Maven)

- **EDT Tracing**: `cd tracing_plugin && .\build-javac.ps1`
  - Авто-определяет EDT, компилирует `src/`, пакует JAR.
  - Итог: P2-репозиторий + ZIP в `tracing_plugin/dist/`.

- **Deploy (quick copy)**: после сборки скопировать JAR в кэш EDT для быстрого тестирования (без переустановки через P2):
  ```
  Copy-Item -LiteralPath "D:\EDT\EDT_tracing\tracing_plugin\dist\p2repo\plugins\com._1c.g5.v8.dt.tracing.ui_1.0.0.jar" `
    -Destination "C:\Users\Тимур\.eclipse\org.eclipse.platform_4.30.0_233488020_win32_win32_x86_64\plugins\" -Force
  ```
  (путь может отличаться для другой версии EDT — находится через `eclipse.p2.data.area` в `configuration/config.ini` EDT-установки + смотрим `plugins/` в конфигурационной локации Eclipse, куда P2 ставит плагины)

- **Важно: `plugin.xml` должен быть в корне модуля** (не внутри `META-INF/`!), иначе Eclipse не видит extension-точки. Build-скрипты копируют `plugin.xml` в корень JAR отдельно от `META-INF/`.

- **Feature обязателен** — без него p2 не показывает плагин в Install New Software.

---

## Плагин EDT Tracing (step-tracing approach)

- **Цель**: позапросный трейсинг через RDBG-степпинг. Вместо агрегированного профилировщика 1С (который возвращает только частоты/длительности, а не последовательность вызовов) — пошаговая трассировка через suspend + stepOver всех debug-таргетов.
- Plugin ID: `com._1c.g5.v8.dt.tracing.ui`
- View ID: `com._1c.g5.v8.dt.tracing.ui.TraceView`
- View class: `com._1c.g5.v8.dt.internal.tracing.ui.view.TraceView` (extends ViewPart)
- Command ID: `com._1c.g5.v8.dt.tracing.ui.commands.toggleTracing`
- Toolbar: `toolbar:org.eclipse.debug.ui.breakpointActionSet` (Debug perspective)
- Activation: `Bundle-ActivationPolicy: lazy`
- Java: compile with `--release 8` (target JavaSE-1.8)

### Как работает (build 035+)

1. **Toggle ON**: собираем все `IDebugTarget` из `ILaunchManager.getDebugTargets()`, фильтр по `instanceof ISuspendResume`
2. **Async-suspend** каждого не-suspended таргета в daemon Thread (не блокируем UI/event thread)
3. Регистрируемся как `IDebugEventSetListener` **только на CREATE/TERMINATE** (НЕ на SUSPEND)
4. Устанавливаем `IDebugEventFilter`, выкидывающий SUSPEND-события (чтобы Debug View не открывал окна)
5. **Poll loop** на background-daemon thread (100ms):
   - Проверяем ВСЕ таргеты: если suspended и frame изменился → записываем
   - Ищем steppable thread round-robin → `stepInto()`/`stepOver()` в daemon Thread
6. **CREATE/TERMINATE** события обрабатываем отдельно (не через poll) для мгновенной реакции на появление/удаление таргетов
7. **Toggle OFF**: `tracingActive=false` → resume всех через daemon Thread → clean state (удаляем фильтр)

### Ключевые классы (Eclipse Debug API)

- `IDebugTarget`, `ISuspendResume`, `IStep`, `IThread` — `org.eclipse.debug.core.model`
- `DebugPlugin.getDefault().addDebugEventListener()` / `removeDebugEventListener()`
- `IStackFrame.getName()` — в EDT возвращает "ModuleName.MethodName"
- `IStackFrame.getLineNumber()` — строка в модуле

### Почему не profiling

- **Профилировщик (IProfilingService / IProfilingResultListener)**: данные агрегируются внутри 1С runtime (1cv8.exe), возвращается `PerformanceInfoMain` с частотами/длительностями строк
- **Per-call events не существуют** ни в RDBG-протоколе, ни в DBGUIExtCmds (15 типов, нет per-call)
- Step-трассировка — единственный способ получить последовательность вызовов без модификации кода

## Декомпиляция JAR (IntelliJ Fernflower)

- Инструмент: `java-decompiler.jar` из IntelliJ IDEA (например, `C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\java-decompiler\lib\java-decompiler.jar`)
- Команда:
  ```
  & "c:\Program Files\BellSoft\LibericaJDK-21\bin\java.exe" -cp "c:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\java-decompiler\lib\java-decompiler.jar" org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler -dgs=true <input.jar> "<output_dir>"
  ```
- Результат: один `.java` файл на каждый `.class` (в той же структуре каталогов, что и JAR).
- Декомпилированные классы EDT хранить в `profiling/_decompiled/<bundle>/`.

## Подводные камни OSGi (Require-Bundle vs Import-Package)

- `com._1c.g5.v8.dt.debug.model` НЕ экспортирует свои внутренние пакеты (`base.data`, `measure`):
  ```
  Export-Package: com._1c.g5.v8.dt.debug.model.area;version="2.0.0"
  ```
  Только `area` экспортируется. Пакеты `base.data` (где `DebugTargetType`) и `measure` (где `PerformanceInfoMain`, `PerformanceInfoModule`) — внутренние.

- **Решение**: `Require-Bundle: com._1c.g5.v8.dt.debug.model` — `Require-Bundle` даёт доступ ко всем пакетам зависимого бандла, включая неэкспортированные.

- **Важно**: Для step-tracing НЕ нужны profiling/core и debug.model EMF-классы. Достаточно стандартного Eclipse Debug API.

## Workspace

- EDT workspace: `D:\EDT\Workspace`
- `.metadata/.log` — основной лог ошибок EDT.

---

## Класс для компиляции (~28 JAR)

- `org.eclipse.swt.win32.win32.x86_64_*.jar` — обязателен (основной SWT пустой).
- `com.google.guava_32.1.3.jre` (не 15.x!).
- `com.google.guava.failureaccess_*`.
- `org.eclipse.emf.common_*` — транзитив для debug.model (может не понадобиться).

---

## Иконки

- Toolbar-иконки: PNG, 16x16 пикселей.
- Расположение: `tracing_plugin/com._1c.g5.v8.dt.tracing.ui/icons/tracing.png`.
- Ссылка в `plugin.xml`: `icon="icons/tracing.png"`.
- Build-скрипт копирует `icons/` из модуля в корень JAR.

## Локализация колонок (NLS)

- Используем `org.eclipse.osgi.util.NLS`.
- `Messages.java` + `messages.properties` / `messages_ru.properties` в пакете view.
- Строки для `plugin.xml` (%variable) — в `META-INF/messages*.properties`.

---

## Подводные камни

- **plugin.xml в META-INF/**: Eclipse не видит extension-точки. Файл должен быть в корне JAR.
- **Версионные диапазоны**: Слишком узкие диапазоны (`[1.7.0,2.0.0)`) блокируют установку. Использовать `[X.Y.Z,10.0.0)` или без версии.
- **Export-Package**: Обязателен для e4-фрагментов (иначе класс view недоступен). Для legacy view не обязателен.
- **Bundle-Activator**: Должен быть конкретный класс, не абстрактный (`AbstractUIPlugin` — абстрактный).
- **1cedtc.exe**: Ищется рекурсивно под `$edtHome`.
- **Две Guava**: В EDT есть `com.google.guava_15.0.0` (Eclipse) и `32.1.3.jre` (1C). Использовать `.jre`.
- **stepInto после programmatic suspend**: первый stepInto работает (заходит в BSL-метод), второй stepInto из тела метода вызывает нативную функцию → поток выходит из BSL. После step SUSPEND поток RUNNING (не suspended), фрейма нет. Re-suspend ловит поток в непредсказуемой позиции (BSL или platform).
- **ToggleState**: `RegistryToggleState` сохраняет состояние между сессиями Eclipse → кнопка отображается нажатой при старте. `org.eclipse.core.commands.ToggleState` без initial value даёт NPE в `HandlerProxy.updateElement()`. Решение: использовать `RegistryToggleState:false` и сбрасывать `state.setValue(false)` в `createPartControl()` при старте вьюхи.
- **Декомпилированные классы EDT**: сохранять внутри `profiling/_decompiled/<bundle>/<package>/<class>.java` для быстрого доступа без повторной декомпиляции.
- **Git**: первый коммит (build 005) — `git log` для истории.

## Build 030 — poll-only stepping + lifecycle events

**Проблема builds 021-029**: `handleSuspend()` был event-driven — мы ловили SUSPEND от stepInto и реагировали. Но EDT стреляет spurious SUSPEND синхронно изнутри `stepInto()` с pre-step позицией. Это вызывало:
- Бесконечный цикл записи дубликатов (record → stepNextTarget → stepInto → spurious SUSPEND → record)
- Окна исходников на каждый такой SUSPEND
- Гонку между `stopTracing()` и фоновыми тредами (delayed stop)

**Решение (build 030)**: перестать слушать SUSPEND вообще. Вся трассировка — через poll каждые 100ms:
1. `stepLoop()` на UI thread через `Display.timerExec`
2. Каждый poll: проверить все таргеты → записать новые позиции (frame:line) → stepNext в daemon Thread
3. `handleDebugEvents()` обрабатывает только CREATE и TERMINATE (не SUSPEND)
4. Spurious SUSPEND от `stepInto()` — игнорируется, потому что мы не слушаем SUSPEND

**Ключевые изменения относительно build 019**:
- Убран `synchronized` (нет блокировок → нет зависания UI)
- Убран `pendingSuspends`, `initializedTargets`, `steppedTarget`, `steppedThread`
- `sr.suspend()` и `stepInto()` — всегда в daemon Thread
- Запись по изменению позиции (`lastPositions` map) — естественный dedup

## Подводные камни build 030

- **DebugUITools.setActiveDebugContext() отсутствует** в Eclipse 4.30. Пока не нашли способ глушить "show source on step" Debug view-а. Spurious SUSPEND всё ещё обрабатывается Debug view (не нами) и может открывать окна.
- **Периодический poll 100ms**: `isSuspended()` и `getTopStackFrame()` вызываются часто. Это локальные вызовы (не HTTP), но потенциальный overhead есть.
- **CREATE-события не всегда приходят для дочерних таргетов** — poll проверяет `ILaunchManager.getDebugTargets()` каждый цикл, так что новые таргеты всё равно подхватятся.

## Stale frame при открытии модуля по двойному клику

- **Проблема**: `TraceStepRecord.frame` — это `IStackFrame`, захваченный на шаге отладки. При двойном клике (позже, возможно после завершения сессии) `frame.getModule()` и `frame.getSource()` возвращают данные **другого** (обычно первого открытого) модуля — фрейм stale.
- **Решение**: Стереть `IBslStackFrame.getSource()` в `String sourceUri` на этапе захвата (`addRecord`) и хранить в `TraceStepRecord.sourceUri`. При открытии использовать сохранённый URI, а не переспрашивать фрейм.

## Re-suspend → unwanted source windows (build 031)

- **Проблема**: В `stepLoop()` после `tryStepNext()` был re-suspend running-таргетов. `sr.suspend()` стреляет SUSPEND-событием, которое Debug View обрабатывает через EDT-шный `BslSourceDisplay.displaySource()` → `openModuleEditor()` → открывается BSL-редактор.
- **Корень**: stepInto в EDT неблокирующий — отправляет RDBG-команду и сразу возвращается, таргет становится RUNNING. Следующий poll (100ms) видит running-таргет и re-suspend'ит его, порождая лишний SUSPEND.
- **Решение**: Убрать re-suspend целиком. Таргеты сами финишируют step и приходят в SUSPENDED. Никогда не звать `sr.suspend()` во время трассировки — только `stepInto()`/`stepOver()`.
- **Класс**: `TraceView.stepLoop()` в `tracing_plugin/src/.../view/TraceView.java`
- `resolveFile(URI)` конвертирует `platform:/resource/...` → `IFile`. `IDE.openEditor(page, file, true)` открывает редактор BSL (BslXtextEditor).
- `openHelper` (EDT-редактор через `OpenHelper.openEditor(EObject owner, EReference crossRef)`) — план Б, если сессия ещё жива и фрейм нестарый. Получаем через `IEclipseContext` (view site или workbench).
- `TextEditorPositioner.positionEditor(editor, lineNo - 1)` — EDT-метод для позиционирования (0-based line).
- **OpenHelper** — имеет публичный конструктор `OpenHelper(IWorkbenchPage)`. Не нужна DI — можно просто `new OpenHelper(page)`. Используем `openEditor(IFile, ISelection)` для открытия BslXtextEditor через EDT, или `openEditor(EObject, EStructuralFeature)` если доступен Module.
- `IDE.openEditor(page, file, true)` открывает файл, но может открыть в обычном TextEditor, а не BslXtextEditor. Использовать `OpenHelper.openEditor(file, null)` для гарантированного открытия модульного редактора EDT.

## Build 034 — SUSPEND diagnostics: все step-ы приходят как BREAKPOINT

- **Наблюдение из лога**: все SUSPEND-события во время трассировки имеют `event.getDetail() == DebugEvent.BREAKPOINT`. Ни одного `STEP_INTO`, `STEP_OVER` или `CLIENT_REQUEST`.
- **Причина**: EDT реализует stepInto/stepOver через установку временного breakpoint-а на следующую строку. RDBG-сервер ставит `setBreakpoints`, продолжает исполнение — при срабатывании приходит SUSPEND с `details=BREAKPOINT`.
- **Следствие**: Debug View обрабатывает ЛЮБОЙ SUSPEND с `details=BREAKPOINT` через `BslSourceDisplay.displaySource()` → открывается BSL-редактор.

### Почему build 019 не открывал окна, а build 033 открывает

(Теория, не подтверждена экспериментально)

- **019**: `stepInto()` вызывался **синхронно на event thread** (внутри `handleSuspend()`). После записи шага сразу отправлялся следующий stepInto, который переводил таргет в RUNNING. Debug View получал SUSPEND к моменту, когда таргет уже был RUNNING — `isSuspended()` возвращал `false`, Debug View не открывал окно.
- **033/034**: `stepInto()` на **background thread** (daemon). Poll-цикл записывает шаг и запускает stepInto в отдельном треде. Между завершением step-а и отправкой следующего проходит 0-100ms. За это время Debug View успевает обработать SUSPEND, видит `isSuspended() == true` и открывает окно.

### Решение (build 035)

`IDebugEventFilter` в `DebugPlugin` — выкидываем SUSPEND-события на уровне фильтра (до всех listener-ов). Debug View не получает SUSPEND → `debugContextChanged()` не вызывается → окна не открываются.

## Иконки (toolbar)
- `icons/export.png` — скопирован из `profiling/ui/icons/elcl16/profiling_16_export.png` (profiler).

## Build 035 — SUSPEND event filter (подавление окон)

**Проблема**: Build 034 открывает окна BSL-редактора на каждый шаг из-за `BslSourceDisplay.displaySource()` в Debug View.

**Решение**: Установка `IDebugEventFilter`, выкидывающего SUSPEND-события (CREATE/TERMINATE проходят). Poll-цикл и listener работают как в 033.

- `suspendFilter` — добавлен/удалён в `startTracing()`/`stopTracing()`
- Debug View не видит SUSPEND → `SourceLookupService.debugContextChanged()` не вызывается → окна не открываются
- `IDebugEventFilter` работает на уровне `DebugPlugin`, до всех listener-ов (включая Debug View)
- Listener: только логирование SUSPEND деталей (диагностика)

---

## Export (toolbar кнопки)
- `Export CSV` / `Export JSONL` → `Экспорт CSV` / `Экспорт JSONL`.
- Каждая кнопка: `setImageDescriptor(TracingUIActivator.getImageDescriptor("icons/export.png"))`.

---

## Протокол RDBG — связь EDT с dbgs.exe (для справки, напрямую не используется)

**dbgs.exe** — 1C:Enterprise Debug Server. При запуске сессии EDT запускает `dbgs.exe` локально.

### Запуск dbgs.exe

1. `LocalRuntimeDebugLaunchDelegate.doLaunch()` (точка входа при старте отладки).
2. `RuntimeDebugClientTargetManager.createLocal()` → `runDebugServerAndCreateDebugTarget()`:
   - Находит свободный порт через `HttpUtil.findFreePort()`
   - Резолвит компонент: `IRuntimeComponentManager.resolveExecutor(...)`
   - Запускает: `IDebugServerExecutor.runServer(...)` → возвращает `java.lang.Process`
   - Резолвит `RuntimeInstallation` платформы
3. `RuntimeProcessFactory` создаёт `IProcess` для Eclipse Debug Model.

### Транспорт

- **Протокол**: HTTP 1.1 (Jetty HttpClient)
- **User-Agent**: `1CV8`
- **URL**: `http://<host>:<port>/e1crdbg/rdbg`
- **Сериализация**: EMF XML (`RuntimeDebugModelXmlSerializer`)
- **Команды**: HTTP POST, `cmd` query-параметр + тело XML

### Команды RDBG (ключевые для step-tracing)

| cmd | Request | Response | Назначение |
|-----|---------|----------|------------|
| `suspend` | (через `ISuspendResume.suspend()`) | — | Остановить цель |
| `step` | `RDBGStepRequest` (targetId, action) | `RDBGStepResponse.item` | Шаг отладки |
| `getCallStack` | `RDBGGetCallStackRequest` (targetId) | `RDBGGetCallStackResponse.callStack` | Стек вызовов |
| `continue` | (через `DebugStepAction.CONTINUE`) | `RDBGStepResponse.item` | Продолжить |
| `getDbgTargets` | `RDBGSGetDbgTargetsRequest` | `RDBGSGetDbgTargetsResponse.id` | Список целей |
| `setBreakpoints` | `RDBGSetBreakpointsRequest` | void | Установить точки останова |
| `getDbgTargetState` | `RDBGGetDbgTargetStateRequest` (targetId) | `RDBGGetDbgTargetStateResponse.state` | Состояние цели |

(Полный список команд — 22 — в `docs/rdbg-protocol.md`)

### Что мы знаем о dbgs.exe (из дизассемблера ASM)

- **dbgs.exe — НЕ прокси, а шлюз (protocol gateway).**
- Парсит XML через libxml2, общается с 1С через anion IPC.
- Типы целей: `dbgtgt` (локальная), `dbgtgsrv` (серверная), `inet` (удалённая), `vrsbase`/`vrscore`, `techsys`, `bsl`.
- Форматы: `xdto` (EMF XML), `pack` (binary), `json`, `bsl`.
- CLI: `-port`, `-addr`, `-ownerPID`, `-debugServerUsers`, `-password`, `-APPID`.

---
