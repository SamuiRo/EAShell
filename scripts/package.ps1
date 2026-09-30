# Builds a self-contained, portable EAShell - no Java required on the target machine.
# Run from the repository root: powershell -ExecutionPolicy Bypass -File scripts/package.ps1
#
# Output:
#   target/dist/EAShell/                      - app-image (EAShell.exe + app/ + trimmed runtime/)
#   target/dist/EAShell-<version>-portable.zip - the same folder, zipped, ready to hand out
#
# Both are in portable mode: a portable.txt next to EAShell.exe makes the app keep its data
# in EAShell/data/ instead of %USERPROFILE%\.eashell, so the folder can live on a USB stick.
# Pass -NoPortable to build a plain image that stores data per user instead.
# See docs/PACKAGING.md for the full runbook and the (not yet built) MSI installer path.

param(
    [switch]$NoPortable,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

# Maven: prefer PATH, fall back to a Maven wrapper distribution already downloaded under
# ~/.m2/wrapper/dists (IDEs leave one there) - see IMPROVEMENTS.md item 30.
$mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
    $wrapperDists = Join-Path $env:USERPROFILE ".m2\wrapper\dists"
    if (Test-Path $wrapperDists) {
        $mvn = Get-ChildItem $wrapperDists -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue |
            Where-Object { $_.Directory.Name -eq "bin" } |
            Sort-Object FullName -Descending |
            Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $mvn) {
    Write-Error "Maven not found - neither 'mvn' on PATH nor a wrapper distribution under ~/.m2/wrapper/dists. Install Maven 3.8+ and retry."
    exit 1
}
Write-Host "Using Maven: $mvn"

$mvnArgs = @("clean", "package")
if ($SkipTests) { $mvnArgs += "-DskipTests" }
& $mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { exit 1 }

# Read the version from pom.xml rather than hardcoding it here too - see
# IMPROVEMENTS.md item 17 on the version already being duplicated enough places.
[xml]$pom = Get-Content pom.xml
$version = $pom.project.version

# jpackage ships alongside java itself but isn't always on PATH - resolve it relative to
# whichever "java" is actually running, rather than assuming a fixed install path.
# (Redirect via cmd /c, not PowerShell's own 2>&1 - PowerShell 5.1 wraps a native command's
# stderr lines in terminating ErrorRecords when $ErrorActionPreference is Stop.)
$javaHome = (cmd /c "java -XshowSettings:properties -version 2>&1" |
    Select-String "java.home" |
    ForEach-Object { ($_ -split "=", 2)[1].Trim() })
$jpackage = Join-Path $javaHome "bin\jpackage.exe"

if (-not (Test-Path $jpackage)) {
    Write-Error "jpackage.exe not found at $jpackage - need JDK 17+, not just a JRE."
    exit 1
}

& $jpackage --type app-image `
    --name EAShell `
    --app-version $version `
    --input target/libs `
    --main-jar "EAShell-$version.jar" `
    --main-class com.eashell.Launcher `
    --icon src/main/resources/app.ico `
    --dest target/dist `
    --vendor "StarLith" `
    --copyright "Copyright (c) 2025" `
    --add-modules java.base,java.desktop,java.logging,java.scripting,jdk.unsupported

if ($LASTEXITCODE -ne 0) { exit 1 }

$image = "target/dist/EAShell"

if ($NoPortable) {
    Write-Host "Built $image/EAShell.exe (per-user data in %USERPROFILE%\.eashell)."
    exit 0
}

# The marker's name is AppPaths.PORTABLE_MARKER - keep the two in sync.
@"
EAShell portable mode.

While this file sits next to EAShell.exe, scripts are stored in the 'data' folder
beside it, so the whole EAShell folder can be moved or carried on a USB stick.

Delete this file to store scripts per user in %USERPROFILE%\.eashell instead.
The folder must be writable (not under C:\Program Files); otherwise EAShell
falls back to %USERPROFILE%\.eashell automatically.
"@ | Set-Content -Encoding UTF8 (Join-Path $image "portable.txt")

$zip = "target/dist/EAShell-$version-portable.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path $image -DestinationPath $zip

Write-Host ""
Write-Host "Built $image/EAShell.exe (portable - data in $image/data)."
Write-Host "Portable ZIP: $zip - unzip anywhere and run EAShell\EAShell.exe, no Java needed."
