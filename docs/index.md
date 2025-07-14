# CodeGraph MCP Server

**Visualise and navigate your Java projects like never before.**

CodeGraph MCP Server automatically scans your JAR files or IntelliJ projects and builds a rich dependency graph stored in an embedded Neo4j database. Through the open MCP protocol you can query this graph from the command line or directly from IntelliJ.

## Features

- **Instant indexing** of JARs and project classes using ClassGraph.
- **Persistent graph storage** with embedded Neo4j for fast lookup.
- **CLI and IntelliJ plugin** provide flexible deployment options.
- **Rich query API** to find callers, discover dependencies and more.
- **Automatic manifest generation** to advertise available query endpoints.

## Use Cases

- **Impact analysis** – check what code might break before refactoring.
- **Architecture reviews** – visualise cross-module coupling.
- **Continuous integration reporting** – track dependency changes in your pipeline.
- **IDE exploration** – browse relationships without leaving IntelliJ.

## Why Choose CodeGraph?

- **Zero configuration** – simply point to your JARs or open your IDE.
- **Incremental updates** – watches directories and source changes automatically.
- **Open protocol** – MCP JSON requests over STDIO or HTTP make integration easy.

Ready to explore your Java codebase? Install the CLI or IntelliJ plugin and start analysing today.
