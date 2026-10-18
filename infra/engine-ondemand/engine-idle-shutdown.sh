#!/bin/bash
# Stops this instance once the judge has had nothing to do for IDLE_MINUTES. Run every minute by
# engine-idle-shutdown.timer. The instance's shutdown behaviour is "stop", so the disk and sandbox images stay, and
# the problem service (or the engine-waker Lambda) starts it again when a submission arrives.
set -u
IDLE_MINUTES="${IDLE_MINUTES:-15}"

# Give a fresh boot time to pull the waiting messages before judging it idle.
uptime_seconds=$(cut -d. -f1 /proc/uptime)
(( uptime_seconds < IDLE_MINUTES * 60 )) && exit 0

# A sandbox that is still running means a submission is being judged right now.
[ -n "$(docker ps -q --filter label=codear.sandbox)" ] && exit 0

# Every submission and custom run starts at least one sandbox, so a recent start means recent work.
recent=$(timeout 10 docker events --since "${IDLE_MINUTES}m" --until "$(date +%s)" \
    --filter type=container --filter event=start --filter label=codear.sandbox | wc -l)
(( recent > 0 )) && exit 0

logger -t engine-idle-shutdown "no sandbox started for ${IDLE_MINUTES} minutes, stopping the instance"
/usr/sbin/shutdown -h now
