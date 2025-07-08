## Project Overview

Build an embedded Neo4j–backed class-graph engine with two entry points:

1. **Core Engine Module**: Scans JARs or IntelliJ PSI trees to build and query an in-memory Neo4j graph.
2. **CLI Plugin**: A standalone Java tool that monitors a folder for new JARs, loads them into the core engine, and exposes an MCP stdio or SSE endpoint.
3. **IntelliJ Plugin**: An IDE plugin that hooks into project open events, scans source/compiled classes via the core engine, watches for changes, and exposes the same MCP API over HTTP.

---

## High-Level Milestones

1. **Architecture & Design** (1 week)
   - Define module boundaries and dependencies
   - Finalize Neo4j embedded schema and core APIs
   - Specify MCP protocol contract and manifest format

2. **Core Engine Implementation** (2–3 weeks)
   - Set up embedded Neo4j and domain model (nodes: Package, Class, Method; edges: EXTENDS, IMPLEMENTS, DEPENDS_ON)
   - Integrate ClassGraph for JAR scanning and initial import
   - Expose query API (Java interfaces) and build manifest generator
   - Unit tests for graph building and basic queries

3. **CLI Plugin Development** (2 weeks)
   - CLI scaffolding with folder watcher (e.g. WatchService)
   - On-new-jar: invoke core engine import and refresh graph
   - MCP stdio server or SSE HTTP server implementation
   - Dockerfile and packaging (fat JAR)
   - Integration tests with sample JARs and OpenHands

4. **IntelliJ Plugin Development** (3–4 weeks)
   - Plugin project setup and MVP skeleton (plugin.xml, StartupActivity)
   - PSI-based scanning to feed core engine
   - File-change listeners to incrementally update graph
   - Embedded HTTP MCP server within plugin
   - UI actions for configuration (port, monitored modules)
   - Manual and automated functional tests in sandbox projects

5. **Integration & Validation** (1 week)
   - End-to-end testing: OpenHands + CLI + IntelliJ plugin scenarios
   - Performance benchmarking on large codebases
   - Security review for embedded Neo4j and HTTP endpoints

6. **Documentation & Release** (1 week)
   - README, Quickstart guides for CLI and IntelliJ plugin
   - API docs for core engine and MCP manifest
   - Example OpenHands config snippets
   - Publish artifacts to Maven Central / JetBrains Marketplace

---

## Detailed Task Breakdown

### Core Engine
- Define Maven/Gradle project structure and dependencies
- Model graph schema in Neo4j (embedded) with indexes on className
- Implement `JarImporter.import(File jar)` using ClassGraph → Neo4j
- Provide `GraphQueryService` interface for callers, implements methods:
  - `List<String> findCallers(String className)`
  - `List<String> findImplementations(String interfaceName)`
  - `List<String> findDependencies(String className)`
- Build manifest generator:
  - Reflect on `GraphQueryService` methods to produce JSON manifest
- Write unit tests against in-memory Neo4j instance

### CLI Plugin
- Use Java NIO `WatchService` to watch `--watch-dir`
- On `ENTRY_CREATE` of `*.jar`, call `CoreEngine.import(jar)`
- Start MCP server:
  - Choose stdio vs SSE based on flag
  - Handle registration (write manifest JSON)
  - Loop read JSON queries → dispatch to `GraphQueryService`
- Logging, metrics, error handling
- Assembly plugin JAR with `Main-Class`

### IntelliJ Plugin
- Create plugin skeleton with `plugin.xml`
- Implement `ProjectOpenActivity` to:
  - Locate compiled output directories and source roots
  - Call `CoreEngine.import(...)` on each target JAR or scan class outputs
- Register `PsiTreeChangeListener` to detect new/modified classes
- On change, call incremental `CoreEngine.update(...)`
- Embed HTTP server (Ktor or Netty): `/mcp/manifest`, `/mcp/query`
- Settings UI: port selection, package filters
- Package as `.zip` plugin distribution

### Integration & Testing
- Write integration tests: launch CLI server, load sample JAR, send JSON query, verify response
- IntelliJ plugin manual test plan: open sample Spring Boot project, verify endpoint responds correctly
- Performance tests: import large open-source JAR (e.g. Spring Boot), measure import time and query latency

### Documentation & Release
- Compose Quickstart:
  - `./cli-plugin --watch-dir path --stdio` example
  - IntelliJ: install plugin, open project, configure port
  - OpenHands config.toml snippets for both
- Publish artifacts:
  - CLI: Maven Central GAV, GitHub Release
  - IntelliJ: JetBrains Marketplace submission

---

## Timeline Summary

| Phase                     | Duration  | Dates (Approx.)            |
|---------------------------|-----------|----------------------------|
| Architecture & Design     | 1 week    | Jul 15 – Jul 22, 2025      |
| Core Engine               | 2–3 weeks | Jul 23 – Aug 12, 2025      |
| CLI Plugin                | 2 weeks   | Aug 13 – Aug 27, 2025      |
| IntelliJ Plugin           | 3–4 weeks | Aug 28 – Sep 25, 2025      |
| Integration & Validation  | 1 week    | Sep 26 – Oct 3, 2025       |
| Documentation & Release   | 1 week    | Oct 4 – Oct 11, 2025       |

---

*Next steps:* Review milestones, adjust timeline for your team’s availability, and kick off the Architecture & Design phase.

