# Volta

Distributed load testing system for HTTP services with AI-powered analysis.

## Prerequisites

- Java 21+

## Quick Start

```bash
git clone <repo-url>
cd volta
./mvnw package
java -jar target/volta-1.0-SNAPSHOT.jar
```

## For Developers

### Setup

1. Clone the repository
2. Open in IntelliJ IDEA (File → Open → select project root)
3. IDEA should detect Maven project automatically

### Important

- Always use `./mvnw` instead of `mvn` (ensures same Maven version for everyone)
- `./mvnw test` — run tests
- `./mvnw package` — build JAR

### Workflow

1. Pull latest changes: `git pull origin main`
2. Create a feature branch: `git checkout -b surname/feature-name`
3. Write code and tests
4. Auto-format code: `./mvnw spotless:apply`
5. Verify locally: `./mvnw test`
6. Commit: `git commit -m "Add feature description"` (in English)
7. Push: `git push origin surname/feature-name`
8. Open a Pull Request on GitHub
9. Wait for CI checks to pass and code review approval
