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
# Tamanhos de 2 vCPU tentados em ordem (a capacidade de cada um varia por região)
VM_SIZES="${VM_SIZES:-${VM_SIZE:-} Standard_D2s_v3 Standard_B2s Standard_B2ms Standard_B2als_v2 Standard_B2as_v2 Standard_D2s_v3 Standard_D2as_v5 Standard_D2s_v5}"
RG="rg-futurevet-$RM"
VM="vm-agente-futurevet-$RM"
DIR="$(cd "$(dirname "$0")" && pwd)"

CRIADA=""
for VM_SIZE in $VM_SIZES; do
  echo ">> 1/2 Criando a VM $VM ($VM_SIZE, Ubuntu 22.04, sem portas de entrada)"
  if az vm create \
      --resource-group "$RG" \
      --name "$VM" \
      --location "$LOCATION" \
      --image Ubuntu2204 \
      --size "$VM_SIZE" \
      --admin-username azureuser \
      --generate-ssh-keys \
      --nsg-rule NONE \
      --public-ip-sku Standard \
      --output table --only-show-errors 2>/tmp/vm-erro.txt; then
    CRIADA="$VM_SIZE"; break
  fi
  echo "   $VM_SIZE indisponível: $(grep -o 'SkuNotAvailable\|QuotaExceeded\|RequestDisallowedByAzure' /tmp/vm-erro.txt | head -1). Tentando o próximo tamanho..."
done
[ -n "$CRIADA" ] || { echo "Nenhum tamanho disponível em $LOCATION. Rode com LOCATION=<outra região permitida>."; cat /tmp/vm-erro.txt | tail -5; exit 1; }

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
