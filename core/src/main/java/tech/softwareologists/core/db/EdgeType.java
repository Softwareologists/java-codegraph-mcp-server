package tech.softwareologists.core.db;

/**
 * Enumerates the relationship types used in the Neo4j graph schema.
 */
public enum EdgeType {
    /**
     * Represents a CALLS relationship between method nodes.
     */
    CALLS;

    @Override
    public String toString() {
        return name();
    }
}
