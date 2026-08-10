# Builds a self-contained EAShell app-image - no Java required on the target machine.
# Run from the repository root: powershell -File scripts/package.ps1
#
# Output: target/dist/EAShell/EAShell.exe - copy the whole EAShell folder to distribute it
# (it carries its own trimmed JRE). See docs/PACKAGING.md for the full runbook and the
# optional MSI installer path (requires WiX Toolset, not covered by this script).

$ErrorActionPreference = "Stop"

mvn clean package
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

Write-Host "Built target/dist/EAShell/EAShell.exe - copy the whole EAShell folder to distribute it."
