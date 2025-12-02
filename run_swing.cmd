@echo off
setlocal
set "BASE=%~dp0"
set "SRC=%BASE%src"
set "LIB=%BASE%lib"
set "BUILD=%BASE%build\classes"

if not exist "%BUILD%" mkdir "%BUILD%"

echo [Compilando]
javac -cp "%LIB%\jfreechart-1.0.19.jar;%LIB%\jcommon-1.0.23.jar;%LIB%\mpj.jar" -d "%BUILD%" "%SRC%\*.java"
if errorlevel 1 (
  echo Error al compilar
  exit /b 1
)

echo [Ejecutando Swing]
java -cp "%BUILD%;%LIB%\jfreechart-1.0.19.jar;%LIB%\jcommon-1.0.23.jar" PtoyectoPCyPoto2025
endlocal

