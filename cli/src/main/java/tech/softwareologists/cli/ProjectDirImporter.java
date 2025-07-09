package tech.softwareologists.cli;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import tech.softwareologists.core.db.NodeLabel;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to import compiled class files from a directory.
 */
public class ProjectDirImporter {
    private static final Logger LOGGER = Logger.getLogger(ProjectDirImporter.class.getName());

    private ProjectDirImporter() {
        // utility class
    }

    /**
     * Import the compiled classes in the given directory into the graph.
     *
     * @param dir    directory containing compiled .class files
     * @param driver Neo4j driver used for persistence
     */
    public static void importDir(File dir, Driver driver) {
        try {
            LOGGER.info("Importing directory: " + dir.getAbsolutePath());

            try (ScanResult scan = new ClassGraph()
                    .overrideClasspath(dir)
                    .enableClassInfo()
                    .enableInterClassDependencies()
                    .scan()) {
                List<String> classes = scan.getAllClasses().getNames();
                LOGGER.info("Classes: " + classes);
                try (Session session = driver.session()) {
                    for (ClassInfo classInfo : scan.getAllClasses()) {
                        String cls = classInfo.getName();
                        session.run(
                                "MERGE (c:" + NodeLabel.CLASS + " {name:$name})",
                                Values.parameters("name", cls));

                        java.util.Set<String> seenDeps = new java.util.HashSet<>();
                        for (ClassInfo dep : classInfo.getClassDependencies()) {
                            String depName = dep.getName();
                            if (cls.equals(depName) || !seenDeps.add(depName)) {
                                continue;
                            }
                            session.run(
                                    "MERGE (d:" + NodeLabel.CLASS + " {name:$dep})",
                                    Values.parameters("dep", depName));
                            session.run(
                                    "MATCH (s:" + NodeLabel.CLASS + " {name:$src}), (t:" + NodeLabel.CLASS + " {name:$tgt}) MERGE (s)-[:DEPENDS_ON]->(t)",
                                    Values.parameters("src", cls, "tgt", depName));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import directory", e);
            throw new RuntimeException(e);
        }
    }
}
