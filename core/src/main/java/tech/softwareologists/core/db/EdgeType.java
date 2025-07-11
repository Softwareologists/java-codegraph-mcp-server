package tech.softwareologists.core.db;

/**
 * Enumerates the relationship types used in the Neo4j graph schema.
 */
public enum EdgeType {
    /**
     * Represents a CALLS relationship between method nodes.
     */
    CALLS,
    /**
     * Represents a DEPENDS_ON relationship between class nodes.
     */
    DEPENDS_ON,
    /**
     * Represents an IMPLEMENTS relationship between class and interface nodes.
     */
    IMPLEMENTS,
    /**
     * Represents an EXTENDS relationship between class nodes.
     */
    EXTENDS,
    /**
     * Represents a USES relationship where a class depends on a service bean.
     */
    USES;

    @Override
    public String toString() {
        return name();
    }
}
