package tech.softwareologists.core;

import java.io.File;

/**
 * High level entry point for interacting with the graph engine.
 */
public interface CoreEngine extends AutoCloseable {
    /**
     * Import the given JAR file into the graph.
     *
     * @param jar JAR file containing compiled classes
     */
    void importJar(File jar);

    /**
     * Return the query service backed by the engine's database.
     */
    QueryService getQueryService();

    /**
     * Generate the MCP manifest describing available queries.
     */
    String getManifest();
}
