# Contributing to Volta

## Setup

1. Clone the repository
2. Open in IntelliJ IDEA (File → Open → select project root)
3. IDEA should detect Maven project automatically
4. Ensure JDK 21+ is configured (File → Project Structure → Project → SDK)

## Important

Use `./mvnw` instead of `mvn` (ensures same Maven version for everyone)

## Workflow

1. Pick an issue from the [Volta Kanban](https://github.com/orgs/volta-org/projects/1) or create one (see the [Creating an Issue](#creating-an-issue) section)
2. Assign yourself to the issue
3. Move the issue to **In Progress** on the board
4. Pull latest changes:
   ```bash
    git pull origin main
    ```
5. Create a feature branch:
   ```bash
    git checkout -b surname/feature-name
    ```
6. Write code and tests
7. Format code: 
    ```bash
   ./mvnw spotless:apply
    ```
8. Verify locally:
    ```bash
   ./mvnw test
    ```
9. Commit with a clear message in English ([Commit Message Style](#commit-message-style) recommended, but not strict):
   ```bash
   git commit -m "add stats collector"
10. Push your branch:
    ```bash
    git push origin surname/feature-name
    ```
11. Open a Pull Request (see the [Opening a Pull Request](#opening-a-pull-request) section)
12. Wait for CI checks to pass and code review approval

## Commit Message Style

Use prefixes:
- `feat:` — new feature
- `fix:` — bug fix
- `refactor:` — code restructuring
- `test:` — adding/updating tests
- `chore:` — CI, configs, dependencies
- `docs:` — documentation

## Creating an Issue

**Title:** Short and clear
```
Implement basic Load Engine with RPS throttling
```

**Body:**
```
## Description
Brief explanation of what needs to be done.

## Tasks
- [ ] Task 1
- [ ] Task 2
- [ ] Task 3
```

If the issue depends on another, add `Depends on #<number>` in the description.

**Labels** — add one component label and one type label:

| Component | Use for |
|-----------|---------|
| `engine` | Load generation logic |
| `stats` | Metrics collection |
| `cli` | Command-line interface |
| `cluster` | Master-Agent networking |
| `infra` | CI, build, configs |

| Type | Use for |
|------|---------|
| `feature` | New functionality |
| `bug` | Bug fix |
| `refactor` | Code improvement |

## Opening a Pull Request

**Title:** Same style as commits (see the [Commit Message Style](#commit-message-style) section)
> **Note:** All PRs are merged using **Squash and Merge**.
> This keeps the main branch history clean — one commit per feature/fix.
> Write a clear PR title, as it becomes the commit message in main.
```
feat: add stats collector
```

**Description:**
```
## What
Brief summary of changes.

## How to verify
./mvnw test

Closes #<issue-number>
```

`Closes #<number>` automatically closes the linked issue when the PR is merged.
