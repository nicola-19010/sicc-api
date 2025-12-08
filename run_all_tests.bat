@echo off
setlocal
set JAVA_HOME=C:\Users\npach\.jdks\openjdk-25.0.1
cd /d C:\Users\npach\IdeaProjects\sicc\sicc-api

echo.
echo ========================================
echo Compilando y ejecutando tests...
echo ========================================
echo.

call mvnw.cmd test

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo Tests EXITOSOS
    echo ========================================
    echo.
) else (
    echo.
    echo ========================================
    echo Hubo errores en los tests
    echo ========================================
    echo.
)

endlocal
pause

