@echo off
REM ==========================================================================
REM  FutureVet - demonstracao local completa (Windows)
REM  Sobe: servidor Eureka (8761) + microsservico de IA (8082) + aplicacao (8080)
REM  A chave da IA e lida do arquivo .env (linha IA_API_KEY=...), se existir.
REM  Sem RabbitMQ local, os avisos sao entregues direto (modo local).
REM ==========================================================================
setlocal
cd /d "%~dp0"
if exist .env for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"

if not exist target\futurevet.jar (
  echo Compilando a aplicacao principal...
  call mvnw.cmd -q -DskipTests package || goto erro
)
if not exist microsservicos\futurevet-ia\target\futurevet-ia.jar (
  echo Compilando o microsservico de IA...
  call mvnw.cmd -q -DskipTests -f microsservicos\futurevet-ia\pom.xml package || goto erro
)
if not exist microsservicos\servidor-eureka\target\servidor-eureka.jar (
  echo Compilando o servidor Eureka...
  call mvnw.cmd -q -DskipTests -f microsservicos\servidor-eureka\pom.xml package || goto erro
)

start "FutureVet - Eureka" java -jar microsservicos\servidor-eureka\target\servidor-eureka.jar
timeout /t 15 /nobreak >nul
start "FutureVet - IA" cmd /c "set EUREKA_ENABLED=true&& java -jar microsservicos\futurevet-ia\target\futurevet-ia.jar"
timeout /t 5 /nobreak >nul

echo.
echo FutureVet - demonstracao local. Acesse http://localhost:8080
echo Tutor: tutor@futurevet.local   Clinica: clinica@futurevet.local   Senha: Futurevet123!
echo Eureka: http://localhost:8761
echo.
set EUREKA_ENABLED=true
set IA_URL=
java -Dfile.encoding=UTF-8 -jar target\futurevet.jar --spring.profiles.active=demo
goto fim

:erro
echo.
echo *** Nao foi possivel compilar. Verifique se o Java 17+ esta instalado (java -version). ***
:fim
pause
