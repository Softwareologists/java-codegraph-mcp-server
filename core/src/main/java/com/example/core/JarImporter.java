package com.example.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.io.File;
import java.util.List;
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

            try (ScanResult scan = new ClassGraph()
                    .overrideClasspath(jar)
                    .enableClassInfo()
                    .scan()) {
                List<String> classes = scan.getAllClasses().getNames();
                LOGGER.info("Classes: " + classes);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import JAR", e);
            throw new RuntimeException(e);
        }
    }
}
