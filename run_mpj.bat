@echo off
echo ========================================
echo Ejecutando Estación Solar con MPJ (Paralelo)
echo ========================================

REM Configurar variables
cd src
set MPJ_HOME=..\lib
set CLASSPATH=.;..\lib\jfreechart-1.0.19.jar;..\lib\jcommon-1.0.23.jar;..\lib\mpj.jar

REM Compilar todas las clases
echo Compilando clases...
javac -cp "%CLASSPATH%" *.java

if %errorlevel% neq 0 (
    echo Error al compilar
    pause
    exit /b 1
)

echo.
echo Clases compiladas exitosamente
echo.

REM Ejecutar con MPJ
echo Iniciando ejecución paralela con MPJ...
echo Ejecutando 4 procesos (1 maestro + 3 trabajadores)
echo.

java -cp "%CLASSPATH%" -Djava.library.path=..\lib org.mpj.runtime.Starter mpj.conf EstacionSolarMPJ

echo.
echo Ejecución completada
echo ========================================
pause