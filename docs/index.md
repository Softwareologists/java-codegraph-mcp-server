# CodeGraph MCP Server

**Explore your codebase with immediate insight and powerful graphs.**

CodeGraph MCP Server turns your compiled artifacts or IntelliJ project into a navigable dependency map. It uses ClassGraph and an embedded Neo4j instance to maintain a blazing‑fast index that you can query via the open MCP protocol from the command line or right inside IntelliJ.

## Features

- **Instant indexing** – new JARs and project classes are scanned as soon as they appear, keeping the graph current at all times.
- **Persistent graph storage** – an embedded Neo4j database serves results in milliseconds even for large codebases.
- **Flexible deployment** – use the standalone CLI in CI pipelines or enable the IntelliJ plugin for seamless in-IDE analysis.
- **Rich query API** – discover call hierarchies, outbound dependencies and more through simple JSON requests.
- **Automatic manifest generation** – each server advertises its capabilities so integrations always know which endpoints are available.

## Use Cases

- **Impact analysis** – understand what will break before you refactor or remove code so you can fix issues proactively.
- **Architecture reviews** – visualise cross-module relationships to uncover hidden coupling and design drift.
- **Continuous integration reporting** – monitor how dependencies evolve across builds and catch surprises early.
- **IDE exploration** – browse callers, callees and references without ever leaving IntelliJ.

## Why Choose CodeGraph?

- **Zero configuration** – just point to your jars or open your IDE and the server handles everything else.
- **Incremental updates** – background watchers notice new artifacts and source changes automatically so the graph is never stale.
- **Open protocol** – query over STDIO or HTTP using plain JSON, making integration with any tool straightforward.

## Get Started

[Download the latest CLI jar and IntelliJ plugin from our GitHub Releases](https://github.com/Softwareologists/java-codegraph-mcp-server/releases) and start analysing your projects today.
