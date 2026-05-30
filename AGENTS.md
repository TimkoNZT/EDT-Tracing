**AGENTS.md** — Накапливай новые знания здесь, стирай устаревшее.

- **AGENTS.md rule**: В процессе работы собирай все важные находки (архитектура, баги, команды, подводные камни) в этот файл. Стирай устаревшее. Этот пункт — тоже правило.

---

## Build (javac, no Maven)

- **EDT Tracing**: `cd tracing_plugin && .\build-javac.ps1`
  - Авто-определяет EDT, компилирует `src/`, пакует JAR.
  - Итог: P2-репозиторий + ZIP в `tracing_plugin/dist/`.

- **Минимальные sample-плагины**: `cd <root> && .\build-minimal.ps1 ...`
  - Параметры: `-ModuleDir`, `-PluginId`, `-PluginLabel`, `-OutDir`, `-SrcDir`.

- **Важно: `plugin.xml` должен быть в корне модуля** (не внутри `META-INF/`!), иначе Eclipse не видит extension-точки. Build-скрипты копируют `plugin.xml` в корень JAR отдельно от `META-INF/`.

- **`fragment.e4xmi`** — тоже в корень JAR.

- **Feature обязателен** — без него p2 не показывает плагин в Install New Software.

---

## Рабочий подход: Legacy `org.eclipse.ui.views`

**Подтверждено**: Legacy Sample (ViewPart + `class="..."` напрямую) устанавливается и показывает view в EDT.

**Ключевые моменты:**
- ViewPart создаётся через прямой `class="com.example.SampleView"` — **фабрика не обязательна**.
- `plugin.xml`:
  ```xml
  <extension point="org.eclipse.ui.views">
     <view id="..." name="%view.name"
           class="com.example.SampleView"
           category="com._1c.g5.v8.dt.ui.v8category"/>
  </extension>
  ```
- `MANIFEST.MF`: обязательно `Require-Bundle: org.eclipse.ui, org.eclipse.core.runtime, org.eclipse.jface, org.eclipse.swt`.
- ViewPart должен иметь no-arg конструктор (или полагаться на умолчания).
- OSGi-сервисы (например, `IProfilingService`) получать через `BundleContext.getServiceReference()` вручную.

---

## e4 Fragment approach — работает в EDT (но не используется)

**Ссылочный плагин**: `com.company1c.link.ide.edt_4.0.3` (exploded bundle) использует `fragment.e4xmi` + `org.eclipse.e4.workbench.model` — его SampleView отображается в EDT.

**Фактические версии e4-бандлов в EDT 2026.1.1:**
```
org.eclipse.e4.core.services      2.4.200.v20231103-2012
org.eclipse.e4.ui.workbench       1.15.200.v20231030-2045
org.eclipse.e4.ui.services        1.6.200.v20231030-2045
```

**Требования:**
- `Export-Package` для пакета с view-классом (чтобы e4 мог загрузить класс).
- `Require-Bundle: org.eclipse.e4.ui.workbench, org.eclipse.e4.ui.services, org.eclipse.e4.core.services`.
- Версионные диапазоны ДОЛЖНЫ включать реальные версии. Использовать `[1.7.0,3.0.0)` или вообще без диапазона.
- View-класс — POJO (не обязательно extends ViewPart), с методами `createPartControl(Composite)` и `setFocus()`.
- `fragment.e4xmi` с `basic:PartDescriptor` + `contributionURI="bundleclass://..."`.

---

## P2 репозиторий

- Publisher (`FeaturesAndBundlesPublisher`) сам генерирует IU для плагина и фичи в `content.xml`.
- Для category IU — инжектим вручную после publisher-а (`<unit id='edt-tracing.category'>` с `org.eclipse.equinox.p2.type.category=true`).
- `.zip` не используется — установка напрямую через Local... в Install New Software.

---

## Плагин EDT Tracing

- Plugin ID: `com._1c.g5.v8.dt.tracing.ui` (временный неймспейс 1С, переименовать перед публикацией).
- View ID: `com._1c.g5.v8.dt.tracing.ui.TraceView`.
- View class: `com._1c.g5.v8.dt.internal.tracing.ui.view.TraceView` (extends ViewPart).
- Command ID: `com._1c.g5.v8.dt.tracing.ui.commands.toggleTracing`.
- Toolbar: `toolbar:org.eclipse.debug.ui.breakpointActionSet` (Debug perspective).
- `MANIFEST.MF`: `Import-Package: com._1c.g5.v8.dt.profiling.core` (НЕ Require-Bundle) + стандартные Eclipse зависимости.
- Activation: `Bundle-ActivationPolicy: lazy`.
- Java: compile with `--release 8` (target JavaSE-1.8).

---

## Classpath для компиляции (~28 JAR)

- `org.eclipse.swt.win32.win32.x86_64_*.jar` — обязателен (основной SWT пустой).
- `com.google.guava_32.1.3.jre` (не 15.x!).
- `com.google.guava.failureaccess_*`.
- `com._1c.g5.v8.dt.profiling.core_*`, `com._1c.g5.v8.dt.debug.model_*`, `com._1c.g5.v8.dt.debug.core_*`.
- `org.eclipse.emf.common_*` — транзитив для debug.model.

---

## Подводные камни

- **plugin.xml в META-INF/**: Eclipse не видит extension-точки. Файл должен быть в корне JAR.
- **Версионные диапазоны**: Слишком узкие диапазоны (`[1.7.0,2.0.0)`) блокируют установку. Использовать `[X.Y.Z,10.0.0)` или без версии.
- **Export-Package**: Обязателен для e4-фрагментов (иначе класс view недоступен). Для legacy view не обязателен.
- **Bundle-Activator**: Должен быть конкретный класс, не абстрактный (`AbstractUIPlugin` — абстрактный).
- **1cedtc.exe**: Ищется рекурсивно под `$edtHome`.
- **Две Guava**: В EDT есть `com.google.guava_15.0.0` (Eclipse) и `32.1.3.jre` (1C). Использовать `.jre`.

---

## Ссылочные плагины

- `com.company1c.link.ide.edt_4.0.3/` — пример e4-фрагмента (exploded bundle) с SampleView.
- `com._1c.g5.v8.dt.profiling.ui_*.jar` — пример legacy view + ExecutableExtensionFactory.
- `legacy-view-plugin/` — наш минимальный legacy sample (подтверждённо работает).
- `e4-fragment-plugin/` — наш минимальный e4-фрагмент (не проверен до конца).
