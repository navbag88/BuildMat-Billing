@echo off
setlocal enabledelayedexpansion

set "APP_DIR=./build/libs/"
set "APP_JAR=%APP_DIR%BuildMat-Billing-1.0.0-all.jar"
set "FX_DIR=%APP_DIR%javafx-sdk"
set "FX_LIB=%FX_DIR%\lib"
set "FX_ZIP=%APP_DIR%javafx-sdk.zip"
set "FX_URL=https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_windows-x64_bin-sdk.zip"
set "FX_TEMP=%APP_DIR%fx_temp"

echo ================================================
echo   BuildMat Billing
echo ================================================
echo.

REM ── Check Java ───────────────────────────────────
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java 17+ not found.
    echo Download from: https://adoptium.net
    echo.
    pause
    exit /b 1
)

REM ── Check JAR exists ─────────────────────────────
if not exist "%APP_JAR%" (
    echo ERROR: JAR not found at:
    echo %APP_JAR%
    echo.
    echo Run build-and-run.bat first to build the app.
    pause
    exit /b 1
)

REM ── Download JavaFX if not present ───────────────
if not exist "%FX_LIB%" (
    echo JavaFX SDK not found. Downloading automatically...
    echo This only happens once ^(~70 MB^).
    echo.

    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%FX_URL%' -OutFile '%FX_ZIP%' -UseBasicParsing"

    if not exist "%FX_ZIP%" (
        echo.
        echo Auto-download failed. Please download manually:
        echo.
        echo  1. Go to: https://gluonhq.com/products/javafx/
        echo  2. Select: Version 21, Windows, SDK
        echo  3. Extract the ZIP
        echo  4. Rename the extracted folder to: javafx-sdk
        echo  5. Place it here: %APP_DIR%
        echo.
        pause
        exit /b 1
    )

    echo Extracting...
    powershell -Command "Expand-Archive -Path '%FX_ZIP%' -DestinationPath '%FX_TEMP%' -Force"

    for /d %%i in ("%FX_TEMP%\javafx-sdk-*") do (
        move "%%i" "%FX_DIR%"
    )

    if exist "%FX_ZIP%" del "%FX_ZIP%"
    if exist "%FX_TEMP%" rmdir /s /q "%FX_TEMP%"

    if not exist "%FX_LIB%" (
        echo Extraction failed. Please manually place the JavaFX SDK folder
        echo at: %FX_DIR%
        echo.
        pause
        exit /b 1
    )

    echo JavaFX SDK ready!
    echo.
)

REM ── Launch app (with required --add-opens for WebView + fat JAR) ──────────
echo Starting BuildMat Billing...
java ^
  --module-path "%FX_LIB%" ^
  --add-modules javafx.controls,javafx.graphics,javafx.fxml,javafx.web ^
  --add-opens javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED ^
  --add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED ^
  --add-opens javafx.graphics/com.sun.javafx.scene.text=ALL-UNNAMED ^
  --add-opens javafx.graphics/com.sun.javafx.css=ALL-UNNAMED ^
  --add-opens javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED ^
  --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED ^
  --add-opens javafx.base/com.sun.javafx.reflect=ALL-UNNAMED ^
  --add-opens javafx.base/com.sun.javafx.beans=ALL-UNNAMED ^
  --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED ^
  --add-opens javafx.web/com.sun.javafx.webkit=ALL-UNNAMED ^
  -jar "%APP_JAR%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo App closed with an error. See output above.
    pause
)
