@echo off
setlocal
set "BASE=%~dp0"
set "WDIR=%BASE%build\classes"
set "CP=%WDIR%;%BASE%lib\mpj.jar;%BASE%lib\jfreechart-1.0.19.jar;%BASE%lib\jcommon-1.0.23.jar"

if not exist "%WDIR%" (
  echo No existe carpeta build\classes. Compilando primero...
  call "%BASE%run_swing.cmd"
  if errorlevel 1 (
    echo Error: no se pudo compilar. Revisa las librerias en %%BASE%%lib.
    exit /b 1
  )
)

rem mpj.conf preferido en el directorio actual (proyecto)
if not exist "%BASE%mpj.conf" (
  echo Creando mpj.conf basico en "%BASE%"...
  >"%BASE%mpj.conf" echo devices=multicore
)

if not defined MPJ_HOME set "MPJ_HOME=C:\mpj\mpj-v0_44"
if not exist "%MPJ_HOME%\bin\mpjrun.bat" (
  echo Error: No se encuentra "%MPJ_HOME%\bin\mpjrun.bat".
  echo Define MPJ_HOME correctamente o instala MPJ en C:\mpj\mpj-v0_44.
  exit /b 1
)

echo [Ejecutando MPJ]
rem IMPORTANTE: Evitar -wdir por espacios en ruta; usar el directorio actual
pushd "%BASE%" >nul
"%MPJ_HOME%\bin\mpjrun.bat" -dev multicore -np 6 -cp "%CP%" EstacionSolarMPJ
popd >nul
endlocal
