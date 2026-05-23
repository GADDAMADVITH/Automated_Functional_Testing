@echo off
setlocal enabledelayedexpansion

set "AUTOMATION_DIR=%~dp0"
set "SRC_DIR=%AUTOMATION_DIR%src"

if not exist "%SRC_DIR%" (
  echo Cannot find src folder: "%SRC_DIR%"
  exit /b 1
)

rem Ensure chromedriver.exe is discoverable (fallback for environments where Selenium Manager is blocked)
if exist "%AUTOMATION_DIR%chromedriver-win64\chromedriver.exe" (
  set "PATH=%AUTOMATION_DIR%chromedriver-win64;%PATH%"
)

pushd "%SRC_DIR%"
echo Compiling LaunchTest.java...
javac -cp ".;..\lib\*" LaunchTest.java
if errorlevel 1 (
  popd
  exit /b 1
)

echo Running LaunchTest...
java -cp ".;..\lib\*" LaunchTest
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%

