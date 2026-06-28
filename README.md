# Volta

Distributed load testing system for HTTP services with AI-powered analysis.

<video src="https://github.com/user-attachments/assets/c950e3b6-1e26-47a4-9af7-d70468d9bc2f" autoplay loop muted playsinline width="100%"></video>

## Prerequisites

- Java 21+

## Quick Start

### 1. Build

```bash
git clone <repo-url>
cd volta
./mvnw package -DskipTests
```

### 2. Start an Agent

The agent listens for commands on port **7070** by default. Pass a port as the first argument to run on another port:

```bash
java -jar volta-agent/target/volta-agent-1.0-SNAPSHOT.jar
java -jar volta-agent/target/volta-agent-1.0-SNAPSHOT.jar 7071
```

Expected output:

```
Agent started on port 7070
```

### 3. Run the Master

#### Option A (recommended): config file

Create `config.json`:

```json
{
  "url": "https://httpbin.org/get",
  "rps": 5,
  "duration": 10
}
```

Or `config.yaml`:

```yaml
url: "https://httpbin.org/get"
rps: 5
duration: 10
```

Run with one agent:

```bash
java -jar volta-master/target/volta-master-1.0-SNAPSHOT.jar \
  --config=./config.json \
  --agent=localhost:7070
```

#### Option B: CLI flags

```bash
java -jar volta-master/target/volta-master-1.0-SNAPSHOT.jar \
  --url=https://httpbin.org/get \
  --rps=5 \
  --duration=10 \
  --agent=localhost:7070
```

#### Stats file (optional)

`--stats-out=path`

- Ends with `.csv` → header row + CSV lines (`sample` each second + one `final` row).
- Any other suffix (for example `.jsonl`) → one JSON object per line (serialized `StatsSnapshot`).

Expected output:

```
[RPS: 0 | Success: 0.0% | Avg: 0ms | Errors: 0]
[RPS: 4 | Success: 100.0% | Avg: 353ms | Errors: 0]
...
[RPS: 5 | Success: 100.0% | Avg: 212ms | Errors: 0]

========= FINAL STATS =========
Total Requests:  50
Success:         50
Errors:          0
Success Rate:    100.00%
Avg Latency:     211.72ms
Min Latency:     114ms
Max Latency:     863ms
===============================
```

## Load Scenarios

By default, configs with a top-level `url` send **GET** requests. For POST, PUT, PATCH, or DELETE, use a `request` block:

```json
{
  "rps": 5,
  "duration": 10,
  "request": {
    "method": "POST",
    "url": "https://httpbin.org/post",
    "headers": {
      "Content-Type": "application/json"
    },
    "body": "{\"name\":\"volta\"}"
  }
}
```

YAML example (`examples/config-put.yaml`):

```yaml
rps: 5
duration: 10
request:
  method: PUT
  url: "https://httpbin.org/put"
  headers:
    Content-Type: application/json
  body: '{"id": 1}'
```

**Supported methods:** GET, POST, PUT, PATCH, DELETE.

**Validation rules:**
- `url` is required and must start with `http://` or `https://`
- `body` is allowed only for POST, PUT, and PATCH
- `headers` are optional

See `examples/config-post.json` and `examples/config-put.yaml` for ready-to-run samples.

## Multi-Agent Cluster

Master can orchestrate several agents at once. Total RPS from the config is split evenly across available agents.

Example with **4 agents** and **40 RPS** (10 RPS per agent):

**Terminals 1–4 — agents:**

```bash
java -jar volta-agent/target/volta-agent-1.0-SNAPSHOT.jar
java -jar volta-agent/target/volta-agent-1.0-SNAPSHOT.jar 7071
java -jar volta-agent/target/volta-agent-1.0-SNAPSHOT.jar 7072
java -jar volta-agent/target/volta-agent-1.0-SNAPSHOT.jar 7073
```

**Terminal 5 — master:**

```bash
java -jar volta-master/target/volta-master-1.0-SNAPSHOT.jar \
  --config=./config.json \
  --agent=localhost:7070 \
  --agent=localhost:7071 \
  --agent=localhost:7072 \
  --agent=localhost:7073
```

Repeat `--agent=host:port` for each worker. Agents can run on different machines — use their IP instead of `localhost`.

**Tips:**

- Set `rps` ≥ number of agents so every agent gets at least 1 RPS.
- Master aggregates stats from all agents into a single live report and final summary.

## Failover

If an agent becomes unavailable during a test, master:

1. Marks it as dead and logs a warning.
2. Redistributes its RPS share across the remaining agents.
3. Notes incomplete data in the final report if any agent was lost.

At startup, unreachable agents are skipped; the test runs on the agents that respond. Master exits with an error only when no agents are reachable.

Example warning during a test:

```
WARNING: agent unavailable (failed during test): http://localhost:7071
WARNING: redistributed load for http://localhost:7070: 10 -> 14 rps
```

## Agent HTTP API

| Endpoint       | Method | Description                          |
|----------------|--------|--------------------------------------|
| `/start`       | POST   | Start load test (`TestConfig` JSON)  |
| `/stop`        | POST   | Stop current test                    |
| `/stats`       | GET    | Return cumulative stats              |
| `/change-rps`  | POST   | Change RPS mid-test (`{"rps": N}`)   |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow, issue and pull request guidelines, and code style.

## License

[MIT](LICENSE)
