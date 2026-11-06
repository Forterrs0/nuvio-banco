@echo off
setlocal
cd /d "%~dp0"
title Nuvia - Java + MySQL
where java >nul 2>&1
if errorlevel 1 (
  echo ERRO: Java JDK 21 nao encontrado. Verifique o PATH.
  pause
  exit /b 1
)
where mvn >nul 2>&1
if errorlevel 1 (
  echo ERRO: Maven 3.9 nao encontrado. Verifique o PATH.
  pause
  exit /b 1
)
where powershell >nul 2>&1
if errorlevel 1 (
  echo ERRO: PowerShell nao encontrado.
  pause
  exit /b 1
)

echo ==============================================
echo       NUVIA - MySQL 8.x + Spring Boot
echo ==============================================
echo.
echo Antes: execute database\00_CRIAR_BANCO_MYSQL.sql no Workbench.
echo Certifique-se de que o servico MySQL esta ligado.
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0INICIAR_NUVIA.ps1"
if errorlevel 1 (
  echo.
  echo Falha ao iniciar. Confira usuario, senha e MySQL ligado.
  pause
  exit /b 1
)
endlocal
