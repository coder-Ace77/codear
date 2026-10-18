#!/bin/bash
# (Re)creates the engine container from the latest image. The restart policy brings it back on every boot.
#   run-engine.sh               always recreate
#   run-engine.sh --if-changed  only when Docker Hub has a newer image (run at boot by engine-update.service, so a
#                               merge to main reaches the engine on its next wake-up)
set -euo pipefail
IMAGE=acecoder121/codear-microservices:engine

docker pull --quiet "$IMAGE" >/dev/null
if [ "${1:-}" = "--if-changed" ] && \
   [ "$(docker inspect --format '{{.Image}}' engine 2>/dev/null)" = "$(docker image inspect --format '{{.Id}}' "$IMAGE")" ]; then
    exit 0
fi

docker rm -f engine 2>/dev/null || true
# The engine starts sandboxes through the host's Docker socket and shares /tmp with them for the workspaces.
docker run -d --name engine --restart unless-stopped \
    -p 127.0.0.1:8081:8081 \
    --env-file /opt/codear-engine/.env \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /tmp:/tmp \
    "$IMAGE"
docker image prune -f >/dev/null
