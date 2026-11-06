@echo off
setlocal
cd /d "%~dp0"
title Nuvia Banco - Testes
echo Executando os testes automatizados Java...
call mvn test
echo.
if errorlevel 1 (
  echo ALGUM TESTE FALHOU ou Maven nao esta configurado.
) else (
  echo Testes concluidos com sucesso.
)
pause
endlocal
