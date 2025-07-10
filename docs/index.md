# CodeGraph MCP Server

A multi-module project that indexes Java class dependencies and exposes them
via the MCP protocol.

## Modules

### Core

- Imports JAR files and discovers classes with **ClassGraph**.
- Persists classes and their dependencies to an embedded **Neo4j** database.
- Provides `QueryService` with methods such as `findCallers`.
- Generates an MCP manifest through `ManifestGenerator`.

### CLI

- `CliMain` parses command line options to watch directories or project paths.
- `JarWatcher` detects new JAR files and triggers imports.
- `StdioMcpServer` responds to manifest and query requests over STDIO.
- `ProjectDirWatcher` and `ProjectDirImporter` handle full project imports.
- Packaged as a standalone fat JAR using the `shadowJar` task.

### IntelliJ

- `StartupActivity` scans the project when the IDE opens.
- `PsiClassImportService` collects PSI classes for indexing.
- `PsiClassChangeListener` updates the graph on file changes.
- `HttpMcpServer` exposes `/mcp/manifest` and `/mcp/query` over HTTP.
- A settings panel allows configuring port and package filters.

## Running Module Tests

Run tests for each module individually with Gradle:

```bash
./gradlew :core:test
./gradlew :cli:test
./gradlew :intellij:test
```

For a full build of all modules use `./gradlew build`.
