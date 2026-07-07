#!/usr/bin/env pwsh
# build-javac.ps1 — Compile, JAR, P2 repo, ZIP for EDT Tracing Plugin
# Detects EDT installation automatically.

param(
    [string]$Version,
    [string]$OutDir,
    [string]$SrcDir
)

$ErrorActionPreference = "Stop"
$PluginDir = Split-Path -Parent $PSCommandPath
$ModuleDir = Join-Path $PluginDir "com.nzt.edt.tracing"
if (-not $SrcDir) { $SrcDir = Join-Path $PluginDir "src" }
if (-not $OutDir) { $OutDir = Join-Path $PluginDir "dist" }
$TargetDir = Join-Path $PluginDir "target"

# Clean output at the start
if (Test-Path $OutDir) { Remove-Item $OutDir -Recurse -Force }

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

# ---------- 2. Version ----------
$PluginId = "com.nzt.edt.tracing"
$FeatureId = "$PluginId.feature"
$manifestPath = Join-Path $ModuleDir "META-INF\MANIFEST.MF"
$manifestText = Get-Content $manifestPath -Raw
$baseVersion = if ($manifestText -match 'Bundle-Version:\s*(\S+)') { $matches[1] } else { "1.0.0.qualifier" }

# If -Version given, update MANIFEST.MF
if ($Version) {
    $manifestText = $manifestText -replace '(Bundle-Version:\s*)\S+', "`$1$Version"
    [System.IO.File]::WriteAllText($manifestPath, $manifestText, [System.Text.UTF8Encoding]::new($false))
    $baseVersion = $Version
}

$timestamp = Get-Date -Format "yyyyMMddHHmm"
$PluginVersion = $baseVersion -replace 'qualifier', "v$timestamp"
$FeatureVersion = $PluginVersion
$catVersion = $baseVersion -replace '\.qualifier$', ''
Write-Output "Version: $PluginVersion"

# ---------- 3. Find required JARs ----------
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
    "org.eclipse.text_*.jar",
    "org.eclipse.swt.win32.win32.x86_64_*.jar",
    "org.eclipse.core.runtime_*.jar",
    "org.eclipse.core.commands_*.jar",
    "org.eclipse.core.expressions_*.jar",
    "org.eclipse.debug.ui_*.jar",
    "org.eclipse.debug.core_*.jar",
    "org.eclipse.e4.core.contexts_*.jar",
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
    "com._1c.g5.v8.dt.bsl.model_*.jar",
    "com._1c.g5.v8.dt.common.ui_*.jar",
    "com._1c.g5.v8.dt.mcore_*.jar",
    "com._1c.g5.v8.dt.debug.core_*.jar",
    "com._1c.g5.v8.dt.debug.model_*.jar",
    "com._1c.g5.v8.dt.ui_*.jar",
    "org.eclipse.emf.common_*.jar",
    "org.eclipse.emf.ecore_*.jar",
    "org.eclipse.ui.workbench.texteditor_*.jar"
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

# ---------- 4. Clean & compile ----------
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

# ---------- 5. Build JAR ----------
$jarStage = Join-Path $TargetDir "jar-stage"
New-Item -ItemType Directory -Path $jarStage -Force | Out-Null

# Copy classes
Copy-Item "$classesOut\*" $jarStage -Recurse -Force

# Copy META-INF (MANIFEST.MF, messages*.properties)
$metaInfModule = Join-Path $ModuleDir "META-INF"
$metaInfStage = Join-Path $jarStage "META-INF"
New-Item -ItemType Directory -Path $metaInfStage -Force | Out-Null
Copy-Item "$metaInfModule\MANIFEST.MF" $metaInfStage -Force
if (Test-Path "$metaInfModule\messages*.properties") { Copy-Item -Path "$metaInfModule\messages*.properties" -Destination $metaInfStage -Force }

# Copy icons from module
if (Test-Path (Join-Path $ModuleDir "icons")) { Copy-Item (Join-Path $ModuleDir "icons") $jarStage -Recurse -Force }

# Copy resource bundles (.properties) from source tree
Get-ChildItem $SrcDir -Recurse -Filter "*.properties" | ForEach-Object {
    $relPath = $_.FullName.Substring($SrcDir.Length + 1)
    $destPath = Join-Path $jarStage $relPath
    $destDir = Split-Path $destPath -Parent
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item $_.FullName $destDir -Force
}

# Convert .properties to ASCII \uXXXX for Java PropertyResourceBundle
Get-ChildItem $jarStage -Recurse -Filter "*.properties" | ForEach-Object {
    $raw = [System.IO.File]::ReadAllText($_.FullName, [System.Text.UTF8Encoding]::new($false))
    $sb = New-Object System.Text.StringBuilder $raw.Length
    $raw.ToCharArray() | ForEach-Object {
        if ([int]$_ -gt 127) { $sb.AppendFormat("\u{0:X4}", [int]$_) | Out-Null }
        else { $sb.Append($_) | Out-Null }
    }
    [System.IO.File]::WriteAllText($_.FullName, $sb.ToString(), [System.Text.Encoding]::ASCII)
}

# Copy root-level files (plugin.xml, fragment.e4xmi) to JAR root
if (Test-Path (Join-Path $ModuleDir "plugin.xml")) { Copy-Item (Join-Path $ModuleDir "plugin.xml") $jarStage -Force }
if (Test-Path (Join-Path $ModuleDir "fragment.e4xmi")) { Copy-Item (Join-Path $ModuleDir "fragment.e4xmi") $jarStage -Force }

# Patch Bundle-Version in staged manifest
$manifestStage = Join-Path $jarStage "META-INF\MANIFEST.MF"
$manifestText = Get-Content $manifestStage -Raw
$manifestText = $manifestText -replace '(Bundle-Version:\s*)\S+', ('${1}' + $PluginVersion)
[System.IO.File]::WriteAllText($manifestStage, $manifestText, [System.Text.UTF8Encoding]::New($false))

$jarFile = Join-Path $TargetDir "com.nzt.edt.tracing_$PluginVersion.jar"
Set-Location $jarStage
& "$($javaHome)\bin\jar" cfm $jarFile "META-INF\MANIFEST.MF" .
Set-Location $PluginDir
Write-Output "JAR: $jarFile ($((Get-Item $jarFile).Length) bytes)"

# ---------- 6. Build Feature JAR ----------
$featureDir = Join-Path $TargetDir "feature"
New-Item -ItemType Directory -Path $featureDir -Force | Out-Null
$featureMetaInf = Join-Path $featureDir "META-INF"
New-Item -ItemType Directory -Path $featureMetaInf -Force | Out-Null

@"
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: EDT Tracing Feature
Bundle-SymbolicName: com.nzt.edt.tracing.feature;singleton:=true
Bundle-Version: $FeatureVersion
Bundle-Vendor: NZT
"@ | Set-Content (Join-Path $featureMetaInf "MANIFEST.MF") -Encoding Ascii

@"
<?xml version="1.0" encoding="UTF-8"?>
<feature id="com.nzt.edt.tracing.feature" label="EDT Tracing" version="$FeatureVersion" provider-name="NZT">
<description>Плагин для EDT добавляющий пошаговую трассировку и экспорт результатов</description>
<plugin id="com.nzt.edt.tracing" download-size="18" install-size="36" version="$PluginVersion" unpack="false"/>
</feature>
"@ | Set-Content (Join-Path $featureDir "feature.xml") -Encoding Utf8

$featureJar = Join-Path $TargetDir "com.nzt.edt.tracing.feature_$FeatureVersion.jar"
Set-Location $featureDir
& "$($javaHome)\bin\jar" cfm $featureJar "META-INF\MANIFEST.MF" feature.xml
Set-Location $PluginDir
Write-Output "Feature JAR: $featureJar ($((Get-Item $featureJar).Length) bytes)"

# ---------- 7. Build P2 repo ----------
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

# ---------- 8. Add category ----------
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
    if ($contentXml -notmatch '<unit id=''com\.nzt\.edt\.tracing''') {
        $pluginUnit = @"
    <unit id='com.nzt.edt.tracing' version='$PluginVersion' singleton='false'>
      <properties size='4'>
        <property name='org.eclipse.equinox.p2.name' value='EDT Tracing UI Plugin'/>
        <property name='org.eclipse.equinox.p2.description' value='Плагин для EDT добавляющий пошаговую трассировку и экспорт результатов'/>
        <property name='org.eclipse.equinox.p2.provider' value='NZT'/>
        <property name='org.eclipse.equinox.p2.type' value='Bundle'/>
      </properties>
      <provides size='2'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='com.nzt.edt.tracing' version='$PluginVersion'/>
        <provided namespace='osgi.bundle' name='com.nzt.edt.tracing' version='$PluginVersion'/>
      </provides>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-ManifestVersion: 2
Bundle-Name: EDT Tracing UI
Bundle-SymbolicName: com.nzt.edt.tracing;singleton:=true
Bundle-Version: $PluginVersion
Bundle-Activator: com.nzt.edt.tracing.TracingUIActivator
Bundle-ActivationPolicy: lazy
Bundle-Vendor: NZT
Bundle-Localization: META-INF/messages
Require-Bundle: org.eclipse.debug.ui,
                 org.eclipse.debug.core,
                 org.eclipse.ui.workbench,
                 org.eclipse.core.runtime;bundle-version="[3.11.1,4.0.0)",
                 org.eclipse.jface;bundle-version="[3.0.0,4.0.0)",
                 org.eclipse.swt;bundle-version="[3.100.0,4.0.0)",
                 com._1c.g5.v8.dt.debug.model;bundle-version="[3.0.0,10.0.0)"
Import-Package: com._1c.g5.v8.dt.profiling.core;version="[5.0.0,10.0.0)",
  org.eclipse.core.commands,
  org.eclipse.emf.common,
  org.osgi.framework
Export-Package: com.nzt.edt.tracing.view;version="1.0.0"
Bundle-RequiredExecutionEnvironment: JavaSE-1.8
          </instruction>
        </instructions>
      </touchpointData>
    </unit>

"@ | Out-String

        # Remove any existing plugin unit before inserting new one
        $contentXml = $contentXml -replace '\s*<unit id=''com\.nzt\.edt\.tracing[^'']*''.*?</unit>\s*', "`n"

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
    $catId = "${PluginId}.category"
    $catPattern = [regex]::Escape("<unit id='$catId'")
    if ($contentXml -notmatch $catPattern) {
        $categoryUnit = @"
    <unit id='$PluginId.category' version='$catVersion' singleton='false'>
      <properties size='2'>
        <property name='org.eclipse.equinox.p2.name' value='NZT Tools'/>
        <property name='org.eclipse.equinox.p2.type.category' value='true'/>
      </properties>
      <provides size='1'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='$PluginId.category' version='$catVersion'/>
      </provides>
      <requires size='1'>
        <required namespace='org.eclipse.equinox.p2.iu' name='${FeatureId}.feature.group' range='[$PluginVersion,$PluginVersion]'/>
      </requires>
      <touchpoint id='null' version='0.0.0'/>
    </unit>

"@
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

# ---------- 9. Create p2.index ----------
@"
version=1
metadata.repository.factory.order= content.jar,content.xml.xz!
artifact.repository.factory.order= artifacts.jar,artifacts.xml.xz!
"@ | Set-Content (Join-Path $p2repoDir "p2.index") -Encoding Ascii

# ---------- 10. Create ZIP (forward slashes for p2 compatibility) ----------
$zipFile = Join-Path $OutDir "edt-tracing-plugin_$PluginVersion.zip"
if (Test-Path $zipFile) { Remove-Item $zipFile -Force }
Add-Type -AssemblyName System.IO.Compression
$zipStream = [System.IO.File]::Create($zipFile)
$zipArchive = [System.IO.Compression.ZipArchive]::new($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem -Recurse -File $p2repoDir | ForEach-Object {
        $entryName = $_.FullName.Substring($p2repoDir.Length + 1) -replace '\\', '/'
        $entry = $zipArchive.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Optimal)
        $entryStream = $entry.Open()
        try {
            $fileBytes = [System.IO.File]::ReadAllBytes($_.FullName)
            $entryStream.Write($fileBytes, 0, $fileBytes.Length)
        } finally { $entryStream.Dispose() }
    }
} finally { $zipArchive.Dispose(); $zipStream.Dispose() }
Write-Output "ZIP: $zipFile"

Write-Output "`n=== BUILD COMPLETE ==="
Write-Output "P2 repo: $p2repoDir"
Write-Output "Install via: Help -> Install New Software... -> Add -> Local -> $p2repoDir"
