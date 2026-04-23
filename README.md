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

### 2. Start the Agent (Terminal 1)

The Agent listens for commands on port **7070** by default.

```bash
java -jar volta-agent/target/volta-agent-1.0-SNAPSHOT.jar
```

Expected output:
```
Agent started on port 7070
```

### 3. Run the Master (Terminal 2)

```bash
java -jar volta-master/target/volta-master-1.0-SNAPSHOT.jar \
  --url=https://httpbin.org/get \
  --rps=5 \
  --duration=10 \
  --agent=localhost:7070
```

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

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow, issue and pull request guidelines, and code style.

## License

[MIT](LICENSE)
