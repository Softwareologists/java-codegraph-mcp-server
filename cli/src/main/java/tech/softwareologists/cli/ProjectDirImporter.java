package tech.softwareologists.cli;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import tech.softwareologists.core.db.NodeLabel;
import tech.softwareologists.core.db.EdgeType;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
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
     * Import the compiled classes in the given directory into the graph and
     * return the number of classes discovered.
     *
     * @param dir    directory containing compiled .class files
     * @param driver Neo4j driver used for persistence
     * @return number of classes imported
     */
    public static int importDirectory(File dir, Driver driver) {
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

                        // parse bytecode to create method nodes and call edges
                        List<String> methodNames = new java.util.ArrayList<>();
                        Map<String, Set<String>> calls = new HashMap<>();
                        try (java.io.InputStream in = classInfo.getResource().open()) {
                            ClassReader cr = new ClassReader(in);
                            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                                @Override
                                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                    String sig = name + descriptor;
                                    methodNames.add(sig);
                                    session.run(
                                            "MERGE (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig})",
                                            Values.parameters("cls", cls, "sig", sig));
                                    return new MethodVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                            String tgtCls = owner.replace('/', '.');
                                            String tgtSig = name + descriptor;
                                            calls.computeIfAbsent(sig, k -> new HashSet<>()).add(tgtCls + "|" + tgtSig);
                                        }
                                    };
                                }
                            }, 0);
                        }
                        if (!methodNames.isEmpty()) {
                            LOGGER.info("Methods in " + cls + ": " + methodNames);
                        }

                        for (Map.Entry<String, Set<String>> entry : calls.entrySet()) {
                            String fromSig = entry.getKey();
                            for (String tgtCombined : entry.getValue()) {
                                int idx = tgtCombined.indexOf('|');
                                String tgtCls = tgtCombined.substring(0, idx);
                                String tgtSig = tgtCombined.substring(idx + 1);
                                session.run(
                                        "MERGE (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig})",
                                        Values.parameters("cls", tgtCls, "sig", tgtSig));
                                session.run(
                                        "MATCH (s:" + NodeLabel.METHOD + " {class:$scls, signature:$ssig}), " +
                                                "(t:" + NodeLabel.METHOD + " {class:$tcls, signature:$tsig}) MERGE (s)-[:CALLS]->(t)",
                                        Values.parameters(
                                                "scls", cls,
                                                "ssig", fromSig,
                                                "tcls", tgtCls,
                                                "tsig", tgtSig));
                            }
                        }

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
                                    "MATCH (s:" + NodeLabel.CLASS + " {name:$src}), (t:" + NodeLabel.CLASS + " {name:$tgt}) MERGE (s)-[:" + EdgeType.DEPENDS_ON + "]->(t)",
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
                                    "MATCH (c:" + NodeLabel.CLASS + " {name:$cls}), (i:" + NodeLabel.CLASS + " {name:$iface}) MERGE (c)-[:" + EdgeType.IMPLEMENTS + "]->(i)",
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
                                    "MATCH (c:" + NodeLabel.CLASS + " {name:$cls}), (s:" + NodeLabel.CLASS + " {name:$sup}) MERGE (c)-[:" + EdgeType.EXTENDS + "]->(s)",
                                    Values.parameters("cls", cls, "sup", superName));
                        }
                    }
                }
                LOGGER.info("Imported " + classes.size() + " classes");
                return classes.size();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import directory", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Backwards compatible wrapper for {@link #importDirectory(File, Driver)}.
     */
    public static void importDir(File dir, Driver driver) {
        importDirectory(dir, driver);
    }
}
