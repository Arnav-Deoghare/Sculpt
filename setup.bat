@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Sculpt setup

set "PKG=com.arnav.sculpt"
set "GRADLE_VER=8.9"
set "DEVFILE=%~dp0.last-device.txt"
set "BOOTDIR=%USERPROFILE%\.sculpt-tools"
set "BOOTGRADLE=%BOOTDIR%\gradle-%GRADLE_VER%\bin\gradle.bat"

echo.
echo ==================== Sculpt setup ====================

rem ---------------------------------------------------------
echo.
echo [1/6] Checking Java ...
where java >nul 2>nul
if errorlevel 1 goto :nojava
java -version
echo (JDK 17 or newer is required.)

rem ---------------------------------------------------------
echo.
echo [2/6] Locating Android SDK ...
if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_HOME goto :nosdk
if not exist "%ANDROID_HOME%\platform-tools\adb.exe" goto :noadb
echo SDK: %ANDROID_HOME%
set "PATH=%ANDROID_HOME%\platform-tools;%PATH%"
set "SDKP=%ANDROID_HOME:\=/%"
>"%~dp0local.properties" echo sdk.dir=%SDKP%

set "SDKM=%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat"
if not exist "%SDKM%" goto :nosdkm
echo Accepting SDK licenses and making sure platform 35 is installed ...
(for /l %%i in (1,1,40) do @echo y) | call "%SDKM%" --licenses >nul 2>nul
(for /l %%i in (1,1,40) do @echo y) | call "%SDKM%" "platforms;android-35" "build-tools;35.0.0" >nul 2>nul
goto :gradle
:nosdkm
echo NOTE: cmdline-tools not found, skipping license/package check. Gradle will try to fetch what it needs.

rem ---------------------------------------------------------
:gradle
echo.
echo [3/6] Gradle ...
set "GRADLE_CMD="
if exist "gradle\wrapper\gradle-wrapper.jar" if exist "gradlew.bat" set "GRADLE_CMD=gradlew.bat"
if defined GRADLE_CMD goto :gradleok

if exist "%BOOTGRADLE%" goto :haveboot
echo Downloading Gradle %GRADLE_VER% once (kept in %BOOTDIR%) ...
mkdir "%BOOTDIR%" 2>nul
set "GZIP=%BOOTDIR%\gradle-%GRADLE_VER%-bin.zip"
powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VER%-bin.zip' -OutFile '%GZIP%'; Expand-Archive -Force '%GZIP%' '%BOOTDIR%'"
if errorlevel 1 goto :gradledlfail
if not exist "%BOOTGRADLE%" goto :gradledlfail

:haveboot
echo Generating Gradle wrapper ...
call "%BOOTGRADLE%" wrapper --gradle-version %GRADLE_VER% --no-validate-url --no-daemon
if errorlevel 1 goto :wrapperfail
if exist "gradlew.bat" set "GRADLE_CMD=gradlew.bat"
if defined GRADLE_CMD goto :gradleok

:wrapperfail
echo.
echo Wrapper generation failed. Using the downloaded Gradle directly instead.
set "GRADLE_CMD=%BOOTGRADLE%"

:gradleok
echo Using: %GRADLE_CMD%

rem ---------------------------------------------------------
echo.
echo [4/6] Connecting to your phone ...
adb start-server >nul 2>nul
call :hasdevice
if "%HAVE%"=="1" goto :deviceok

if not exist "%DEVFILE%" goto :askpair
set /p LASTDEV=<"%DEVFILE%"
echo Trying last phone at %LASTDEV% ...
adb connect %LASTDEV% >nul 2>nul
call :hasdevice
if "%HAVE%"=="1" goto :deviceok

:askpair
echo.
echo No phone connected. Wireless debugging setup:
echo   1. Phone: Developer options ^> Wireless debugging ^> turn ON.
echo   2. Tap "Pair device with pairing code" and note the IP:port and 6-digit code.
echo   3. Phone and PC must be on the same Wi-Fi.
echo.
set "PAIRADDR="
set /p PAIRADDR=Pairing IP:port (leave blank if already paired):
if "%PAIRADDR%"=="" goto :askconnect
set "PAIRCODE="
set /p PAIRCODE=Pairing code:
adb pair %PAIRADDR% %PAIRCODE%

:askconnect
echo.
echo Close the pairing dialog. On the main Wireless debugging screen, note the IP address and port shown there.
set "CONNADDR="
set /p CONNADDR=Connect IP:port:
if "%CONNADDR%"=="" goto :connfail
adb connect %CONNADDR%
call :hasdevice
if not "%HAVE%"=="1" goto :connfail
>"%DEVFILE%" echo %CONNADDR%

:deviceok
echo Phone connected.
adb devices

rem ---------------------------------------------------------
echo.
echo [5/6] Building and installing (first run downloads dependencies, be patient) ...
call "%GRADLE_CMD%" installDebug
if not errorlevel 1 goto :built
if /i "%GRADLE_CMD%"=="%BOOTGRADLE%" goto :fail
if not exist "%BOOTGRADLE%" goto :fail
echo.
echo Build via wrapper failed. Retrying once with the downloaded Gradle ...
call "%BOOTGRADLE%" installDebug
if errorlevel 1 goto :fail

:built
rem ---------------------------------------------------------
echo.
echo [6/6] Launching Sculpt ...
adb shell am start -n %PKG%/.MainActivity
where git >nul 2>nul
if errorlevel 1 goto :done
if exist ".git" goto :done
git init -q
echo Initialised a git repository.

:done
echo.
echo Done. Sculpt is installed on your phone.
echo Re-run setup.bat any time to rebuild and reinstall.
pause
exit /b 0

rem ---------------------------------------------------------
:hasdevice
set "HAVE=0"
for /f "skip=1 tokens=1,2" %%a in ('adb devices') do if "%%b"=="device" set "HAVE=1"
exit /b 0

:connfail
echo.
echo Could not connect. Common causes:
echo   - The connect port changes whenever Wireless debugging is toggled, so re-read it on the phone.
echo   - Router or guest Wi-Fi blocks device to device traffic. Try the phone hotspot.
echo   - Fallback: plug in USB with USB debugging on, then run this script again.
echo.
pause
goto :askpair

:gradledlfail
echo.
echo ERROR: Could not download or unpack Gradle %GRADLE_VER%. Check your internet, VPN or proxy and re-run.
goto :fail

:nojava
echo ERROR: Java not found. Install JDK 17 or newer and add it to PATH, then re-run.
goto :fail

:nosdk
echo ERROR: Android SDK not found. Set ANDROID_HOME to your SDK folder, then re-run.
goto :fail

:noadb
echo ERROR: platform-tools not found in %ANDROID_HOME%. Install "Android SDK Platform-Tools" and re-run.
goto :fail

:fail
echo.
echo Setup stopped. Fix the issue above and run setup.bat again.
pause
exit /b 1