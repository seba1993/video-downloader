@echo off
setlocal

set "APP_ROOT=%~dp0"
cd /d "%APP_ROOT%"

set "PATH=%APP_ROOT%tools;%APP_ROOT%youtube;%PATH%"

if exist "%APP_ROOT%runtime\bin\javaw.exe" (
  set "JAVA_CMD=%APP_ROOT%runtime\bin\javaw.exe"
) else (
  where javaw.exe >nul 2>&1
  if errorlevel 1 (
    echo No se encontro Java. Instale Java o copie la carpeta runtime de la version anterior.
    pause
    exit /b 1
  )
  set "JAVA_CMD=javaw.exe"
)

if not exist "%APP_ROOT%Downloader.jar" (
  echo No se encontro "%APP_ROOT%Downloader.jar"
  pause
  exit /b 1
)

start "" "%JAVA_CMD%" --add-opens=java.base/java.lang=ALL-UNNAMED -jar "%APP_ROOT%Downloader.jar"
exit /b 0
