@echo off
rem Double-click to (re)build the portable EAShell into dist\ - see scripts\package.ps1.
rem Nothing needs to be installed first: a missing JDK / Maven is downloaded into .tools\.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package.ps1" %*
if errorlevel 1 (
    echo.
    echo Build FAILED - see the messages above.
) else (
    explorer "%~dp0dist"
)
pause
