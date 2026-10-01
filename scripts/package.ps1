# Builds a self-contained, portable EAShell - no Java required on the target machine.
#
#   Double-click build-portable.cmd (rebuild) or run.cmd (build once, then launch), or:
#   powershell -ExecutionPolicy Bypass -File scripts/package.ps1
#
# Works on a clean Windows PC with nothing installed: if no JDK 17+ / Maven is found, they are
# downloaded (checksum-verified) into .tools\ inside the repo - no installer, no admin rights,
# no PATH or JAVA_HOME changes. Offline: drop a JDK 17+ .zip into .tools\ and it is used instead.
#
# Output:
#   dist/EAShell/                          - ready to run, data in dist/EAShell/data (kept on rebuild)
#   dist/EAShell-<version>-portable.zip    - the same, zipped, to copy to another PC
#   target/dist/...                        - build intermediates (wiped by mvn clean)
#
# Switches: -NoPortable (per-user data, no ZIP), -SkipTests, -NoInstall (don't touch dist/),
#           -LocalToolsOnly (ignore any installed JDK/Maven, use/download only .tools\ -
#           reproducible, and how the clean-machine path is tested on a dev box).
# See docs/PACKAGING.md for the full runbook.

param(
    [switch]$NoPortable,
    [switch]$SkipTests,
    [switch]$NoInstall,
    [switch]$LocalToolsOnly
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"   # Invoke-WebRequest is ~10x slower with the progress bar
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$tools = Join-Path $root ".tools"

$MavenVersion = "3.9.9"
$JdkFeature = 17

function Fail($message) {
    Write-Host ""
    Write-Host "ERROR: $message" -ForegroundColor Red
    exit 1
}

function Download($url, $file) {
    Write-Host "  downloading $url"
    New-Item -ItemType Directory -Force (Split-Path $file) | Out-Null
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $file
}

function Assert-Hash($file, $algorithm, $expected) {
    $actual = (Get-FileHash $file -Algorithm $algorithm).Hash
    if ($actual -ne $expected.Trim().ToUpper()) {
        Remove-Item $file -Force
        Fail "$algorithm checksum mismatch for $file - download corrupted or tampered with, deleted it. Re-run to retry."
    }
}

function Expand-Zip($zip, $dest) {
    Write-Host "  extracting $(Split-Path $zip -Leaf)"
    New-Item -ItemType Directory -Force $dest | Out-Null
    # tar.exe (bsdtar, built into Windows 10 1803+) is far faster than Expand-Archive on a JDK.
    if (Get-Command tar.exe -ErrorAction SilentlyContinue) {
        tar.exe -xf $zip -C $dest
        if ($LASTEXITCODE -eq 0) { return }
    }
    Expand-Archive $zip -DestinationPath $dest -Force
}

# ---------------------------------------------------------------- JDK

function Get-JdkMajor($jdkHome) {
    $release = Join-Path $jdkHome "release"
    if (-not (Test-Path $release)) { return 0 }
    $line = Select-String -Path $release -Pattern '^JAVA_VERSION="([0-9]+)' | Select-Object -First 1
    if (-not $line) { return 0 }
    return [int]$line.Matches[0].Groups[1].Value
}

function Test-Jdk($jdkHome) {
    return $jdkHome -and
        (Test-Path (Join-Path $jdkHome "bin\java.exe")) -and
        (Test-Path (Join-Path $jdkHome "bin\jpackage.exe")) -and
        ((Get-JdkMajor $jdkHome) -ge $JdkFeature)
}

function Find-Jdk {
    $candidates = @()
    if ($env:JAVA_HOME -and -not $LocalToolsOnly) { $candidates += $env:JAVA_HOME }

    # "java" on PATH is often a shim (Oracle's javapath) - ask it where it really lives.
    if ((Get-Command java -ErrorAction SilentlyContinue) -and -not $LocalToolsOnly) {
        $candidates += (cmd /c "java -XshowSettings:properties -version 2>&1" |
            Select-String "java.home" |
            ForEach-Object { ($_ -split "=", 2)[1].Trim() })
    }

    $candidates += Get-ChildItem $tools -Directory -Filter "jdk*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | ForEach-Object FullName

    $systemDirs = @()
    if (-not $LocalToolsOnly) {
        $systemDirs = @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium",
                        "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu",
                        "$env:ProgramFiles\BellSoft", "$env:ProgramFiles\Amazon Corretto")
    }
    foreach ($base in $systemDirs) {
        $candidates += Get-ChildItem $base -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | ForEach-Object FullName
    }

    foreach ($c in $candidates) {
        if (Test-Jdk $c) { return (Resolve-Path $c).Path }
    }
    return $null
}

function Install-Jdk {
    # A JDK .zip dropped into .tools\ by hand (offline machine) wins over downloading one.
    $local = Get-ChildItem $tools -File -Filter "*.zip" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "jdk" } | Select-Object -First 1
    if ($local) {
        Write-Host "Using JDK archive $($local.Name) from .tools\"
        Expand-Zip $local.FullName $tools
        return
    }

    Write-Host "No JDK $JdkFeature+ found - downloading Eclipse Temurin $JdkFeature (~190 MB) into .tools\ ..."
    $api = "https://api.adoptium.net/v3/assets/latest/$JdkFeature/hotspot?os=windows&architecture=x64&image_type=jdk&vendor=eclipse"
    $package = (Invoke-RestMethod -UseBasicParsing $api)[0].binary.package
    $zip = Join-Path $tools "downloads\$($package.name)"
    Download $package.link $zip
    Assert-Hash $zip "SHA256" $package.checksum
    Expand-Zip $zip $tools
    Remove-Item $zip -Force
}

# ---------------------------------------------------------------- Maven

function Find-Maven {
    $onPath = (Get-Command mvn -ErrorAction SilentlyContinue).Source
    if ($onPath -and -not $LocalToolsOnly) { return $onPath }

    $local = Get-ChildItem $tools -Directory -Filter "apache-maven-*" -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName "bin\mvn.cmd" } |
        Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($local) { return $local }

    # IDEs leave a Maven wrapper distribution here - see IMPROVEMENTS.md item 30.
    $wrapperDists = Join-Path $env:USERPROFILE ".m2\wrapper\dists"
    if ((Test-Path $wrapperDists) -and -not $LocalToolsOnly) {
        $wrapped = Get-ChildItem $wrapperDists -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue |
            Where-Object { $_.Directory.Name -eq "bin" } |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
        if ($wrapped) { return $wrapped }
    }
    return $null
}

function Install-Maven {
    Write-Host "Maven not found - downloading Apache Maven $MavenVersion (~9 MB) into .tools\ ..."
    $name = "apache-maven-$MavenVersion-bin.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/$name"
    $zip = Join-Path $tools "downloads\$name"
    Download $url $zip
    # The .sha512 file is "<hash>" or "<hash>  <filename>" - take the first token.
    $expected = ((Invoke-WebRequest -UseBasicParsing "$url.sha512").Content -split "\s+")[0]
    Assert-Hash $zip "SHA512" $expected
    Expand-Zip $zip $tools
    Remove-Item $zip -Force
}

# ---------------------------------------------------------------- tools

$jdk = Find-Jdk
if (-not $jdk) {
    try { Install-Jdk } catch { Fail "Could not get a JDK: $($_.Exception.Message)`nInstall JDK $JdkFeature+ (not a JRE - jpackage is needed), or put its .zip into .tools\ and re-run." }
    $jdk = Find-Jdk
    if (-not $jdk) { Fail "JDK was unpacked into .tools\ but no usable JDK $JdkFeature+ (with jpackage) was found there." }
}
Write-Host "Using JDK:   $jdk (Java $(Get-JdkMajor $jdk))"

$mvn = Find-Maven
if (-not $mvn) {
    try { Install-Maven } catch { Fail "Could not download Maven: $($_.Exception.Message)" }
    $mvn = Find-Maven
    if (-not $mvn) { Fail "Maven was unpacked into .tools\ but mvn.cmd was not found." }
}
Write-Host "Using Maven: $mvn"

# Only for this script's child processes - nothing is changed system-wide.
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"

# ---------------------------------------------------------------- build

$mvnArgs = @("clean", "package", "--batch-mode")
if ($SkipTests) { $mvnArgs += "-DskipTests" }
& $mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { Fail "Maven build failed (the first build needs internet to fetch dependencies)." }

# Read the version from pom.xml rather than hardcoding it here too - see
# IMPROVEMENTS.md item 17 on the version already being duplicated enough places.
[xml]$pom = Get-Content pom.xml
$version = $pom.project.version

& (Join-Path $jdk "bin\jpackage.exe") --type app-image `
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
if ($LASTEXITCODE -ne 0) { Fail "jpackage failed." }

$image = "target/dist/EAShell"

if (-not $NoPortable) {
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
    Compress-Archive -Path $image -DestinationPath $zip -Force
}

if ($NoInstall) {
    Write-Host ""
    Write-Host "Built $image/EAShell.exe"
    exit 0
}

# ---------------------------------------------------------------- install into dist/

# dist/ lives outside target/, so mvn clean can't wipe it - and the portable data folder
# (dist/EAShell/data) is preserved across rebuilds: everything else is replaced.
$dest = Join-Path $root "dist\EAShell"
New-Item -ItemType Directory -Force $dest | Out-Null
try {
    Get-ChildItem $dest | Where-Object { $_.Name -ne "data" } | Remove-Item -Recurse -Force
} catch {
    Fail "Could not replace dist\EAShell - close EAShell if it's running from there, then re-run."
}
Copy-Item "$image\*" $dest -Recurse -Force
if (-not $NoPortable) {
    Get-ChildItem (Join-Path $root "dist") -Filter "EAShell-*-portable.zip" | Remove-Item -Force
    Copy-Item $zip (Join-Path $root "dist") -Force
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green
Write-Host "  Run:       dist\EAShell\EAShell.exe  (or run.cmd)"
if (-not $NoPortable) {
    Write-Host "  Other PC:  copy dist\EAShell-$version-portable.zip, unzip anywhere, run EAShell\EAShell.exe - no Java needed."
}
