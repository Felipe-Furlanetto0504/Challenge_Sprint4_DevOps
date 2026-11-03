#!/usr/bin/env bash
# =====================================================================================
#  FutureVet — agente self-hosted do Azure Pipelines numa VM Linux da Azure
#
#  Organizações novas do Azure DevOps vêm sem agente gratuito da Microsoft
#  ("No hosted parallelism has been purchased or granted"). Este script cria uma
#  VM Ubuntu no mesmo grupo de recursos e a registra como agente no pool "Default".
#
#  Rode no Azure Cloud Shell (bash), na pasta do repositório:
#     AZP_URL=https://dev.azure.com/RM562766 AZP_TOKEN=<PAT> bash infra/criar-agente-vm.sh
#
#  PAT: Azure DevOps > User settings > Personal access tokens > New Token
#       escopo "Agent Pools: Read & manage", validade curta (7 dias).
#  A VM não aceita nenhuma conexão de entrada: o agente só faz chamadas de saída.
# =====================================================================================
set -euo pipefail
: "${AZP_URL:?Informe AZP_URL, ex.: https://dev.azure.com/RM562766}"
: "${AZP_TOKEN:?Informe AZP_TOKEN (PAT com escopo Agent Pools: Read & manage)}"

RM="${RM:-562766}"
LOCATION="${LOCATION:-eastus}"
VM_SIZE="${VM_SIZE:-Standard_B2s}"     # 2 vCPU / 4 GB — se faltar capacidade, use Standard_B2ms
RG="rg-futurevet-$RM"
VM="vm-agente-futurevet-$RM"
DIR="$(cd "$(dirname "$0")" && pwd)"

echo ">> 1/2 Criando a VM $VM ($VM_SIZE, Ubuntu 22.04, sem portas de entrada)"
az vm create \
  --resource-group "$RG" \
  --name "$VM" \
  --location "$LOCATION" \
  --image Ubuntu2204 \
  --size "$VM_SIZE" \
  --admin-username azureuser \
  --generate-ssh-keys \
  --nsg-rule NONE \
  --public-ip-sku Standard \
  --output table

echo ">> 2/2 Instalando JDK 17, Maven, Azure CLI e o agente (leva ~5 min)"
az vm run-command invoke \
  --resource-group "$RG" \
  --name "$VM" \
  --command-id RunShellScript \
  --scripts @"$DIR/agente-setup.sh" \
  --parameters "$AZP_URL" "$AZP_TOKEN" "Default" "agente-futurevet" \
  --query "value[0].message" -o tsv | tail -25

echo
echo "Confira em: Organization settings > Agent pools > Default > Agents (status Online)."
echo "Depois da gravação, para não gastar créditos:  az vm deallocate -g $RG -n $VM"
