package tech.softwareologists.core;

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
 * Utility class to import JAR files into the core engine.
 */
public class JarImporter {
    private static final Logger LOGGER = Logger.getLogger(JarImporter.class.getName());

    private JarImporter() {
        // utility class
    }

    /**
     * Import the given JAR file into the graph.
     *
     * @param jar the JAR file to import
     * @param driver the Neo4j driver to use for persistence
     */
    public static void importJar(File jar, Driver driver) {
        try {
            LOGGER.info("Importing JAR: " + jar.getAbsolutePath());

            try (ScanResult scan = new ClassGraph()
                    .overrideClasspath(jar)
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

                        // record implemented interfaces
                        java.util.Set<String> seenInterfaces = new java.util.HashSet<>();
                        for (ClassInfo iface : classInfo.getInterfaces()) {
                            String ifaceName = iface.getName();
                            if (!seenInterfaces.add(ifaceName)) {
                                continue;
                            }
                            session.run(
                                    "MERGE (i:" + NodeLabel.CLASS + " {name:$iface})",
                                    Values.parameters("iface", ifaceName));
                            session.run(
                                    "MATCH (c:" + NodeLabel.CLASS + " {name:$cls}), (i:" + NodeLabel.CLASS + " {name:$iface}) MERGE (c)-[:IMPLEMENTS]->(i)",
                                    Values.parameters("cls", cls, "iface", ifaceName));
                        }

                        // record superclass relationship
                        ClassInfo superInfo = classInfo.getSuperclass();
                        if (superInfo != null) {
                            String superName = superInfo.getName();
                            session.run(
                                    "MERGE (s:" + NodeLabel.CLASS + " {name:$sup})",
                                    Values.parameters("sup", superName));
                            session.run(
                                    "MATCH (c:" + NodeLabel.CLASS + " {name:$cls}), (s:" + NodeLabel.CLASS + " {name:$sup}) MERGE (c)-[:EXTENDS]->(s)",
                                    Values.parameters("cls", cls, "sup", superName));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import JAR", e);
            throw new RuntimeException(e);
        }
    }
}
