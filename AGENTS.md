# AGENTS.md

## Overview

This file contains best practices and instructions for AI agents contributing to the **mcp-graph** project. Follow these guidelines to work effectively on isolated features, ensure code quality, and maintain consistency across modules.

## Repository Structure

```
├── core/
├── cli/
├── intellij/
├── examples/
├── README.md
├── AGENTS.md
└── settings.gradle.kts
```

- **core/**: Core engine, graph model, Neo4j integration.
- **cli/**: Standalone CLI watcher & MCP stdio server.
- **intellij/**: IntelliJ plugin code and resources.
- **examples/**: Scripts and sample artifacts for smoke testing.

## Task Workflow

1. **Select a Feature Task**: Pick one `FEAT-...` identifier from `Agent Task Breakdown` (e.g., `FEAT-CORE-002`).
2. **Isolate Module**: Open only the target module directory. Run module-specific tests: `./gradlew :<module>:test`.
3. **Implement & Test**: Write code, unit tests, and integration tests as needed. Follow module conventions.
4. **Build & Smoke**: Execute full build: `./gradlew build`. Run relevant examples (`examples/sample-cli.sh` or plugin sandbox).
5. **Commit & Push**: Use feature branch: `feat/<module>/<short-feature>`. Commit message:
   ```
   FEAT-<MODULE>-<ID>: <Short description>

   - What was done
   - Why it was done
   - How to test
   ```
6. **Open PR**: Target `main` branch. Include:
   - Linked task ID
   - Test instructions
   - Screenshots or logs for manual tests (if applicable)

## Coding Guidelines

- **Language & Style**: Java 11+, idiomatic Kotlin optional in core; follow existing style in each module.
- **Formatting**: Use the shared `spotless` config. Run `./gradlew spotlessApply`.
- **Logging**: Use SLF4J for Java; keep logs at appropriate levels (DEBUG for verbose, INFO for major events).
- **Error Handling**: Fail-fast on configuration errors; wrap checked exceptions with meaningful messages.

## Testing Practices

- **Unit Tests**: Each public method must have tests covering positive, negative, and edge cases.
- **Integration Tests**: Use in-memory Neo4j for core; sample JARs for CLI; IntelliJ test fixtures for plugin.
- **Test Names**: `<methodUnderTest>_<condition>_<expectedResult>`.
- **Coverage**: Aim for ≥80% coverage in the changed module.

## Branch & Commit Naming

- **Branch**: `feat/<module>/<task-id>-<short-desc>`, `fix/<module>/<issue-id>-<short-desc>`.
- **Commit**: Prefix with task ID (e.g., `FEAT-CORE-003:`). Separate subject and body with a blank line.

## Pull Request Guidelines

- **Title**: `<FEAT-...>: Short summary`.
- **Description**:
  - Context and motivation
  - Implementation summary
  - Testing steps
  - Screenshots or logs
- **Reviewers**: Assign one core maintainer and one module specialist.

## Continuous Integration

- Commits trigger CI pipeline:
  1. `:core:test`
  2. `:cli:integrationTest`
  3. `:intellij:buildPlugin`
  4. `spotlessCheck`
- Fix any failures before merging.

## Manifest & MCP Validation

- Update `manifest.json.tpl` in core to reflect new capabilities.
- Validate manifest with `./gradlew :core:generateManifest` and inspect output.

## Communication

- For blockers or ambiguities, open an issue on GitHub linking the task ID.
- Use consistent terminology from `Agent Task Breakdown` and codebase.

---

Thank you for your contributions! Following these guidelines helps keep our multi-agent workflow smooth, reliable, and high-quality. Feel free to suggest improvements to this document via a PR.

