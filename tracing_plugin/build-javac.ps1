#!/usr/bin/env pwsh
# build-javac.ps1 — Compile, JAR, P2 repo, ZIP for EDT Tracing Plugin
# Detects EDT installation automatically.

$ErrorActionPreference = "Stop"
$PluginDir = Split-Path -Parent $PSCommandPath
$ModuleDir = Join-Path $PluginDir "com._1c.g5.v8.dt.tracing.ui"
$SrcDir    = Join-Path $PluginDir "src"
$OutDir    = Join-Path $PluginDir "dist"
$TargetDir = Join-Path $PluginDir "target"

# ---------- 1. Find EDT ----------
$edtHomeCandidates = @(
    "C:\Program Files\1C\1CE\components",
    "C:\Program Files (x86)\1C\1CE\components"
)
$edtHome = $null
foreach ($base in $edtHomeCandidates) {
    if (Test-Path $base) {
        $dirs = Get-ChildItem $base -Directory -Name | Where-Object { $_ -match "1c-edt" }
        if ($dirs) {
            $edtHome = Join-Path $base $dirs[0]
            break
        }
    }
}
if (-not $edtHome) {
    # Fallback: try env var
    $edtHome = $env:EDT_HOME
}
if (-not $edtHome -or -not (Test-Path $edtHome)) {
    Write-Error "EDT installation not found. Set EDT_HOME env var or install EDT."
    exit 1
}
$pluginsDir = Join-Path $edtHome "plugins"
Write-Output "EDT: $edtHome"

# ---------- 2. Find required JARs ----------
$requiredPatterns = @(
    "com._1c.g5.v8.dt.profiling.core_*.jar",
    "com._1c.g5.wiring_*.jar",
    "com.google.inject_*.jar",
    "com.google.guava_*.jar",
    "com.google.guava.failureaccess_*.jar",
    "org.eclipse.osgi_*.jar",
    "org.eclipse.ui.workbench_*.jar",
    "org.eclipse.jface_*.jar",
    "org.eclipse.jface.text_*.jar",
    "org.eclipse.swt.win32.win32.x86_64_*.jar",
    "org.eclipse.core.runtime_*.jar",
    "org.eclipse.core.commands_*.jar",
    "org.eclipse.core.expressions_*.jar",
    "org.eclipse.debug.ui_*.jar",
    "org.eclipse.debug.core_*.jar",
    "org.eclipse.e4.core.di_*.jar",
    "org.eclipse.e4.core.services_*.jar",
    "org.eclipse.core.jobs_*.jar",
    "org.eclipse.core.contenttype_*.jar",
    "org.eclipse.core.resources_*.jar",
    "org.eclipse.ui_*.jar",
    "org.eclipse.ui.ide_*.jar",
    "org.eclipse.equinox.common_*.jar",
    "org.eclipse.equinox.registry_*.jar",
    "org.eclipse.osgi.services_*.jar",
    "com._1c.g5.v8.dt.debug.core_*.jar",
    "com._1c.g5.v8.dt.debug.model_*.jar",
    "org.eclipse.emf.common_*.jar"
)

$classpathJars = @()
foreach ($pat in $requiredPatterns) {
    $found = Get-ChildItem (Join-Path $pluginsDir $pat) -Name | Select-Object -First 1
    if (-not $found) {
        Write-Warning "Missing: $pat"
    } else {
        $classpathJars += Join-Path $pluginsDir $found
    }
}
$classpath = $classpathJars -join ";"
Write-Output "Classpath JARs: $($classpathJars.Count)"

# ---------- 3. Clean & compile ----------
if (Test-Path $TargetDir) { Remove-Item $TargetDir -Recurse -Force }
$classesOut = Join-Path $TargetDir "classes"
New-Item -ItemType Directory -Path $classesOut -Force | Out-Null

# Find Java
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    # Try common paths
    $javaHomeCandidates = @(
        "C:\Program Files\BellSoft\LibericaJDK-17",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk-17*"
    )
    foreach ($c in $javaHomeCandidates) {
        $dirs = Get-ChildItem $c -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($dirs) { $javaHome = $dirs.FullName; break }
    }
}
if (-not $javaHome) { $javaHome = $env:ProgramFiles + "\BellSoft\LibericaJDK-17" }
$javac = Join-Path $javaHome "bin\javac"
if (-not (Test-Path $javac)) {
    # Try finding javac on PATH
    $javac = (Get-Command "javac" -ErrorAction SilentlyContinue).Source
    if (-not $javac) { Write-Error "javac not found"; exit 1 }
}
Write-Output "javac: $javac"

# Collect all .java files
$javaFiles = Get-ChildItem $SrcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
Write-Output "Compiling $($javaFiles.Count) files..."

$srcArgs = @(
    "--release", "8",
    "-cp", $classpath,
    "-d", $classesOut,
    "-sourcepath", $SrcDir
) + $javaFiles

& $javac $srcArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output "Compilation OK"

# ---------- 4. Build JAR ----------
$jarStage = Join-Path $TargetDir "jar-stage"
New-Item -ItemType Directory -Path $jarStage -Force | Out-Null

# Copy classes
Copy-Item "$classesOut\*" $jarStage -Recurse -Force

# Copy META-INF (MANIFEST.MF, messages*.properties)
$metaInfModule = Join-Path $ModuleDir "META-INF"
$metaInfStage = Join-Path $jarStage "META-INF"
New-Item -ItemType Directory -Path $metaInfStage -Force | Out-Null
Copy-Item "$metaInfModule\MANIFEST.MF" $metaInfStage -Force
if (Test-Path "$metaInfModule\messages*.properties") { Copy-Item "$metaInfModule\messages*.properties" $metaInfStage -Force }

# Copy root-level files (plugin.xml, fragment.e4xmi) to JAR root
if (Test-Path (Join-Path $ModuleDir "plugin.xml")) { Copy-Item (Join-Path $ModuleDir "plugin.xml") $jarStage -Force }
if (Test-Path (Join-Path $ModuleDir "fragment.e4xmi")) { Copy-Item (Join-Path $ModuleDir "fragment.e4xmi") $jarStage -Force }

$jarFile = Join-Path $TargetDir "com._1c.g5.v8.dt.tracing.ui_1.0.0.jar"
Set-Location $jarStage
& "$($javaHome)\bin\jar" cfm $jarFile "META-INF\MANIFEST.MF" .
Set-Location $PluginDir
Write-Output "JAR: $jarFile ($((Get-Item $jarFile).Length) bytes)"

# ---------- 5. Build Feature JAR ----------
$featureDir = Join-Path $TargetDir "feature"
New-Item -ItemType Directory -Path $featureDir -Force | Out-Null
$featureMetaInf = Join-Path $featureDir "META-INF"
New-Item -ItemType Directory -Path $featureMetaInf -Force | Out-Null

@"
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: EDT Tracing Feature
Bundle-SymbolicName: com._1c.g5.v8.dt.tracing.ui.feature;singleton:=true
Bundle-Version: 1.0.0
Bundle-Vendor: 1C
"@ | Set-Content (Join-Path $featureMetaInf "MANIFEST.MF") -Encoding Ascii

@"
<?xml version="1.0" encoding="UTF-8"?>
<feature id="com._1c.g5.v8.dt.tracing.ui.feature" label="EDT Tracing Plugin" version="1.0.0" provider-name="1C">
<description>EDT Tracing UI - просмотр и экспорт трейсинга (CSV/JSONL)</description>
<license url="https://www.eclipse.org/legal/epl-v10.html">Eclipse Public License - v 1.0</license>
<plugin id="com._1c.g5.v8.dt.tracing.ui" download-size="18" install-size="36" version="1.0.0" unpack="false"/>
</feature>
"@ | Set-Content (Join-Path $featureDir "feature.xml") -Encoding Utf8

$featureJar = Join-Path $TargetDir "com._1c.g5.v8.dt.tracing.ui.feature_1.0.0.jar"
Set-Location $featureDir
& "$($javaHome)\bin\jar" cfm $featureJar "META-INF\MANIFEST.MF" feature.xml
Set-Location $PluginDir
Write-Output "Feature JAR: $featureJar ($((Get-Item $featureJar).Length) bytes)"

# ---------- 6. Build P2 repo ----------
$p2repoDir = Join-Path $OutDir "p2repo"
if (Test-Path $p2repoDir) { Remove-Item $p2repoDir -Recurse -Force }

# Find 1cedtc.exe
$1cedtc = Get-ChildItem $edtHome -Recurse -Filter "1cedtc.exe" | Select-Object -First 1 -ExpandProperty FullName
if (-not $1cedtc) {
    Write-Error "1cedtc.exe not found in $edtHome"
    exit 1
}

$p2PluginsDir = Join-Path $p2repoDir "plugins"
$p2FeaturesDir = Join-Path $p2repoDir "features"
New-Item -ItemType Directory -Path $p2PluginsDir -Force | Out-Null
New-Item -ItemType Directory -Path $p2FeaturesDir -Force | Out-Null

Copy-Item $jarFile $p2PluginsDir
Copy-Item $featureJar $p2FeaturesDir

Write-Output "Running FeaturesAndBundlesPublisher..."
$p2repoUri = "file:/$($p2repoDir -replace '\\', '/')"
& $1cedtc -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher `
    -source "$p2repoDir" `
    -metadataRepository "$p2repoUri" `
    -artifactRepository "$p2repoUri" `
    -publishArtifacts -compress
if ($LASTEXITCODE -ne 0) { Write-Warning "Publisher returned $LASTEXITCODE" }

# ---------- 7. Add category ----------
$contentJar = Join-Path $p2repoDir "content.jar"
if (Test-Path $contentJar) {
    # Inject category into content.xml
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $tmp = Join-Path $TargetDir "content_extract"
    New-Item -ItemType Directory -Path $tmp -Force | Out-Null
    $z = [System.IO.Compression.ZipFile]::OpenRead($contentJar)
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($z.Entries[0], (Join-Path $tmp "content.xml"), $true)
    $z.Dispose()

    $contentXml = Get-Content (Join-Path $tmp "content.xml") -Raw

    # Add plugin IU if missing
    if ($contentXml -notmatch '<unit id=''com._1c.g5.v8.dt.tracing.ui''') {
        $pluginUnit = @"
    <unit id='com._1c.g5.v8.dt.tracing.ui' version='1.0.0' singleton='false'>
      <properties size='4'>
        <property name='org.eclipse.equinox.p2.name' value='EDT Tracing UI Plugin'/>
        <property name='org.eclipse.equinox.p2.description' value='EDT Tracing UI - просмотр и экспорт трейсинга'/>
        <property name='org.eclipse.equinox.p2.provider' value='1C'/>
        <property name='org.eclipse.equinox.p2.type' value='Bundle'/>
      </properties>
      <provides size='2'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='com._1c.g5.v8.dt.tracing.ui' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='com._1c.g5.v8.dt.tracing.ui' version='1.0.0'/>
      </provides>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-ManifestVersion: 2
Bundle-Name: EDT Tracing UI
Bundle-SymbolicName: com._1c.g5.v8.dt.tracing.ui;singleton:=true
Bundle-Version: 1.0.0
Bundle-Activator: com._1c.g5.v8.dt.internal.tracing.ui.TracingUIActivator
Bundle-ActivationPolicy: lazy
Bundle-Vendor: 1C
Bundle-Localization: META-INF/messages
Require-Bundle: com._1c.g5.v8.dt.profiling.core;bundle-version="[5.0.0,10.0.0)",
                 org.eclipse.debug.ui,
                 org.eclipse.debug.core,
                 org.eclipse.ui.workbench,
                 org.eclipse.jface;bundle-version="[3.0.0,4.0.0)",
                 org.eclipse.core.commands;bundle-version="[3.6.0,4.0.0)",
                 org.eclipse.swt;bundle-version="[3.100.0,4.0.0)",
                 org.eclipse.e4.ui.workbench;bundle-version="[1.8.0,2.0.0)"
Export-Package: com._1c.g5.v8.dt.internal.tracing.ui.view;version="1.0.0"
Bundle-RequiredExecutionEnvironment: JavaSE-1.8
          </instruction>
        </instructions>
      </touchpointData>
    </unit>

"@ | Out-String

        # Remove any existing plugin unit before inserting new one
        $contentXml = $contentXml -replace '\s*<unit id=''com._1c.g5.v8.dt.tracing.ui[^'']*''.*?</unit>\s*', "`n"

        # Find the units closing tag and add plugin before it
        $contentXml = $contentXml -replace '</units>', "$pluginUnit</units>"

        # Bump units size by 1 (we're adding one plugin IU)
        $sizeMatch = [regex]::Match($contentXml, "<units size='(\d+)'")
        if ($sizeMatch.Success) {
            $oldSize = [int]$sizeMatch.Groups[1].Value
            $contentXml = $contentXml -replace "<units size='$oldSize'>", "<units size='$($oldSize + 1)'>"
        }
    }

    # Add category unit if missing
    if ($contentXml -notmatch '<unit id=''edt-tracing.category''') {
        $categoryUnit = @"
    <unit id='edt-tracing.category' version='1.0.0' singleton='false'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='EDT Tracing'/>
        <property name='org.eclipse.equinox.p2.description' value='EDT Tracing UI - просмотр и экспорт трейсинга (CSV/JSONL)'/>
        <property name='org.eclipse.equinox.p2.type.category' value='true'/>
      </properties>
      <provides size='1'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='edt-tracing.category' version='1.0.0'/>
      </provides>
      <requires size='1'>
        <required namespace='org.eclipse.equinox.p2.iu' name='com._1c.g5.v8.dt.tracing.ui.feature.feature.group' range='[1.0.0,1.0.0]'/>
      </requires>
      <touchpoint id='null' version='0.0.0'/>
    </unit>

"@ | Out-String

        # Remove any existing edt-tracing category unit before inserting new one
        $contentXml = $contentXml -replace '\s*<unit id=''edt-tracing[^'']*''.*?</unit>\s*', "`n"

        # Bump units size by 1 (we're adding one category IU)
        $sizeMatch = [regex]::Match($contentXml, "<units size='(\d+)'")
        if ($sizeMatch.Success) {
            $oldSize = [int]$sizeMatch.Groups[1].Value
            $contentXml = $contentXml -replace "<units size='$oldSize'>", "<units size='$($oldSize + 1)'>"
        }

        $contentXml = $contentXml -replace '</units>', "$categoryUnit</units>"
    }

    Set-Content (Join-Path $tmp "content.xml") $contentXml -NoNewline

    # Rebuild content.jar
    Remove-Item $contentJar -Force
    Set-Location $tmp
    & "$($javaHome)\bin\jar" cfM $contentJar "content.xml"
    Set-Location $PluginDir
    Remove-Item $tmp -Recurse -Force
}

# ---------- 8. Create p2.index ----------
@"
version=1
metadata.repository.factory.order= content.jar,content.xml.xz!
artifact.repository.factory.order= artifacts.jar,artifacts.xml.xz!
"@ | Set-Content (Join-Path $p2repoDir "p2.index") -Encoding Ascii

# ---------- 9. Create ZIP ----------
$zipFile = Join-Path $OutDir "edt-tracing-plugin_1.0.0.zip"
if (Test-Path $zipFile) { Remove-Item $zipFile -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($p2repoDir, $zipFile)
Write-Output "ZIP: $zipFile"

Write-Output "`n=== BUILD COMPLETE ==="
Write-Output "P2 repo: $p2repoDir"
Write-Output "Install via: Help -> Install New Software... -> Add -> Local -> $p2repoDir"
