@echo off
echo ================================================
echo   BuildMat Billing - Windows EXE Packager
echo ================================================
echo.
echo This script creates a standalone Windows .exe installer
echo from the jlink image. No Java needed on the target PC.
echo.
echo Requirements:
echo   - JDK 17+ with jpackage (included in JDK 14+)
echo   - WiX Toolset for .exe installer: https://wixtoolset.org
echo   - jlink image must be built first (run build-and-run.bat)
echo.

SET IMAGE_DIR=build\image
SET OUT_DIR=dist

IF NOT EXIST "%IMAGE_DIR%" (
    echo ERROR: jlink image not found at %IMAGE_DIR%
    echo Run build-and-run.bat first to build the image.
    pause
    exit /b 1
)

IF NOT EXIST %OUT_DIR% mkdir %OUT_DIR%

echo Packaging as Windows EXE installer...
echo.

jpackage ^
  --type exe ^
  --app-image "%IMAGE_DIR%" ^
  --name "BuildMat Billing" ^
  --app-version "1.0.0" ^
  --vendor "BuildMat Supplies" ^
  --description "Billing software for building material suppliers" ^
  --dest %OUT_DIR% ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "BuildMat" ^
  --win-dir-chooser

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo Packaging failed.
    echo If you see a WiX error, install WiX Toolset: https://wixtoolset.org/releases/
    echo.
    echo Alternative - create a portable app folder instead (no WiX needed):
    echo   xcopy /E /I build\image "dist\BuildMat Billing"
    echo   Then zip and distribute the dist\BuildMat Billing\ folder.
    pause
    exit /b 1
)

echo.
echo SUCCESS! Installer created in: %OUT_DIR%\
dir /b %OUT_DIR%\*.exe 2>nul
echo.
echo Distribute this .exe — it installs without needing Java on the target PC.
pause
