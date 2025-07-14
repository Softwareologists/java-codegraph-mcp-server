# CodeGraph MCP Server for Java applications(java-codegraph-mcp-server)

[![CI](https://github.com/Softwareologists/java-codegraph-mcp-server/actions/workflows/pr-tests.yml/badge.svg)](https://github.com/Softwareologists/java-codegraph-mcp-server/actions/workflows/pr-tests.yml)
[![Build](https://github.com/Softwareologists/java-codegraph-mcp-server/actions/workflows/ci.yml/badge.svg)](https://github.com/Softwareologists/java-codegraph-mcp-server/actions/workflows/ci.yml)

A Java-based multi-module project that uses ClassGraph to scan JARs (and IntelliJ PSI), persists the resulting class-dependency graph in an embedded Neo4j database, and exposes it over the MCP protocol via both a CLI folder-watcher and an IntelliJ plugin.

## Features

* **Core Engine**: Scans JAR files or IntelliJ project classes, builds a class-dependency graph, and stores it in an embedded Neo4j instance.
* **CLI Module**: Watches a specified directory for new JARs and serves MCP requests over STDIO or SSE.
* **IntelliJ Plugin**: Hooks into the IDE to scan open projects, incrementally update the graph on code changes, and serves MCP over HTTP.
* **Manifest Generation**: Automatically generates an MCP manifest reflecting available query capabilities.
* **Default Query Options**: The manifest shows defaults `{ "limit": 100, "page": 1, "pageSize": 50 }` that apply when these values are omitted in a query.
* **Examples**: Sample scripts for CLI usage and IntelliJ sandbox setup.

## Repository Structure

```
java-codegraph-mcp-server/
├── core/              # Core engine, graph model, Neo4j integration
├── cli/               # CLI watcher & MCP stdio/SSE server
├── intellij/          # IntelliJ plugin source and resources
├── examples/          # Sample scripts and artifacts for smoke tests
├── AGENTS.md          # Guidelines for AI agents
├── Task_breakdown.md  # Feature list for task-driven development
├── README.md          # Project overview and setup
└── settings.gradle.kts
```

## Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/<your-org>/java-codegraph-mcp-server.git
   cd java-codegraph-mcp-server
   ```

2. **Build all modules**

   ```bash
   ./gradlew build
   ```

3. **Run Core Engine Tests**

   ```bash
   ./gradlew :core:test
   ```

## CLI Usage

1. **Package the CLI**

   ```bash
   ./gradlew :cli:shadowJar
   ```
2. **Check the built jar**

   ```bash
   java -jar cli/build/libs/cli-all.jar --help
   ```
3. **Run the watcher**

   ```bash
   java -jar cli/build/libs/cli-all.jar --watch-dir /path/to/jars --stdio
   ```
4. **Send MCP Requests**

   * At startup, the manifest JSON is printed.
   * Send JSON queries on stdin; responses appear on stdout.
   * Example with paging parameters:

     ```bash
     echo '{"findCallers":{"className":"com.example.A","limit":10,"page":2,"pageSize":5}}' \
       | java -jar cli/build/libs/cli-all.jar --watch-dir /path/to/jars --stdio
     ```
   * Omitted values fallback to the defaults printed in the manifest.

For a full demonstration:

1. Run `examples/build-example-jar.sh` to create `examples/example.jar`.
2. Execute `examples/sample-cli.sh` which builds the CLI, imports that JAR, and runs a sample query.

## IntelliJ Plugin

1. **Install locally**

   * In IntelliJ IDEA, go to *Settings > Plugins > ⚙️ > Install Plugin from Disk...*
   * Select `intellij/build/distributions/intellij-*.zip`.
2. **Configure**

   * Open *Settings > Tools > CodeGraph MCP* and set the HTTP port and optional package filters.
3. **Use**

   * On project open, the plugin scans classes and exposes `/mcp/manifest` and `/mcp/query`.
   * Example HTTP request with paging:

     ```bash
     curl -X POST -H "Content-Type: application/json" \
       -d '{"findCallers":{"className":"com.example.A","limit":10,"page":2,"pageSize":5}}' \
       http://localhost:9090/mcp/query
     ```
   * Omitting these parameters uses the defaults shown in the manifest.

For detailed setup steps see [examples/idea-launch.md](examples/idea-launch.md).

## Configuring OpenHands

Add to your `openhands.toml`:

```toml
[mcp]
# for CLI stdio tool
stdio_servers = [ { name = "codegraph", command = "java", args = ["-jar","/path/to/cli-all.jar","--stdio"] } ]

# or for IntelliJ HTTP server
sse_servers = ["http://localhost:9090/mcp/query"]
```

## Contributing

See [AGENTS.md](AGENTS.md) for guidelines on working with AI agents and feature tasks. To contribute:

1. Pick a task from the Agent Task Breakdown.
2. Follow the branch and commit naming conventions.
3. Run tests and examples.
4. Open a PR against `main`.

## License

This project is released under the MIT License. See [LICENSE](LICENSE) for details.
