# java-codegraph-mcp-server
A Java-based multi-module project that uses ClassGraph to scan JARs (and IntelliJ PSI), persists the resulting class-dependency graph in an embedded Neo4j database, and exposes it over the MCP protocol via both a CLI folder-watcher and an IntelliJ plugin.
