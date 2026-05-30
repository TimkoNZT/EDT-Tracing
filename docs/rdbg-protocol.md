# Протокол RDBG — связь EDT с dbgs.exe

## Что такое dbgs.exe?

**dbgs.exe** (1C:Enterprise Debug Server) — сервер отладки платформы 1С. EDT запускает его локально как дочерний процесс для каждой сессии отладки. dbgs подключается к процессу 1С (клиент или сервер) и служит HTTP-прокси между EDT и рантаймом. Весь обмен данными (включая результаты профилирования) идёт через него.

---

## 1. Запуск dbgs.exe

### 1.1. Цепочка вызовов (EDT Java)

```
LocalRuntimeDebugLaunchDelegate.doLaunch()
  └─ getDebugTarget(config, launch, port, installation)
      └─ IRuntimeDebugClientTargetManager.createLocal(installation, infobaseRef, port, launch)
          └─ runDebugServerAndCreateDebugTarget(installation, key, port, launch, infobaseRef)
              ├─ HttpUtil.findFreePort()                  // найти свободный порт
              ├─ resolveDebugServer(installation)          // найти компонент DebugServer
              │   └─ IRuntimeComponentManager.resolveExecutor(
              │          ILaunchableRuntimeComponent,
              │          IDebugServerExecutor,
              │          installation,
              │          "com._1c.g5.v8.dt.platform.services.core.componentTypes.DebugServer"
              │      )
              └─ IDebugServerExecutor.runServer(
                     ILaunchableRuntimeComponent,   // сам dbgs.exe
                     port,                          // порт из findFreePort()
                     parentPid                      // ProcessHandle.current().pid()
                 )  →  java.lang.Process
```

После запуска процесса `RuntimeProcessFactory` оборачивает его в Eclipse `IProcess`.

### 1.2. CLI-параметры dbgs.exe (из ASM-дизассемблера)

| Параметр | Назначение |
|----------|------------|
| `-port` | Порт TCP |
| `-portRange` | Диапазон портов |
| `-addr` | Адрес привязки |
| `-ownerPID` | PID родительского процесса (EDT) |
| `-debugServerUsers` | Пользователи, допущенные к отладке |
| `-password` | Пароль для подключения |
| `-APPID` | Идентификатор приложения |

### 1.3. Исходные файлы dbgs.exe (C++)

- `Platform\src\dbgs\src\dbgs.cpp` — точка входа
- `Platform\src\dbgs\src\DebugServerHTTPRequestProcessorHelper.cpp` — обработка HTTP-запросов, парсинг, маршрутизация
- `Platform\src\dbgs\src\DebugServerHTTPRequestReplayingDecoder.cpp` — декодирование HTTP-запросов
- `Platform\src\dbgs\src\IRequestProcessorServiceImpl.cpp` — реализация сервиса обработки запросов
- `Platform\src\anion/ChannelEvents.h` — канальные события

---

## 2. Транспорт

- **Протокол**: HTTP 1.1
- **User-Agent**: `1CV8`
- **URL**: `http://<host>:<port>/e1crdbg/rdbg`
- **HTTP-метод**: только `POST`
- **Клиент (EDT)**: Jetty HttpClient
- **Сервер (dbgs)**: собственный HTTP-сервер на boost::spirit::qi
- **Максимальный размер ответа**: 2 GB (2147483647 байт)

### Архитектура dbgs (worker-thread модель)

```
StartDBGSWorkerThread
  └─ ждёт входящие HTTP-соединения
      └─ StartProcessRequest
          ├─ парсинг HTTP (spirit::qi)
          ├─ диспетчеризация команды
          └─ формирование ответа
      └─ FinishProcessRequest
FinishDBGSWorkerThread
```

Логирование: `core::SCOM_LoggerBase`, методы `isDebugEnabled()` / `logDebug()`.

---

## 3. Сериализация (EMF XML)

Все данные передаются как XML, сериализованные через Eclipse EMF (Eclipse Modeling Framework).

### Схема

```
Запрос:   <request:ClassNameImpl>...</request:ClassNameImpl>
Ответ:    <response:ClassNameImpl>...</response:ClassNameImpl>
```

- Кодировка: UTF-8
- Сериализатор в EDT: `RuntimeDebugModelXmlSerializer`
- EMF-модели находятся в `com._1c.g5.v8.dt.debug.model_*.jar`

### Трассировка

- `/trace/dbgs/responses` — логирует сериализованный XML запросов/ответов
- `/trace/dbgs/io` — логирует URI запросов и время выполнения

---

## 4. Версионирование

Перед подключением EDT запрашивает версию API dbgs:

```
ApiResolveDebugHttpClient.getVersion(url)
  → POST http://host:port/e1crdbg/rdbg?cmd=getRDbgAPIVer
  → MiscRDbgGetAPIVerResponse.version  (строка)
```

Далее `RuntimeDebugClientProvider.get(url)`:
1. Получает версию API
2. Ищет подходящий `RuntimeDebugClientExtension` (загрузка через extension point `runtimeDebugClients`)
3. Создаёт `RuntimeDebugHttpClient` и задаёт ему `IDebugServerVersion`

---

## 5. Команды RDBG (полный список)

### 5.1. Подключение и управление сессией

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `getRDbgAPIVer` | — | `MiscRDbgGetAPIVerResponse.version` | Получить версию API |
| `attachDebugUI` | `RDBGAttachDebugUIRequest` (userName, credentials) | `RDBGAttachDebugUIResponse.result` → `AttachDebugUIResult` | Подключиться к dbgs |
| `detachDebugUI` | `RDBGDetachDebugUIRequest` | `RDBGDetachDebugUIResponse.isResult` (boolean) | Отключиться |
| `initSettings` | `RDBGSetInitialDebugSettingsRequest` (+ `HTTPServerInitialDebugSettingsData`) | void | Начальные настройки |
| `pingDebugUIParams` | `dbgui=<uuid>` query param | `RDBGPingDebugUIResponse.result` → `EList<DBGUIExtCmdInfoBase>` | Poll асинхронных команд |

### 5.2. Профилирование

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `setMeasureMode` | `RDBGSetMeasureModeRequest` (measureModeSeanceID) | void | Вкл/выкл профилирование |

### 5.3. Точки останова

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `setBreakpoints` | `RDBGSetBreakpointsRequest` | void | Установить точки останова |
| `setBreakOnRTE` | `RDBGSetRunTimeErrorProcessingRequest` | void | Останов по ошибкам времени выполнения |

### 5.4. Управление целями отладки

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `getDbgTargets` | `RDBGSGetDbgTargetsRequest` | `RDBGSGetDbgTargetsResponse.id` → `EList<DebugTargetId>` | Список целей |
| `getDbgTargetState` | `RDBGGetDbgTargetStateRequest` (targetId) | `RDBGGetDbgTargetStateResponse.state` → `DbgTargetState` | Состояние цели |
| `attachDetachDbgTargets` | `RDBGAttachDetachDebugTargetsRequest` (attach bool + ids) | void | Подключиться/отключиться к целям |
| `terminateDbgTarget` | `RDBGTerminateDbgTargetRequest` (targetIds) | void | Завершить цель |
| `restartDbgTarget` | `RDBGRestartRequest` (targetIds) | void | Перезапустить цель |
| `setAutoAttachSettings` | `RDBGSetAutoAttachSettingsRequest` | void | Настройки автоподключения |

### 5.5. Шаг отладки

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `step` | `RDBGStepRequest` (targetId, action) | `RDBGStepResponse.item` → `EList<DbgTargetStateInfo>` | Один шаг отладки |

Команда `continue` реализуется через `performStepAction(targetId, DebugStepAction.CONTINUE)` → тот же `cmd=step`, но с `DebugStepAction.CONTINUE`.

### 5.6. Стек вызовов

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `getCallStack` | `RDBGGetCallStackRequest` (targetId) | `RDBGGetCallStackResponse.callStack` (обратный порядок) | Стек вызовов |

### 5.7. Области отладки

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `setListOfDebugArea` | `RDBGSetListOfDebugAreaRequest` (list) | void | Установить области отладки |
| `getListOfDebugArea` | `RDBGGetListOfDebugAreaRequest` | `RDBGGetListOfDebugAreaResponse.debugAreaInfo` | Получить список областей |

### 5.8. Выражения и переменные

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `evalExpr` | `RDBGEvalExprRequest` | `RDBGEvalExprResponse.result` | Вычислить выражение |
| `evalLocalVariables` | `RDBGEvalLocalVariablesRequest` | `RDBGEvalLocalVariablesResponse.result` | Локальные переменные |
| `modifyValue` | `RDBGModifyValueRequest` | `RDBGModifyValueResponse` | Изменить значение |

### 5.9. Обновление информационной базы

| cmd | Request | Response | Описание |
|-----|---------|----------|----------|
| `startUpdateIB` | `RDBGStartUpdateIBRequest` | `RDBGStartUpdateIBResponse.isResult` (boolean) | Начать обновление ИБ |
| `finishUpdateIB` | `RDBGStartUpdateIBRequest` | `RDBGFinishUpdateIBResponse.isResult` (boolean) | Закончить обновление ИБ |

### 5.10. Диспетчеризация на стороне dbgs

В дизассемблированном `dbgs.exe` строковые литералы команд (типа `setMeasureMode`, `attachDebugUI`) **отсутствуют**. Это означает, что сервер, вероятно, диспатчит команды не по `cmd` query-параметру, а по XML-тегу тела запроса (содержимое EMF-сериализации). Например:

```
<request:RDBGSetMeasureModeRequest>
  <measureModeSeanceID>...</measureModeSeanceID>
</request:RDBGSetMeasureModeRequest>
```

---

## 6. Profiling (setMeasureMode)

### 6.1. Полный цикл

```
toggleProfiling(UUID)                           // EDT → вызывается нашей командой
  └─ RuntimeDebugHttpClient.toggleProfiling(uuid)
      └─ ResponseFactory.createRDBGSetMeasureModeRequest()
      └─ request.setMeasureModeSeanceID(uuid != null ? uuid.toString() : null)
      └─ POST /e1crdbg/rdbg?cmd=setMeasureMode
         └─ body: <request:RDBGSetMeasureModeRequest>
                     <measureModeSeanceID>uuid-string</measureModeSeanceID>
                   </request:RDBGSetMeasureModeRequest>

dbgs получает команду:
  └─ передаёт рантайму 1С (через attach к процессу)
  └─ 1С начинает сбор статистики

-- через некоторое время --

toggleProfiling(null)                           // EDT → выключение
  └─ аналогично, но measureModeSeanceID = null

dbgs:
  └─ 1С останавливает сбор
  └─ формирует PerformanceInfoMain (агрегированные данные)
  └─ отдаёт результат EDT через ping-механизм
```

### 6.2. Параметры

- **Единственный параметр**: `measureModeSeanceID` — UUID сессии замера
- **Включение**: UUID задан (строка)
- **Выключение**: UUID = null (сервер останавливает замер и возвращает результат)
- **Нет** режима trace vs profile
- **Нет** частоты сэмплирования
- **Нет** фильтров (по модулям, по строкам и т.д.)

### 6.3. Как приходит результат

1. EDT периодически вызывает `ping()`
2. dbgs отвечает списком `DBGUIExtCmdInfoBase` — асинхронные события
3. Среди них — `DBGUIExtCmdInfoMeasure` (value 6), содержащий `PerformanceInfoMain`
4. Обработка в EDT:

```
RuntimeEventDispatchJob
  └─ разбирает по типу DBGUIExtCmds
      └─ MEASURE_RESULT_PROCESSING (value 6)
          └─ ProfilingService.handleProfilingResult(measure)
              └─ PerformanceInfoMain = measure.getMeasure()
              └─ new ProfilingResult(PerformanceInfoMain, projects, timestamp, connStr)
              └─ loadSourcesAndSaveResult(result)
                  ├─ loadSources(result)           // подгружает текст строк
                  ├─ saveResult(result)            // сохраняет
                  └─ notifyProfilingResultsListeners(result)   // синхронно!
                      └─ наш TraceView.resultsUpdated(result)
```

### 6.4. Структура ProfilingResult

```
ProfilingResult(PerformanceInfoMain, Map<BSLModuleIdInternal, IProject>, LocalDateTime, String)
  ├── performanceInfo: PerformanceInfoMain                     (getPerformanceInfo())
  ├── results: LinkedHashMap<BslModuleReference, Queue<LineProfilingResult>>
  │   └── заполняется из PerformanceInfoMain.getModuleData()
  │       → PerformanceInfoModule.getLineInfo()
  │         → new LineProfilingResult(lineNo, freq, durability, pureDurability,
  │                                   serverCallSignal, moduleId, targetType, project)
  ├── getProfilingResults()     → flatten all queues → List<ILineProfilingResult>
  ├── getReferences()           → results.keySet() (unmodifiable)
  ├── getResultsForModule(ref)  → results.get(ref) → ArrayList
  └── getPerformanceInfo()      → raw EMF PerformanceInfoMain
```

### 6.5. Поля PerformanceInfoLine

| Поле | Тип | Описание |
|------|-----|----------|
| lineNo | BigDecimal | Номер строки |
| frequency | BigDecimal | Количество вызовов (агрегированное) |
| durability | BigDecimal | Общее время |
| pureDurability | BigDecimal | Чистое время (без учёта вложенных вызовов) |
| serverCallSignal | BigDecimal | Сигнал вызова сервера (1.0 — сервер, 0.0 — клиент) |

---

## 7. Асинхронные команды (ping-механизм)

EDT не умеет получать push-события от dbgs. Вместо этого работает **polling**:

1. EDT периодически вызывает `cmd=pingDebugUIParams&dbgui=<uuid>`
2. dbgs отвечает `RDBGPingDebugUIResponse.result` — `EList<DBGUIExtCmdInfoBase>`
3. EDT обрабатывает каждый элемент списка по его типу

### Типы DBGUIExtCmds

| Константа | Value | Описание |
|-----------|-------|----------|
| UNKNOWN | 0 | Неизвестный |
| TARGET_STARTED | 1 | Цель отладки запущена |
| TARGET_QUIT | 2 | Цель отладки завершена |
| CORRECTED_BP | 3 | Точки останова скорректированы |
| RTE_PROCESSING | 4 | Ошибка времени выполнения |
| RTE_ON_BP_CONDITION_PROCESSING | 5 | Ошибка на условии точки останова |
| **MEASURE_RESULT_PROCESSING** | **6** | **Результат профилирования** |
| CALL_STACK_FORMED | 7 | Стек вызовов сформирован |
| EXPR_EVALUATED | 8 | Выражение вычислено |
| VALUE_MODIFIED | 9 | Значение изменено |
| ERROR_VIEW_INFO | 10 | Информация об ошибке |
| FOREGROUND_HELPER_SET | 11 | Helper установлен |
| FOREGROUND_HELPER_REQUEST | 12 | Helper запрошен |
| FOREGROUND_HELPER_PROCESS | 13 | Helper обработан |
| SHOW_METADATA_OBJECT | 14 | Показать объект метаданных |

---

## 8. Формат HTTP-запроса/ответа

### Запрос (EDT → dbgs)

```
POST /e1crdbg/rdbg?cmd=<command>&param1=value1&param2=value2 HTTP/1.1
User-Agent: 1CV8
Content-Type: application/xml

<request:ClassNameImpl>
  <idOfDebuggerUI>uuid-string</idOfDebuggerUI>
  <infoBaseAlias>infobase-alias</infoBaseAlias>
  ...command-specific fields...
</request:ClassNameImpl>
```

### Успешный ответ (dbgs → EDT)

```
HTTP/1.1 200 OK
Content-Type: application/xml

<response:ClassNameImpl>
  ...result fields...
</response:ClassNameImpl>
```

### Ответ с ошибкой (dbgs → EDT)

```
HTTP/1.1 4xx/5xx
Content-Type: application/xml

<response:Exception>
  <descr>Error description</descr>
</response:Exception>
```

---

## 9. Подводные камни

- **Require-Bundle vs Import-Package**: `com._1c.g5.v8.dt.debug.model` не экспортирует внутренние пакеты (`base.data`, `measure`, `rdbg` и т.д.). Для доступа к классам вроде `DebugTargetType`, `DBGUIExtCmdInfoMeasure` нужно использовать `Require-Bundle`, а не `Import-Package`.
- **Лимит 2 GB**: очень большие профили могут не влезть в ответ.
- **Трассировка**: включается через Tracing в Eclipse/EDT для опций `/trace/dbgs/io` и `/trace/dbgs/responses`.
- **Синхронная нотификация**: `notifyProfilingResultsListeners()` вызывается синхронно в том же потоке, что `loadSourcesAndSaveResult()`. Если async = true, это thread-pool поток EDT; если false — поток EDT (Debug event listener).

---

## 10. Ключевые классы EDT для изучения

### `com._1c.g5.v8.dt.debug.core_*.jar`

| Класс | Роль |
|-------|------|
| `RuntimeDebugHttpClient` | Основной HTTP-клиент, реализует все RDBG-команды |
| `AbstractRuntimeDebugHttpClient` | Базовая реализация (buildRequest, performRuntimeHttpRequest) |
| `RuntimeDebugClientProvider` | Загрузка клиентов через extension point `runtimeDebugClients` |
| `ApiResolveDebugHttpClient` | Определение версии API dbgs |
| `RuntimeDebugModelXmlSerializer` | Сериализация EMF ↔ XML |
| `RuntimeDebugClientTargetManager` | Управление целями, запуск dbgs |
| `LocalRuntimeDebugLaunchDelegate` | Запуск локальной сессии отладки |
| `RuntimeProcessFactory` | Создание IProcess для Eclipse Debug Model |
| `DebugServerVersion` / `DebugServerVersionRegistry` | Версии сервера |
| `RuntimeEventDispatchJob` | Обработка асинхронных событий (ping) |

### `com._1c.g5.v8.dt.debug.model_*.jar`

| Модель | Роль |
|--------|------|
| `RDBG*Request` / `RDBG*Response` | EMF-модели всех RDBG-команд |
| `DBGUIExtCmds` | Enum типов асинхронных команд |
| `DBGUIExtCmdInfoMeasure` | Результат замера (содержит `PerformanceInfoMain`) |
| `PerformanceInfoMain` / `PerformanceInfoModule` / `PerformanceInfoLine` | Агрегированные данные профилирования |

---

## 11. Принципиальные ограничения

1. **Агрегация на стороне сервера**: данные профилирования (`PerformanceInfoLine`) содержат только агрегированные счётчики (frequency, durability) по строкам кода. Позапросных (per-call) событий не существует ни на одном слое — ни в RDBG-протоколе, ни в DBGUIExtCmds (15 типов, ни один не про per-call), ни в Eclipse DebugEvent listener.

2. **Нет управления режимом замера**: `setMeasureMode` имеет единственный параметр — UUID сессии. Нельзя выбрать trace vs profile, настроить частоту сэмплирования, включить фильтры.

3. **Polling, не push**: EDT получает результаты профилирования и другие события только через периодический ping к dbgs.
