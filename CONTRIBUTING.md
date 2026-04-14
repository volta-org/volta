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
5. Create a branch ([Conventional Naming Style](#conventional-naming-style) recommended, but not strict):
   ```bash
    git checkout -b feature/stats-collector
    ```
6. Write code and tests
7. Verify locally (see the [Suppressing Checkstyle Warnings](#suppressing-checkstyle-warnings) section): 
    ```bash
    ./mvnw verify
    ```
8. Commit with a clear message in English ([Conventional Naming Style](#conventional-naming-style) recommended, but not strict):
   ```bash
   git commit -m "feature: add stats collector"
   ```
9. Push your branch:
   ```bash
   git push origin feature/stats-collector
   ```
10. Open a Pull Request (see the [Opening a Pull Request](#opening-a-pull-request) section)
11. Wait for CI checks to pass and code review approval

## Conventional Naming Style

Use prefixes:
- `feature` — new feature
- `fix` — bug fix
- `refactor` — code restructuring
- `test` — adding/updating tests
- `chore` — CI, configs, dependencies
- `docs` — documentation

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

| Component | Use for                 |
|-----------|-------------------------|
| `engine`  | Load generation logic   |
| `stats`   | Metrics collection      |
| `cli`     | Command-line interface  |
| `cluster` | Master-Agent networking |
| `infra`   | CI, build, configs      |

| Type       | Use for           |
|------------|-------------------|
| `feature`  | New functionality |
| `bug`      | Bug fix           |
| `refactor` | Code improvement  |

## Opening a Pull Request

**Title:** Same style as commits (see the[Conventional Naming Style](#conventional-naming-style) section)
> **Note:** All PRs are merged using **Squash and Merge**.
> This keeps the main branch history clean — one commit per feature/fix.
> Write a clear PR title, as it becomes the commit message in main.
```
feature: add stats collector
```

**Description:**
```
## What
Brief summary of changes.

## How to verify
./mvnw verify

Closes #<issue-number>
```

`Closes #<number>` automatically closes the linked issue when the PR is merged.

## Suppressing Checkstyle Warnings

In rare cases you may need to suppress a Checkstyle rule. Use `@SuppressWarnings("checkstyle:RuleName")` where `RuleName` is the exact module name from `checkstyle.xml`.

**Suppress a single rule on one declaration:**

You see:
```
[ERROR] .../Main.java:[15,9] (naming) LocalVariableName: Name 'MyVar' must match pattern '^[a-z][a-zA-Z0-9]*$'.
```

You write:
```java
@SuppressWarnings("checkstyle:LocalVariableName")
int MyVar = 123;
```

**Suppress multiple rules:**

You see:
```
[ERROR] .../AgentClient.java:[34,33] (naming) ParameterName: Name 'target_url' must match pattern '^[a-z][a-zA-Z0-9]*$'.
[ERROR] .../AgentClient.java:[36,16] (naming) LocalVariableName: Name 'response_code' must match pattern '^[a-z][a-zA-Z0-9]*$'.
```

You write:
```java
@SuppressWarnings({"checkstyle:ParameterName", "checkstyle:LocalVariableName"})
public void connect(String target_url) {
    int response_code = 123;
}
```

> **Note:** The annotation suppresses all violations of the listed rules
> within the annotated element (field, method, or class). Keep suppressions
> as narrow as possible - prefer annotating a field over a whole class.
