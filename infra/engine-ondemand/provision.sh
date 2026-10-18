#!/bin/bash
# Prepares a fresh Amazon Linux 2023 instance to run the Codear engine on demand. Run as root from a folder holding
# this directory's files plus Dockerfile.cpp and Dockerfile.python (java_src/engine/docker). Idempotent.
#
# Afterwards: put the engine's env file at /opt/codear-engine/.env, run run-engine.sh, then
# `systemctl enable --now engine-idle-shutdown.timer`. engine-update.service pulls a newer engine image on every boot.
set -euxo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"

dnf install -y docker
mkdir -p /etc/docker
# The judge raises the stack limit inside each sandbox (prlimit), which needs an unlimited hard limit. Amazon Linux
# starts dockerd with a 10 MB hard cap, so every run would fail with "failed to set the STACK resource limit".
# Its /etc/sysconfig/docker also passes --default-ulimit, and dockerd refuses to start when a setting comes from both
# a flag and daemon.json, so that nofile limit moves in here.
sed -i 's/^OPTIONS=.*/OPTIONS=""/' /etc/sysconfig/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" },
  "default-ulimits": {
    "nofile": { "Name": "nofile", "Soft": 32768, "Hard": 65536 },
    "stack": { "Name": "stack", "Soft": 8388608, "Hard": -1 }
  }
}
EOF
systemctl enable --now docker
usermod -aG docker ec2-user

# 1 GB RAM is tight for the JVM plus a sandbox; a small swap file absorbs the peaks.
if [ ! -f /swapfile ]; then
    dd if=/dev/zero of=/swapfile bs=1M count=1024
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap defaults 0 0' >> /etc/fstab
fi

# Sandbox images the engine runs submissions in (names come from Language.java).
docker build -f "$HERE/Dockerfile.cpp" -t codear-cpp:latest "$HERE"
docker build -f "$HERE/Dockerfile.python" -t codear-python:latest "$HERE"
docker pull acecoder121/codear-microservices:engine

install -d -m 700 /opt/codear-engine
install -m 755 "$HERE/run-engine.sh" /opt/codear-engine/run-engine.sh
install -m 755 "$HERE/engine-idle-shutdown.sh" /usr/local/bin/engine-idle-shutdown
install -m 644 "$HERE/engine-idle-shutdown.service" "$HERE/engine-idle-shutdown.timer" "$HERE/engine-update.service" \
    /etc/systemd/system/
systemctl daemon-reload
systemctl enable engine-update.service
touch /opt/codear-engine/.provisioned
