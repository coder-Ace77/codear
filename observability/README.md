# Observability

Prometheus + Grafana + node_exporter for the `codear-ap-south` EC2 box, published at
`https://backend.apilabs.top/grafana/`.

Everything binds to `127.0.0.1`. Nginx is the only way in from the internet.

Sized for a 1 vCPU / 950 MB instance: 60s scrape, 7 day retention, and `mem_limit`
on both containers so Docker kills Prometheus or Grafana under memory pressure
instead of the kernel picking one of the API containers.

| Container | Port (localhost) | Memory cap |
|---|---|---|
| prometheus | 9090 | 200 MB |
| grafana | 3000 | 180 MB |
| node-exporter | 9100 | 40 MB |

## Deploy

**1. Copy this directory to the box**

```bash
rsync -avz -e "ssh -i ~/Codear-key.pem" --exclude .env \
    observability/ ubuntu@3.108.61.89:~/observability/
```

**2. Create the Grafana password**

```bash
cd ~/observability
cp .env.example .env
$EDITOR .env          # set a real GF_SECURITY_ADMIN_PASSWORD
```

The password is only read on first boot — after that it lives in the Grafana
database, and changing `.env` does nothing.

**3. Turn on Docker daemon metrics** (optional, powers the "Containers running" panel)

```bash
echo '{ "metrics-addr": "127.0.0.1:9323" }' | sudo tee /etc/docker/daemon.json
sudo systemctl restart docker
```

Restarting Docker restarts every container on the box — do it when a few seconds
of downtime is fine. Skip this step and everything else still works; only that
one panel stays empty.

**4. Start the stack**

```bash
cd ~/observability && docker compose up -d
docker compose ps
curl -s localhost:9090/api/v1/targets | grep -o '"health":"[a-z]*"'
```

**5. Wire up nginx**

```bash
sudo tee /etc/nginx/conf.d/upgrade.conf >/dev/null <<'EOF'
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}
EOF
```

Then paste the `location /grafana/` block from `nginx/grafana.conf` into the
`server { ... }` block in `/etc/nginx/sites-enabled/default` that listens on 443
(the one with `server_name backend.apilabs.top`), and:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

Grafana is now at `https://backend.apilabs.top/grafana/`.

## Dashboards

`grafana/dashboards/host-overview.json` is provisioned automatically into the
"Codear" folder — CPU, memory, swap, disk, load, network, container count.

For more, import by ID in the Grafana UI (Dashboards → New → Import):

- **1860** — Node Exporter Full. Very thorough; some panels will be empty because
  this deployment runs a reduced collector set to save memory.
- **4701** — JVM (Micrometer), once the engine exposes actuator metrics.

## Adding application metrics

Prometheus already has the scrape jobs written and commented out in
`prometheus.yml`. To enable a service, add its instrumentation, redeploy the
image, uncomment the job, and `docker compose restart prometheus`.

**engine** (Spring Boot) — add to `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
  <scope>runtime</scope>
</dependency>
```

and to `application.properties`:

```properties
management.endpoints.web.exposure.include=health,prometheus
```

**user / problem** (FastAPI) — add `prometheus-fastapi-instrumentator` to
`requirements.txt` and two lines to `app/main.py`:

```python
from prometheus_fastapi_instrumentator import Instrumentator
Instrumentator().instrument(app).expose(app)
```

Keep the services bound to `127.0.0.1` when you do this — `/metrics` and
`/actuator/prometheus` are unauthenticated and leak a lot about the runtime.

## Alerts

Use Grafana's built-in alerting (Alerting → Alert rules) rather than adding
Alertmanager — it is one less container and ~30 MB. Rules worth having:
`node_memory_MemAvailable_bytes` under ~80 MB, disk above 85%, and
`up{job="node"} == 0`.

## Port exposure

`scripts/expose.sh` toggles whether a backend port is reachable from outside the
box. Copy it to the server (`~/expose.sh`) and `chmod +x` it.

```bash
./expose.sh status              # what is open right now
./expose.sh close all           # bind everything to 127.0.0.1 (the safe default)
./expose.sh open engine 30      # expose engine for 30 minutes, then auto-close
./expose.sh tunnel engine       # safer: print an SSH tunnel command instead
```

It rewrites the `ports:` line in that service's `docker-compose.yaml` (keeping a
`.bak`) and recreates the container, so each switch costs a few seconds of
downtime. Binding to `0.0.0.0` only matters if the EC2 security group also allows
the port — both have to be open for traffic to arrive.
