@echo off
rem Double-click to build the portable EAShell (no Java needed on the target machine).
rem Needs JDK 17+ here, on the build machine. Output: target\dist\EAShell-<version>-portable.zip
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package.ps1" %*
if errorlevel 1 (
    echo.
    echo Build FAILED - see the messages above.
) else (
    explorer "%~dp0target\dist"
)
pause
