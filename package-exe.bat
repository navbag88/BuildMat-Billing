@echo off
setlocal enabledelayedexpansion
echo ================================================================
echo   BuildMat Billing Windows EXE Packager
echo ================================================================
echo.


    set "GRADLE_CMD=gradle"


echo.
echo Step 1: Building jlink self-contained runtime image...
echo (First run takes 2-3 minutes — subsequent runs are faster)
echo.
%GRADLE_CMD% jlink --info 2>&1 | findstr /i "error\|warning\|success\|FAILED\|BUILD"

if not exist build\image\bin\BuildMat-Billing.bat (
    echo.
    echo BUILD FAILED. Running again with full output for diagnosis:
    echo.
    %GRADLE_CMD% jlink
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo ── TROUBLESHOOTING ───────────────────────────────────────
        echo If you see "createMergedModule" error:
        echo   The forceMerge list in build.gradle may need updating.
        echo   Run: gradle dependencies ^> deps.txt
        echo   Then add any failing JAR name prefix to forceMerge in build.gradle
        echo.
        echo If you see "module not found" error:
        echo   Add the module name to addExtraDependencies in build.gradle
        echo.
        echo ── FALLBACK: Portable folder (no EXE, no WiX needed) ────
        echo Run this instead:
        echo   %GRADLE_CMD% jlink
        echo   xcopy /E /I build\image "dist\BuildMat Billing"
        echo   Then ZIP dist\BuildMat Billing\ and distribute.
        pause & exit /b 1
    )
)

echo.
echo jlink image built successfully!
echo.
echo Step 2: Creating Windows EXE installer...
echo.

if not exist dist mkdir dist

jpackage ^
  --type exe ^
  --app-image "build\image" ^
  --name "BuildMat Billing" ^
  --app-version "1.0.0" ^
  --vendor "BuildMat Supplies" ^
  --description "Billing software for building material suppliers" ^
  --dest dist ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "BuildMat" ^
  --win-dir-chooser

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo jpackage EXE creation failed.
    echo.
    echo Most likely cause: WiX Toolset is not installed.
    echo Install WiX 3.x from: https://wixtoolset.org/releases/
    echo (Restart CMD after installing WiX)
    echo.
    echo ── FALLBACK: Portable folder instead of EXE ────────────────
    echo Your jlink image is ready at build\image\
    echo To create a portable distribution:
    if not exist dist mkdir dist
    xcopy /E /I /Y "build\image" "dist\BuildMat Billing" >nul 2>&1
    echo.
    echo Created portable folder: dist\BuildMat Billing\
    echo Users can run: dist\BuildMat Billing\bin\BuildMat-Billing.bat
    echo ZIP this folder and distribute — no Java needed on target PC!
    pause & exit /b 0
)

echo.
echo ================================================================
echo   SUCCESS!
echo ================================================================
for /f %%f in ('dir /b dist\*.exe 2^>nul') do echo Installer: dist\%%f
echo.
echo Distribute this .exe — installs without Java on any Windows PC.
pause
