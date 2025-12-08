@echo off
REM Script para ejecutar las pruebas HTTP de la API SICC

echo ========================================
echo SUITE DE PRUEBAS HTTP - SICC API
echo ========================================
echo.

REM Verificar si Python está instalado
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Python no está instalado o no está en el PATH
    echo Por favor instala Python desde https://www.python.org/
    exit /b 1
)

REM Instalar requests si no está disponible
pip install requests >nul 2>&1

REM Ejecutar el script de pruebas
python test_api.py

pause

