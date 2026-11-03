#!/usr/bin/env sh
# FutureVet - demonstração local completa (Linux/macOS): Eureka + IA + aplicação.
cd "$(dirname "$0")" || exit 1
[ -f .env ] && set -a && . ./.env && set +a
[ -f target/futurevet.jar ] || ./mvnw -q -DskipTests package || exit 1
[ -f microsservicos/futurevet-ia/target/futurevet-ia.jar ] || ./mvnw -q -DskipTests -f microsservicos/futurevet-ia/pom.xml package || exit 1
[ -f microsservicos/servidor-eureka/target/servidor-eureka.jar ] || ./mvnw -q -DskipTests -f microsservicos/servidor-eureka/pom.xml package || exit 1
java -jar microsservicos/servidor-eureka/target/servidor-eureka.jar > eureka.log 2>&1 &
EUREKA_PID=$!
sleep 15
EUREKA_ENABLED=true java -jar microsservicos/futurevet-ia/target/futurevet-ia.jar > ia.log 2>&1 &
IA_PID=$!
trap 'kill $EUREKA_PID $IA_PID 2>/dev/null' EXIT INT TERM
echo "FutureVet: http://localhost:8080  (tutor@futurevet.local / clinica@futurevet.local — senha Futurevet123!)"
EUREKA_ENABLED=true IA_URL= java -Dfile.encoding=UTF-8 -jar target/futurevet.jar --spring.profiles.active=demo
