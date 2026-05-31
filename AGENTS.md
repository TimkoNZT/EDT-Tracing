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

- **Цель**: позапросный трейсинг через RDBG-степпинг. Вместо агрегированного профилировщика 1С — пошаговая трассировка через suspend + stepOver всех debug-таргетов.
- Plugin ID: `com._1c.g5.v8.dt.tracing.ui`
- View ID: `com._1c.g5.v8.dt.tracing.ui.TraceView`
- View class: `com._1c.g5.v8.dt.internal.tracing.ui.view.TraceView` (extends ViewPart)
- Command ID: `com._1c.g5.v8.dt.tracing.ui.commands.toggleTracing`
- Toolbar: `toolbar:org.eclipse.debug.ui.breakpointActionSet` (Debug perspective)
- Activation: `Bundle-ActivationPolicy: lazy`
- Java: compile with `--release 8` (target JavaSE-1.8)

### Как работает (build 036+)

1. **Toggle ON**: собираем `IDebugTarget` из `ILaunchManager.getDebugTargets()`, фильтр `instanceof ISuspendResume`
2. **Async-suspend** каждого не-suspended таргета в daemon Thread
3. Регистрируем `IDebugEventSetListener` на CREATE/TERMINATE/SUSPEND
4. **На SUSPEND** (event thread): запись → stepInto() синхронно. `steppingInProgress` guard от рекурсии, `lastPositions` dedup
5. **Poll loop** (background daemon, 100ms): safety net — запись + step для таргетов, не пойманных обработчиком SUSPEND
6. **CREATE/TERMINATE** — мгновенное добавление/удаление таргетов
7. **Toggle OFF**: `tracingActive=false` → resume всех → clean state

### Ключевое наблюдение (build 034)

Все SUSPEND-события имеют `event.getDetail() == DebugEvent.BREAKPOINT` — EDT реализует stepInto через временный breakpoint. Ни одного `STEP_INTO`/`STEP_OVER`. Debug View обрабатывает ЛЮБОЙ SUSPEND с BREAKPOINT через `BslSourceDisplay.displaySource()`.

**Почему окна не открываются (build 036)**: stepInto() на event thread переводит таргет в RUNNING до того, как Debug View обработает SUSPEND → `isSuspended()` возвращает false → `displaySource()` не вызывается.

**Варианты, которые не сработали**:
- Poll-only (build 033): окна открываются, т.к. stepInto асинхронный, Debug View видит SUSPEND вовремя
- `IDebugEventFilter` (build 035): ломает `isSuspended()` — только 1 шаг

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
- **Не очищать traceRecords на старте**: при `startTracing()` список записей не чистится (`traceRecords.clear()` убран). Предыдущие сессии трассировки остаются в таблице для сравнения.


## Stale frame & opening editors

- **Stale frame**: `IStackFrame` при двойном клике (после завершения сессии) возвращает данные **другого** модуля — фрейм stale.
- **Решение**: хранить `sourceUri` как строку на этапе записи; при открытии использовать `resolveFile(URI)` → `IFile` → `OpenHelper.openEditor(file, null)` для BslXtextEditor, или `IDE.openEditor()` как fallback.
- **OpenHelper** — публичный конструктор `new OpenHelper(page)`, не нужна DI.
- `resolveFile(URI)` конвертирует `platform:/resource/...` → `IFile`.
- `TextEditorPositioner.positionEditor(editor, lineNo - 1)` — 0-based line.
- `IDE.openEditor(page, file, true)` может открыть TextEditor вместо BslXtextEditor. `OpenHelper.openEditor(file, null)` гарантирует EDT-редактор.

## Иконки (toolbar)
- `icons/export.png` — скопирован из `profiling/ui/icons/elcl16/profiling_16_export.png` (profiler).

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

(Полный список команд — 22 — смотри в RDBG-протоколе, декомпилированные классы EDT)

### Что мы знаем о dbgs.exe (из дизассемблера ASM)

- **dbgs.exe — НЕ прокси, а шлюз (protocol gateway).**
- Парсит XML через libxml2, общается с 1С через anion IPC.
- Типы целей: `dbgtgt` (локальная), `dbgtgsrv` (серверная), `inet` (удалённая), `vrsbase`/`vrscore`, `techsys`, `bsl`.
- Форматы: `xdto` (EMF XML), `pack` (binary), `json`, `bsl`.
- CLI: `-port`, `-addr`, `-ownerPID`, `-debugServerUsers`, `-password`, `-APPID`.

---
