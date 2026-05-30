#!/usr/bin/env pwsh
param(
    [Parameter(Mandatory=$true)]
    [string]$ModuleDir,
    [Parameter(Mandatory=$true)]
    [string]$PluginId,
    [string]$PluginLabel = "Sample Plugin",
    [string]$FeatureId = "$PluginId.feature",
    [string]$OutDir,
    [string]$SrcDir
)
$ErrorActionPreference = "Stop"
$PluginDir = Split-Path -Parent $PSCommandPath
if (-not $SrcDir) { $SrcDir = Join-Path $PluginDir "src" }
if (-not $OutDir) { $OutDir = Join-Path $PluginDir "dist" }
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
        if ($dirs) { $edtHome = Join-Path $base $dirs[0]; break }
    }
}
if (-not $edtHome) { $edtHome = $env:EDT_HOME }
if (-not $edtHome -or -not (Test-Path $edtHome)) { Write-Error "EDT not found"; exit 1 }
$pluginsDir = Join-Path $edtHome "plugins"

# ---------- 2. Classpath ----------
$requiredPatterns = @(
    "org.eclipse.osgi_*.jar",
    "org.eclipse.ui.workbench_*.jar",
    "org.eclipse.jface_*.jar",
    "org.eclipse.swt.win32.win32.x86_64_*.jar",
    "org.eclipse.core.runtime_*.jar",
    "org.eclipse.core.commands_*.jar",
    "org.eclipse.core.expressions_*.jar",
    "org.eclipse.e4.ui.workbench_*.jar",
    "org.eclipse.e4.ui.services_*.jar",
    "org.eclipse.e4.core.services_*.jar",
    "org.eclipse.e4.core.di_*.jar",
    "org.eclipse.ui_*.jar",
    "org.eclipse.core.jobs_*.jar",
    "org.eclipse.equinox.common_*.jar",
    "org.eclipse.equinox.registry_*.jar",
    "org.eclipse.osgi.services_*.jar"
)
$classpathJars = @()
foreach ($pat in $requiredPatterns) {
    $found = Get-ChildItem (Join-Path $pluginsDir $pat) -Name | Select-Object -First 1
    if ($found) { $classpathJars += Join-Path $pluginsDir $found }
}
$classpath = $classpathJars -join ";"

# ---------- 3. Compile ----------
if (Test-Path $TargetDir) { Remove-Item $TargetDir -Recurse -Force }
$classesOut = Join-Path $TargetDir "classes"
New-Item -ItemType Directory -Path $classesOut -Force | Out-Null

$javaHome = $env:JAVA_HOME
if (-not $javaHome) { $javaHome = "C:\Program Files\BellSoft\LibericaJDK-17" }
$javac = Join-Path $javaHome "bin\javac"
$javaFiles = Get-ChildItem $SrcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $javac --release 8 -cp $classpath -d $classesOut -sourcepath $SrcDir $javaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output "Compilation OK"

# ---------- 4. JAR ----------
$jarStage = Join-Path $TargetDir "jar-stage"
New-Item -ItemType Directory -Path $jarStage -Force | Out-Null
Copy-Item "$classesOut\*" $jarStage -Recurse -Force
$metaInfModule = Join-Path $ModuleDir "META-INF"
$metaInfStage = Join-Path $jarStage "META-INF"
New-Item -ItemType Directory -Path $metaInfStage -Force | Out-Null
# Copy only MANIFEST.MF and messages*.properties to META-INF/
Copy-Item (Join-Path $metaInfModule "MANIFEST.MF") $metaInfStage -Force
if (Test-Path "$metaInfModule\messages*.properties") { Copy-Item "$metaInfModule\messages*.properties" $metaInfStage -Force }
# Copy plugin.xml to JAR root (from META-INF/ or module root)
if (Test-Path (Join-Path $ModuleDir "plugin.xml")) { Copy-Item (Join-Path $ModuleDir "plugin.xml") $jarStage -Force }
elseif (Test-Path (Join-Path $metaInfModule "plugin.xml")) { Copy-Item (Join-Path $metaInfModule "plugin.xml") $jarStage -Force }
# Copy fragment.e4xmi from module root to JAR root if present
if (Test-Path (Join-Path $ModuleDir "fragment.e4xmi")) { Copy-Item (Join-Path $ModuleDir "fragment.e4xmi") $jarStage -Force }
$jarFile = Join-Path $TargetDir "$PluginId.jar"
Set-Location $jarStage
& "$($javaHome)\bin\jar" cfm $jarFile "META-INF\MANIFEST.MF" .
Set-Location $PluginDir
Write-Output "JAR: $jarFile ($((Get-Item $jarFile).Length) bytes)"

# ---------- 5. Feature JAR ----------
$featureDir = Join-Path $TargetDir "feature"
New-Item -ItemType Directory -Path $featureDir -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $featureDir "META-INF") -Force | Out-Null
@"
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: $PluginLabel Feature
Bundle-SymbolicName: $FeatureId;singleton:=true
Bundle-Version: 1.0.0
Bundle-Vendor: 1C
"@ | Set-Content (Join-Path $featureDir "META-INF\MANIFEST.MF") -Encoding Ascii
@"
<?xml version="1.0" encoding="UTF-8"?>
<feature id="$FeatureId" label="$PluginLabel" version="1.0.0" provider-name="1C">
<plugin id="$PluginId" download-size="18" install-size="36" version="1.0.0" unpack="false"/>
</feature>
"@ | Set-Content (Join-Path $featureDir "feature.xml") -Encoding Utf8
$featureJar = Join-Path $TargetDir "$FeatureId.jar"
Set-Location $featureDir
& "$($javaHome)\bin\jar" cfm $featureJar "META-INF\MANIFEST.MF" feature.xml
Set-Location $PluginDir
Write-Output "Feature JAR: $featureJar ($((Get-Item $featureJar).Length) bytes)"

# ---------- 6. P2 repo ----------
$p2repoDir = Join-Path $OutDir "p2repo"
if (Test-Path $p2repoDir) { Remove-Item $p2repoDir -Recurse -Force }
$1cedtc = Get-ChildItem $edtHome -Recurse -Filter "1cedtc.exe" | Select-Object -First 1 -ExpandProperty FullName
if (-not $1cedtc) { Write-Error "1cedtc.exe not found"; exit 1 }
$p2PluginsDir = Join-Path $p2repoDir "plugins"
$p2FeaturesDir = Join-Path $p2repoDir "features"
New-Item -ItemType Directory -Path $p2PluginsDir -Force | Out-Null
New-Item -ItemType Directory -Path $p2FeaturesDir -Force | Out-Null
Copy-Item $jarFile $p2PluginsDir
Copy-Item $featureJar $p2FeaturesDir
$p2repoUri = "file:/$($p2repoDir -replace '\\', '/')"
Write-Output "Running FeaturesAndBundlesPublisher..."
& $1cedtc -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher `
    -source "$p2repoDir" -metadataRepository "$p2repoUri" -artifactRepository "$p2repoUri" -publishArtifacts -compress
if ($LASTEXITCODE -ne 0) { Write-Warning "Publisher returned $LASTEXITCODE" }

# ---------- 7. Add plugin IU if missing ----------
$contentJar = Join-Path $p2repoDir "content.jar"
if (Test-Path $contentJar) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $tmp = Join-Path $TargetDir "content_extract"
    New-Item -ItemType Directory -Path $tmp -Force | Out-Null
    $z = [System.IO.Compression.ZipFile]::OpenRead($contentJar)
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($z.Entries[0], (Join-Path $tmp "content.xml"), $true)
    $z.Dispose()
    $contentXml = Get-Content (Join-Path $tmp "content.xml") -Raw
    if ($contentXml -notmatch "<unit id='$PluginId'") {
        Write-Output "Injecting plugin IU for $PluginId..."
        $manifestContent = Get-Content (Join-Path $ModuleDir "META-INF\MANIFEST.MF") -Raw
        $manifestContent = $manifestContent -replace "`r", "" -replace "`n", "`n"
        $pluginUnit = @"
    <unit id='$PluginId' version='1.0.0' singleton='false'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='$PluginLabel'/>
        <property name='org.eclipse.equinox.p2.type' value='Bundle'/>
      </properties>
      <provides size='2'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='$PluginId' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='$PluginId' version='1.0.0'/>
      </provides>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
$manifestContent
          </instruction>
        </instructions>
      </touchpointData>
    </unit>

"@
        $sizeMatch = [regex]::Match($contentXml, "<units size='(\d+)'")
        if ($sizeMatch.Success) {
            $oldSize = [int]$sizeMatch.Groups[1].Value
            $contentXml = $contentXml -replace "<units size='$oldSize'>", "<units size='$($oldSize + 1)'>"
        }
        $contentXml = $contentXml -replace '</units>', "$pluginUnit</units>"
    }
    $contentXml = $contentXml -replace '\s*<unit id=''edt-tracing[^'']*''.*?</unit>\s*', "`n"
    Set-Content (Join-Path $tmp "content.xml") $contentXml -NoNewline
    Remove-Item $contentJar -Force
    Set-Location $tmp
    & "$($javaHome)\bin\jar" cfM $contentJar "content.xml"
    Set-Location $PluginDir
    Remove-Item $tmp -Recurse -Force
}

# ---------- 8. p2.index ----------
@"
version=1
metadata.repository.factory.order= content.jar,content.xml.xz!
artifact.repository.factory.order= artifacts.jar,artifacts.xml.xz!
"@ | Set-Content (Join-Path $p2repoDir "p2.index") -Encoding Ascii

Write-Output "`n=== BUILD COMPLETE ==="
Write-Output "P2 repo: $p2repoDir"
