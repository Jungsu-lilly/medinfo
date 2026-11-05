#!/bin/bash
# EC2 첫 부팅 때 한 번 실행 (Amazon Linux 2023). 로그: /var/log/cloud-init-output.log
# Terraform templatefile이 compose_version을 채운다
# shellcheck disable=SC2154
set -euxo pipefail

# Docker + compose 플러그인 (AL2023 저장소엔 compose가 없어서 GitHub 릴리스에서 받음)
dnf install -y docker
systemctl enable --now docker
mkdir -p /usr/local/lib/docker/cli-plugins
# 파일 이름 끝 = uname -m (x86_64 | aarch64)
curl -fsSL "https://github.com/docker/compose/releases/download/${compose_version}/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 스왑 2GB: Blue/Green 전환 중 두 쌍이 잠깐 같이 떠도 메모리 부족으로 죽지 않게
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# 배포 파일 자리 (compose.yml·스크립트는 배포 때마다 GitHub Actions가 내려받음)
mkdir -p /opt/medinfo
