## Agent-Friendly Feature Breakdown

Below is a list of small, self-contained features grouped by module. Each can be assigned independently.

---

### Core Engine Module

1. **FEAT-CORE-001: JAR Importer Skeleton**  
   - Create JarImporter.import(File jar) stub method.  
   - Define method signature and basic exception handling.  
   - Write placeholder unit test expecting no exceptions on an empty JAR.

2. **FEAT-CORE-002: ClassGraph Integration**  
   - Add ClassGraph dependency.  
   - Implement scanning of provided JAR to list class names.  
   - Console-log raw class list.  
   - Unit test: assert sample JAR yields expected classes.

3. **FEAT-CORE-003: Neo4j Schema Setup**  
   - Add embedded Neo4j dependency and driver configuration.  
   - Define node labels (Class, Method) and index on class name.  
   - Unit test: start in-memory Neo4j, create a node, query by label.

4. **FEAT-CORE-004: Graph Persistence**  
   - Extend JarImporter to write scanned classes as Class nodes in Neo4j.  
   - Unit test: import sample JAR, verify nodes exist in graph.

5. **FEAT-CORE-005: Dependency Edges**  
   - Capture inter-class dependencies via ClassInfo.getClassDependencies().  
   - Persist DEPENDS_ON relationships between class nodes.  
   - Unit test: for two dependent classes, assert relationship.

6. **FEAT-CORE-006: QueryService Interface**  
   - Define QueryService.findCallers(String className).  
   - Implement Cypher query to return caller class names.  
   - Unit test: seed graph, call method, verify output list.

7. **FEAT-CORE-007: Manifest Generator**  
   - Create ManifestGenerator.generate() reflecting on QueryService methods.  
   - Emit JSON manifest listing capabilities and parameter names.  
   - Unit test: verify manifest contains expected capability names.

8. **FEAT-CORE-008: Core Dockerfile**  
   - Write Dockerfile to build and run core engine jar.  
   - Smoke test: build image, run container, ensure exit code 0.

---

### CLI Plugin Module

1. **FEAT-CLI-001: CLI Main Class**  
   - Create CliMain with flags --watch-dir and --stdio.  
   - Print usage on missing args.  
   - Unit test: invoking --help returns usage message.

2. **FEAT-CLI-002: Folder Watcher**  
   - Implement JarWatcher using WatchService to detect new JAR files.  
   - Log detected file paths.  
   - Unit test: simulate file creation, assert watcher logs event.

3. **FEAT-CLI-003: MCP Stdio Server**  
   - Create StdioMcpServer to read JSON from stdin and echo responses.  
   - Integrate ManifestGenerator to send capabilities at startup.  
   - Unit test: feed manifest request, assert stdout manifest output.

4. **FEAT-CLI-004: Integration Wiring**  
   - Wire JarWatcher events to call JarImporter.import().  
   - Dispatch JSON queries to QueryService and return results.  
   - Integration test: start CLI, import sample JAR, query CALLERS_OF_CLASS.

5. **FEAT-CLI-005: Packaging and Samples**  
   - Configure Gradle fat‑jar.  
   - Add sample example.jar for CI.  
   - CI script: assemble CLI and run smoke invocation.

---

### IntelliJ Plugin Module

1. **FEAT-IJ-001: Plugin Skeleton**  
   - Add plugin.xml and StartupActivity.  
   - Log project-open events in IDE logs.  
   - Manual test: install plugin, verify logs on project open.

2. **FEAT-IJ-002: PSI Scanner**  
   - Use PsiManager to collect PsiClass elements in project.  
   - Pass class names to Core JarImporter API.  
   - Unit test: load test project, assert classes discovered.

3. **FEAT-IJ-003: Incremental Updates**  
   - Implement PsiTreeChangeListener for class file changes.  
   - On change, update nodes/edges in Neo4j via core service.  
   - Manual test: modify class, verify graph update via HTTP query.

4. **FEAT-IJ-004: Embedded HTTP Server**  
   - Integrate lightweight HTTP server exposing /mcp/manifest and /mcp/query.  
   - Use ManifestGenerator and QueryService for responses.  
   - Unit test: GET manifest, POST query, assert JSON responses.

5. **FEAT-IJ-005: Settings UI**  
   - Add UI panel for MCP port and package filters.  
   - Store settings in PropertiesComponent.  
   - Manual test: change settings, restart plugin, confirm server port.

---

### Examples & Documentation

1. **FEAT-DOC-001: README Update**  
   - Add overview of modules and feature list.  
   - Provide instructions for running individual feature tests.

2. **FEAT-DOC-002: Example Scripts**  
   - `examples/sample-cli.sh` showing import and query flow.  
   - `examples/idea-launch.md` with plugin installation steps.

3. **FEAT-DOC-003: CI Pipeline**  
   - Define CI job to run core unit tests, CLI integration tests, and plugin build verification.

