package tech.softwareologists.core.db;

/**
 * Enumerates the node labels used in the Neo4j graph schema.
 */
public enum NodeLabel {
    /** Node representing a class. */
    CLASS,
    /**
     * Node representing a method declaration. Method nodes store the fully
     * qualified class name in the {@code class} property and the JVM method
     * signature in the {@code signature} property.
     */
    METHOD;

    @Override
    public String toString() {
        return name();
    }
}
