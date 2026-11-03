#!/usr/bin/env bash
# =====================================================================================
#  FutureVet — criação dos recursos na Azure via Azure CLI (Sprint 4 · DevOps)
#
#  Cria: Grupo de Recursos -> Plano do App Service (Linux) -> Web App Java 17
#        + configurações da aplicação (perfil Oracle, porta, HTTPS, health check, logs)
#
#  Rode no Azure Cloud Shell (bash) ou em qualquer terminal com `az login` feito:
#     RM=562766 LOCATION=brazilsouth bash infra/criar-recursos-azure.sh
#
#  O banco é o Oracle da FIAP (PaaS aceito pela disciplina) — não é criado aqui.
#  Usuário/senha do banco e a chave JWT NÃO estão neste script: o pipeline de CD
#  aplica esses valores a partir do Variable Group protegido "futurevet-secrets".
# =====================================================================================
set -euo pipefail

RM="${RM:-562766}"                      # sufixo dos nomes (RM do Felipe, dono da assinatura Azure)
LOCATION="${LOCATION:-brazilsouth}"     # se a assinatura bloquear a região, use eastus
SKU="${SKU:-B1}"                        # B1 aguenta o Spring Boot; F1 é lento demais

RG="rg-futurevet-$RM"
PLAN="plan-futurevet-$RM"
APP="futurevet-$RM"

echo ">> 1/6 Grupo de recursos: $RG ($LOCATION)"
az group create --name "$RG" --location "$LOCATION" --output table

echo ">> 2/6 Plano do App Service (Linux, $SKU): $PLAN"
az appservice plan create \
  --resource-group "$RG" \
  --name "$PLAN" \
  --location "$LOCATION" \
  --is-linux \
  --sku "$SKU" \
  --output table

echo ">> 3/6 Web App Java 17 (Java SE): $APP"
az webapp create \
  --resource-group "$RG" \
  --plan "$PLAN" \
  --name "$APP" \
  --runtime "JAVA:17-java17" \
  --output table

echo ">> 4/6 Segurança e disponibilidade"
az webapp update --resource-group "$RG" --name "$APP" --https-only true --output none
az webapp config set \
  --resource-group "$RG" \
  --name "$APP" \
  --always-on true \
  --min-tls-version 1.2 \
  --ftps-state Disabled \
  --generic-configurations '{"healthCheckPath": "/actuator/health"}' \
  --output none

echo ">> 5/6 Configurações da aplicação (não sensíveis)"
az webapp config appsettings set \
  --resource-group "$RG" \
  --name "$APP" \
  --settings \
    SPRING_PROFILES_ACTIVE=oracle \
    SERVER_PORT=80 \
    PORT=80 \
    HTTPS_ENABLED=true \
    SERVER_FORWARD_HEADERS_STRATEGY=framework \
    DB_MIGRATIONS_ENABLED=true \
    DB_EXISTENTE=false \
    IA_URL=https://futurevet-ia.onrender.com \
    RABBITMQ_ENABLED=false \
    WEBSITES_CONTAINER_START_TIME_LIMIT=600 \
    JAVA_OPTS="-XX:MaxRAMPercentage=70.0" \
  --output none

echo ">> 6/6 Logs da aplicação (Log stream no portal)"
az webapp log config \
  --resource-group "$RG" \
  --name "$APP" \
  --docker-container-logging filesystem \
  --output none

echo
echo "Pronto. Recursos criados:"
az resource list --resource-group "$RG" --output table
echo
echo "URL da aplicação (fica no ar depois do primeiro CD): https://$APP.azurewebsites.net"
echo "Assinatura (para a service connection): $(az account show --query name -o tsv)"
