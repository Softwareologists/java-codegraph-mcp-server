package com.example.core;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to import JAR files into the core engine.
 */
public class JarImporter {
    private static final Logger LOGGER = Logger.getLogger(JarImporter.class.getName());

    private JarImporter() {
        // utility class
    }

    /**
     * Import the given JAR file into the graph. Implementation to follow.
     *
     * @param jar the JAR file to import
     */
    public static void importJar(File jar) {
        try {
            LOGGER.info("Importing JAR: " + jar.getAbsolutePath());
            // TODO implement scanning and persistence
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import JAR", e);
            throw new RuntimeException(e);
        }
    }
}
