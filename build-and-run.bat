@echo off
setlocal enabledelayedexpansion
echo ================================================================
echo   BuildMat Billing — Build and Run
echo ================================================================
echo.

java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java JDK 17+ not found. Download from https://adoptium.net
    pause & exit /b 1
)

gradle -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if not exist gradlew.bat (
        echo ERROR: Gradle not found. Install from https://gradle.org
        pause & exit /b 1
    )
    set GRADLE_CMD=gradlew.bat
) else (
    set GRADLE_CMD=gradle
)

echo Building fat JAR...
%GRADLE_CMD% fatJar

if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED. Check errors above.
    pause & exit /b 1
)

echo.
echo Build successful! Copying JAR...
copy /y "build\libs\BuildMat-Billing-1.0.0-all.jar" "%~dp0" >nul 2>&1

echo Launching app...
call "%~dp0Run.bat"
