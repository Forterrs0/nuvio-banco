@echo off
setlocal
cd /d "%~dp0"
title Nuvia Banco - Servidor Local
echo ======================================
echo          NUVIA - BANCO LOCAL
 echo ======================================
where java >nul 2>&1
if errorlevel 1 (
  echo ERRO: Java nao encontrado. Instale JDK 21 e configure o PATH.
  pause
  exit /b 1
)
where mvn >nul 2>&1
if errorlevel 1 (
  echo ERRO: Maven nao encontrado. Instale Maven 3.9 e configure o PATH.
  pause
  exit /b 1
)
echo.
echo Acesse http://localhost:8080 quando o servidor iniciar.
echo Use Ctrl+C para desligar.
echo.
call mvn spring-boot:run
if errorlevel 1 (
  echo.
  echo Erro ao iniciar. Confira as mensagens acima.
  pause
  exit /b 1
)
endlocal
