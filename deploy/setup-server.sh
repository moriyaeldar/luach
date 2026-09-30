#!/usr/bin/env bash
# One-time setup of a fresh Ubuntu server (AWS EC2, Oracle Cloud, or any VPS; x86 or ARM).
# Installs Docker, opens ports 80/443, clones the repo, writes .env with random passwords and starts Luach.
#
#   curl -fsSL https://raw.githubusercontent.com/moriyaeldar/luach/main/deploy/setup-server.sh | bash
set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/moriyaeldar/luach.git}"
APP_DIR="${APP_DIR:-$HOME/luach}"

echo "==> Installing Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

echo "==> Opening ports 80 and 443 in the server firewall"
# Oracle's Ubuntu images ship iptables rules that reject everything except SSH; AWS and most others don't.
# Either way, the cloud provider's firewall (security list / security group) must allow 80 and 443 too.
for port in 80 443; do
  if ! sudo iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null; then
    sudo iptables -I INPUT 1 -p tcp -m state --state NEW --dport "$port" -j ACCEPT
  fi
done
if command -v netfilter-persistent >/dev/null; then
  sudo netfilter-persistent save
fi

echo "==> Getting the code"
if [ -d "$APP_DIR/.git" ]; then
  git -C "$APP_DIR" pull --ff-only
else
  git clone "$REPO_URL" "$APP_DIR"
fi
cd "$APP_DIR"

if [ ! -f .env ]; then
  echo "==> Writing .env"
  public_ip="$(curl -fsS https://api.ipify.org)"
  cat > .env <<ENV
DOMAIN=${DOMAIN:-${public_ip//./-}.sslip.io}
POSTGRES_PASSWORD=$(openssl rand -hex 16)
TASKS_DB_PASSWORD=$(openssl rand -hex 16)
ENV
  chmod 600 .env
fi

echo "==> Building and starting (the first build takes several minutes)"
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build

echo
echo "Luach is starting at: https://$(grep '^DOMAIN=' .env | cut -d= -f2)"
