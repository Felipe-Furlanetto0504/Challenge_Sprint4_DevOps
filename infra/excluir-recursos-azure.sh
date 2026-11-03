#!/usr/bin/env bash
# Remove todos os recursos do FutureVet na Azure (use só depois da correção, para não gastar créditos).
#   RM=562074 bash infra/excluir-recursos-azure.sh
set -euo pipefail
RM="${RM:-562074}"
az group delete --name "rg-futurevet-$RM" --yes --no-wait
echo "Exclusão de rg-futurevet-$RM solicitada."
