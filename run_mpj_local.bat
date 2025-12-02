@echo off
setlocal

REM Ruta de MPJ instalada
set "MPJ_HOME=C:\mpj\mpj-v0_44"

REM Rutas del proyecto
set "PROJ=%~dp0"
set "CLASSES=%PROJ%build\classes"
set "SOURCES=%PROJ%src"
set "LIB=%PROJ%lib"
set "JAVA_CP=%MPJ_HOME%\lib\starter.jar;%MPJ_HOME%\lib\mpj.jar"
set "APP_CP=%CLASSES%;%LIB%\jfreechart-1.0.19.jar;%LIB%\jcommon-1.0.23.jar"

REM Compilar si no existen clases
if not exist "%CLASSES%" (
  echo Compilando fuentes...
  javac -cp "%LIB%\*" -d "%CLASSES%" "%PROJ%src\*.java"
  if errorlevel 1 (
    echo Error al compilar
    pause
    exit /b 1
  )
)

echo Ejecutando EstacionSolarMPJ con MPJ (multicore)...
"%SystemRoot%\System32\cmd.exe" /c java -cp "%JAVA_CP%" -Djava.awt.headless=false -Dsun.java2d.noddraw=true runtime.starter.MPJRun -dev multicore -np 6 -cp "%APP_CP%" EstacionSolarMPJ drones

echo.
echo Finalizado.
pause
