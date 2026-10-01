@echo off
rem Double-click to start EAShell. The first time, it builds dist\EAShell (a few minutes:
rem a missing JDK / Maven is downloaded into .tools\). Later runs start it straight away.
rem To pick up new code after a git pull, run build-portable.cmd.
if not exist "%~dp0dist\EAShell\EAShell.exe" (
    echo EAShell is not built yet - building it now, this happens only once...
    echo.
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package.ps1" -SkipTests
    if errorlevel 1 (
        echo.
        echo Build FAILED - see the messages above.
        pause
        exit /b 1
    )
)
start "" "%~dp0dist\EAShell\EAShell.exe"
