#!/usr/bin/env bash
# =====================================================================================
#  Roda DENTRO da VM (via az vm run-command, como root). Não execute no Cloud Shell.
#  Instala JDK 17, Maven e Azure CLI e registra a VM como agente self-hosted do
#  Azure DevOps no pool "Default", rodando como serviço (sobe sozinho após reboot).
#
#  Parâmetros: $1 = URL da organização  $2 = PAT (Agent Pools: Read & manage)
#              $3 = pool                 $4 = nome do agente
# =====================================================================================
set -euo pipefail
AZP_URL="$1"; AZP_TOKEN="$2"; AZP_POOL="${3:-Default}"; AGENT_NAME="${4:-agente-futurevet}"
export DEBIAN_FRONTEND=noninteractive HOME=/root

echo ">> Pacotes: JDK 17, Maven, utilitários"
apt-get update -y -q
apt-get install -y -q openjdk-17-jdk-headless maven curl git jq unzip ca-certificates

echo ">> Azure CLI"
command -v az >/dev/null || curl -sL https://aka.ms/InstallAzureCLIDeb | bash

echo ">> Swap de 2 GB (folga de memória para o build com testes)"
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo ">> Usuário do agente"
id azagent >/dev/null 2>&1 || useradd -m -s /bin/bash azagent

VER=$(curl -fsSL https://api.github.com/repos/microsoft/azure-pipelines-agent/releases/latest | jq -r '.tag_name' | sed 's/^v//' || true)
[ -z "${VER:-}" ] || [ "$VER" = "null" ] && VER=5.276.0
echo ">> Agente do Azure Pipelines $VER"
DIR=/home/azagent/agent
mkdir -p "$DIR"
curl -fsSL "https://download.agent.dev.azure.com/agent/${VER}/vsts-agent-linux-x64-${VER}.tar.gz" | tar -xz -C "$DIR"
chown -R azagent:azagent "$DIR"
"$DIR/bin/installdependencies.sh"

echo ">> Registrando no pool $AZP_POOL"
cd "$DIR"
sudo -u azagent ./config.sh --unattended \
  --url "$AZP_URL" --auth pat --token "$AZP_TOKEN" \
  --pool "$AZP_POOL" --agent "$AGENT_NAME" --replace --acceptTeeEula

# A task Maven@4 (jdkVersionOption 1.17) procura o JDK nesta variável
echo "JAVA_HOME_17_X64=/usr/lib/jvm/java-17-openjdk-amd64" >> "$DIR/.env"
echo "JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> "$DIR/.env"
chown azagent:azagent "$DIR/.env"

echo ">> Instalando como serviço"
./svc.sh install azagent
./svc.sh start
./svc.sh status | head -5
echo "AGENTE ONLINE: $AGENT_NAME"
