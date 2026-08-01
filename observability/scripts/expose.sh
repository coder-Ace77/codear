#!/usr/bin/env bash

set -euo pipefail

SERVICES=(engine problem user drive)

dir_for() {
    case "$1" in
        engine)  echo "$HOME/codear-engine" ;;
        problem) echo "$HOME/codear-problem" ;;
        user)    echo "$HOME/codear-user" ;;
        drive)   echo "$HOME/drive" ;;
        *)       return 1 ;;
    esac
}

port_for() {
    case "$1" in
        engine)  echo 8081 ;;
        problem) echo 8082 ;;
        user)    echo 8083 ;;
        drive)   echo 8000 ;;
        *)       return 1 ;;
    esac
}

compose_file() {
    local dir
    dir=$(dir_for "$1") || return 1
    echo "$dir/docker-compose.yaml"
}

die() { echo "error: $*" >&2; exit 1; }

validate() {
    local svc=$1
    dir_for "$svc" >/dev/null 2>&1 || die "unknown service '$svc' (known: ${SERVICES[*]})"
    [ -f "$(compose_file "$svc")" ] || die "no compose file at $(compose_file "$svc")"
}

# Current bind address as written in the compose file: 0.0.0.0, 127.0.0.1, or
# "unset" for a bare "PORT:PORT" mapping, which Docker treats as 0.0.0.0.
current_bind() {
    local file port line
    file=$(compose_file "$1")
    port=$(port_for "$1")
    line=$(grep -E "^[[:space:]]*-[[:space:]]*\"?([0-9.]+:)?${port}:${port}\"?" "$file" | head -1) \
        || { echo "unknown"; return; }
    if [[ "$line" =~ ([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+):${port}:${port} ]]; then
        echo "${BASH_REMATCH[1]}"
    else
        echo "unset"
    fi
}

set_bind() {
    local svc=$1 bind=$2 file port
    file=$(compose_file "$svc")
    port=$(port_for "$svc")

    [ -f "$file.bak" ] || cp "$file" "$file.bak"

    sed -i -E \
        "s|^([[:space:]]*-[[:space:]]*)\"?([0-9.]+:)?${port}:${port}\"?[[:space:]]*$|\1\"${bind}:${port}:${port}\"|" \
        "$file"

    [ "$(current_bind "$svc")" = "$bind" ] || die "could not rewrite the port line in $file (restore from $file.bak)"

    ( cd "$(dir_for "$svc")" && docker compose up -d )
}

cancel_timer() {
    local svc=$1 pidfile="/tmp/expose-${svc}.pid"
    if [ -f "$pidfile" ]; then
        kill "$(cat "$pidfile")" 2>/dev/null || true
        rm -f "$pidfile"
    fi
}

cmd_status() {
    printf '%-9s %-8s %-12s %s\n' SERVICE PORT BIND REACHABLE
    for svc in "${SERVICES[@]}"; do
        local bind port reachable
        bind=$(current_bind "$svc")
        port=$(port_for "$svc")
        case "$bind" in
            127.0.0.1) reachable="localhost only" ;;
            unset|0.0.0.0) reachable="INTERNET (if the security group allows)" ;;
            *) reachable="$bind" ;;
        esac
        printf '%-9s %-8s %-12s %s\n' "$svc" "$port" "$bind" "$reachable"
    done
    echo
    echo "Timers armed:"
    ls /tmp/expose-*.pid >/dev/null 2>&1 && ls /tmp/expose-*.pid || echo "  none"
}

cmd_open() {
    local svc=$1 minutes=${2:-}
    validate "$svc"
    if [ -n "$minutes" ]; then
        [[ "$minutes" =~ ^[0-9]+$ ]] || die "minutes must be a number, got '$minutes'"
    fi
    cancel_timer "$svc"
    set_bind "$svc" 0.0.0.0

    echo
    echo "$svc is now bound to 0.0.0.0:$(port_for "$svc")."
    echo "It is only actually reachable if the EC2 security group opens that port too."

    if [ -n "$minutes" ]; then
        [[ "$minutes" =~ ^[0-9]+$ ]] || die "minutes must be a number, got '$minutes'"
        setsid nohup bash -c "sleep $((minutes * 60)); '$(readlink -f "$0")' close '$svc'" \
            >/tmp/expose-${svc}.log 2>&1 &
        echo $! > "/tmp/expose-${svc}.pid"
        echo "Auto-closing in ${minutes} minute(s)."
    else
        echo "No timer set -- remember to run: $0 close $svc"
    fi
}

cmd_close() {
    local svc=$1
    if [ "$svc" = all ]; then
        for s in "${SERVICES[@]}"; do cmd_close "$s"; done
        return
    fi
    validate "$svc"
    cancel_timer "$svc"
    set_bind "$svc" 127.0.0.1
    echo "$svc is now bound to 127.0.0.1:$(port_for "$svc") -- reachable only via nginx or an SSH tunnel."
}

cmd_tunnel() {
    local svc=$1 port
    validate "$svc"
    port=$(port_for "$svc")
    echo "Run this on your laptop, then use http://localhost:${port} there:"
    echo
    echo "  ssh -i ~/Codear-key.pem -N -L ${port}:127.0.0.1:${port} ubuntu@$(curl -s --max-time 3 ifconfig.me || echo '<server-ip>')"
    echo
    echo "This works while $svc stays closed, which is the safer way to debug."
}

usage() {
    cat <<EOF
Toggle whether a backend container's port is reachable from outside the box.

  $0 status
  $0 open <service> [minutes]    bind 0.0.0.0, optionally auto-close after N minutes
  $0 close <service>|all         bind 127.0.0.1 (the safe default)
  $0 tunnel <service>            print an SSH tunnel command instead of opening

Services: ${SERVICES[*]}
EOF
}

case "${1:-}" in
    status) cmd_status ;;
    open)   [ $# -ge 2 ] || die "usage: $0 open <service> [minutes]"; cmd_open "$2" "${3:-}" ;;
    close)  [ $# -ge 2 ] || die "usage: $0 close <service>|all"; cmd_close "$2" ;;
    tunnel) [ $# -ge 2 ] || die "usage: $0 tunnel <service>"; cmd_tunnel "$2" ;;
    *)      usage; exit 1 ;;
esac
