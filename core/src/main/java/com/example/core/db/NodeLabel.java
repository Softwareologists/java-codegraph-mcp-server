package com.example.core.db;

/**
 * Enumerates the labels used in the Neo4j graph schema.
 */
public enum NodeLabel {
    CLASS,
    METHOD;

    @Override
    public String toString() {
        return name();
    }
}
