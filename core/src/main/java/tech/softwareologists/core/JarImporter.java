package tech.softwareologists.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import tech.softwareologists.core.db.NodeLabel;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;

/**
 * Utility class to import JAR files into the core engine.
 */
public class JarImporter {
    private static final Logger LOGGER = Logger.getLogger(JarImporter.class.getName());

    private static final java.util.Map<String, String> MAPPING_VERBS = java.util.Map.of(
            "org.springframework.web.bind.annotation.GetMapping", "GET",
            "org.springframework.web.bind.annotation.PostMapping", "POST",
            "org.springframework.web.bind.annotation.PutMapping", "PUT",
            "org.springframework.web.bind.annotation.DeleteMapping", "DELETE",
            "org.springframework.web.bind.annotation.PatchMapping", "PATCH"
    );
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";

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

                        // record annotations on the class
                        java.util.List<String> annos = new java.util.ArrayList<>();
                        classInfo.getAnnotationInfo().forEach(a -> annos.add(a.getName()));
                        if (!annos.isEmpty()) {
                            session.run(
                                    "MATCH (c:" + NodeLabel.CLASS + " {name:$cls}) SET c.annotations=$annos",
                                    Values.parameters("cls", cls, "annos", annos));
                        }

                        // parse bytecode to create method nodes and call edges
                        List<String> methodNames = new java.util.ArrayList<>();
                        Map<String, Set<String>> calls = new HashMap<>();
                        Map<String, java.util.List<String>> methodAnnos = new HashMap<>();
                        Map<String, String> methodRoutes = new HashMap<>();
                        Map<String, String> methodVerbs = new HashMap<>();
                        Map<String, String> eventTypes = new HashMap<>();
                        Map<String, String> cronExprs = new HashMap<>();
                        Set<String> usesDeps = new HashSet<>();
                        java.util.List<String> classConfigProps = new java.util.ArrayList<>();
                        Map<String, java.util.List<String>> methodConfigProps = new java.util.HashMap<>();
                        try (java.io.InputStream in = classInfo.getResource().open()) {
                            ClassReader cr = new ClassReader(in);
                            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                                @Override
                                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                    return new FieldVisitor(Opcodes.ASM9) {
                                        boolean autowired = false;
                                        String configKey = null;

                                        @Override
                                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                            String ann = Type.getType(desc).getClassName();
                                            if ("org.springframework.beans.factory.annotation.Autowired".equals(ann)) {
                                                autowired = true;
                                            }
                                            AnnotationVisitor parent = super.visitAnnotation(desc, visible);
                                            if (ann.endsWith(".Value") || ann.endsWith(".ConfigurationProperty")) {
                                                return new AnnotationVisitor(Opcodes.ASM9, parent) {
                                                    @Override
                                                    public void visit(String name, Object value) {
                                                        if (value instanceof String && (name == null || "value".equals(name) || "name".equals(name))) {
                                                            String v = (String) value;
                                                            if (v.startsWith("${") && v.endsWith("}")) {
                                                                v = v.substring(2, v.length() - 1);
                                                                int idx = v.indexOf(':');
                                                                if (idx > -1) v = v.substring(0, idx);
                                                            }
                                                            configKey = v;
                                                        }
                                                    }

                                                    @Override
                                                    public void visitEnd() {
                                                        if (configKey != null) {
                                                            classConfigProps.add(configKey);
                                                        }
                                                        super.visitEnd();
                                                    }
                                                };
                                            }
                                            return parent;
                                        }

                                        @Override
                                        public void visitEnd() {
                                            if (autowired) {
                                                String type = Type.getType(descriptor).getClassName();
                                                usesDeps.add(type);
                                            }
                                            super.visitEnd();
                                        }
                                    };
                                }

                                @Override
                                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                    String sig = name + descriptor;
                                    if ("<init>".equals(name)) {
                                        for (Type t : Type.getArgumentTypes(descriptor)) {
                                            usesDeps.add(t.getClassName());
                                        }
                                    }
                                    methodNames.add(sig);
                                    session.run(
                                            "MERGE (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig})",
                                            Values.parameters("cls", cls, "sig", sig));
                                    java.util.List<String> mprops = new java.util.ArrayList<>();
                                    methodConfigProps.put(sig, mprops);
                                    return new MethodVisitor(Opcodes.ASM9) {
                                        @Override
                                        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                                            String ann = Type.getType(descriptor).getClassName();
                                            AnnotationVisitor parent = super.visitParameterAnnotation(parameter, descriptor, visible);
                                            if (ann.endsWith(".Value") || ann.endsWith(".ConfigurationProperty")) {
                                                return new AnnotationVisitor(Opcodes.ASM9, parent) {
                                                    String key = null;

                                                    @Override
                                                    public void visit(String name, Object value) {
                                                        if (value instanceof String && (name == null || "value".equals(name) || "name".equals(name))) {
                                                            String v = (String) value;
                                                            if (v.startsWith("${") && v.endsWith("}")) {
                                                                v = v.substring(2, v.length() - 1);
                                                                int idx = v.indexOf(':');
                                                                if (idx > -1) v = v.substring(0, idx);
                                                            }
                                                            key = v;
                                                        }
                                                    }

                                                    @Override
                                                    public void visitEnd() {
                                                        if (key != null) {
                                                            mprops.add(key);
                                                        }
                                                        super.visitEnd();
                                                    }
                                                };
                                            }
                                            return parent;
                                        }
                                        @Override
                                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                            String ann = org.objectweb.asm.Type.getType(descriptor).getClassName();
                                            methodAnnos.computeIfAbsent(sig, k -> new java.util.ArrayList<>()).add(ann);
                                            AnnotationVisitor parent = super.visitAnnotation(descriptor, visible);
                                            if (MAPPING_VERBS.containsKey(ann) || REQUEST_MAPPING.equals(ann)) {
                                                return new AnnotationVisitor(Opcodes.ASM9, parent) {
                                                    String route = null;
                                                    String verb = MAPPING_VERBS.get(ann);

                                                    @Override
                                                    public void visit(String name, Object value) {
                                                        if (("value".equals(name) || "path".equals(name)) && value instanceof String) {
                                                            route = (String) value;
                                                        }
                                                    }

                                                    @Override
                                                    public AnnotationVisitor visitArray(String name) {
                                                        if ("value".equals(name) || "path".equals(name)) {
                                                            return new AnnotationVisitor(Opcodes.ASM9) {
                                                                String first = null;

                                                                @Override
                                                                public void visit(String n, Object v) {
                                                                    if (v instanceof String && first == null) {
                                                                        first = (String) v;
                                                                    }
                                                                }

                                                                @Override
                                                                public void visitEnd() {
                                                                    if (first != null) {
                                                                        route = first;
                                                                    }
                                                                }
                                                            };
                                                        } else if ("method".equals(name)) {
                                                            return new AnnotationVisitor(Opcodes.ASM9) {
                                                                @Override
                                                                public void visitEnum(String n, String desc, String value) {
                                                                    if (verb == null) {
                                                                        verb = value;
                                                                    }
                                                                }
                                                            };
                                                        }
                                                        return super.visitArray(name);
                                                    }

                                                    @Override
                                                    public void visitEnum(String name, String descriptor, String value) {
                                                        if ("method".equals(name)) {
                                                            verb = value;
                                                        }
                                                    }

                                                    @Override
                                                    public void visitEnd() {
                                                        if (route != null) {
                                                            methodRoutes.put(sig, route);
                                                        }
                                                        if (verb != null) {
                                                            methodVerbs.put(sig, verb);
                                                        }
                                                        super.visitEnd();
                                                    }
                                                };
                                            } else if ("org.springframework.context.event.EventListener".equals(ann)) {
                                                return new AnnotationVisitor(Opcodes.ASM9, parent) {
                                                    String evType = null;

                                                    @Override
                                                    public void visit(String name, Object value) {
                                                        if (("classes".equals(name) || "value".equals(name)) && value instanceof Type) {
                                                            evType = ((Type) value).getClassName();
                                                        }
                                                    }

                                                    @Override
                                                    public AnnotationVisitor visitArray(String name) {
                                                        if ("classes".equals(name) || "value".equals(name)) {
                                                            return new AnnotationVisitor(Opcodes.ASM9) {
                                                                String first = null;

                                                                @Override
                                                                public void visit(String n, Object v) {
                                                                    if (v instanceof Type && first == null) {
                                                                        first = ((Type) v).getClassName();
                                                                    }
                                                                }

                                                                @Override
                                                                public void visitEnd() {
                                                                    if (first != null) {
                                                                        evType = first;
                                                                    }
                                                                }
                                                            };
                                                        }
                                                        return super.visitArray(name);
                                                    }

                                                    @Override
                                                    public void visitEnd() {
                                                        if (evType != null) {
                                                            eventTypes.put(sig, evType);
                                                        }
                                                        super.visitEnd();
                                                    }
                                                };
                                            } else if ("org.springframework.scheduling.annotation.Scheduled".equals(ann)) {
                                                return new AnnotationVisitor(Opcodes.ASM9, parent) {
                                                    String cron = null;

                                                    @Override
                                                    public void visit(String name, Object value) {
                                                        if ("cron".equals(name) && value instanceof String) {
                                                            cron = (String) value;
                                                        }
                                                    }

                                                    @Override
                                                    public void visitEnd() {
                                                        if (cron != null) {
                                                            cronExprs.put(sig, cron);
                                                        }
                                                        super.visitEnd();
                                                    }
                                                };
                                            }
                                            return parent;
                                        }
                                        @Override
                                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                            String tgtCls = owner.replace('/', '.');
                                            String tgtSig = name + descriptor;
                                            calls.computeIfAbsent(sig, k -> new HashSet<>())
                                                    .add(tgtCls + "|" + tgtSig);
                                        }
                                    };
                                }
                            }, 0);
                        }
                        if (!methodNames.isEmpty()) {
                            LOGGER.info("Methods in " + cls + ": " + methodNames);
                        }

                        for (Map.Entry<String, java.util.List<String>> ma : methodAnnos.entrySet()) {
                            session.run(
                                    "MATCH (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig}) SET m.annotations=$annos",
                                    Values.parameters("cls", cls, "sig", ma.getKey(), "annos", ma.getValue()));
                        }

                        for (Map.Entry<String, String> en : methodRoutes.entrySet()) {
                            session.run(
                                    "MATCH (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig}) SET m.httpRoute=$route",
                                    Values.parameters("cls", cls, "sig", en.getKey(), "route", en.getValue()));
                        }


                        for (Map.Entry<String, String> en : methodVerbs.entrySet()) {
                            session.run(
                                    "MATCH (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig}) SET m.httpMethod=$verb",
                                    Values.parameters("cls", cls, "sig", en.getKey(), "verb", en.getValue()));
                        }

                        for (Map.Entry<String, String> en : eventTypes.entrySet()) {
                            session.run(
                                    "MATCH (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig}) SET m.eventType=$type",
                                    Values.parameters("cls", cls, "sig", en.getKey(), "type", en.getValue()));
                        }

                        for (Map.Entry<String, String> en : cronExprs.entrySet()) {
                            session.run(
                                    "MATCH (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig}) SET m.cron=$cron",
                                    Values.parameters("cls", cls, "sig", en.getKey(), "cron", en.getValue()));
                        }

                        if (!classConfigProps.isEmpty()) {
                            session.run(
                                    "MATCH (c:" + NodeLabel.CLASS + " {name:$cls}) SET c.configProperties=$props",
                                    Values.parameters("cls", cls, "props", classConfigProps));
                        }
                        for (Map.Entry<String, java.util.List<String>> en : methodConfigProps.entrySet()) {
                            if (!en.getValue().isEmpty()) {
                                session.run(
                                        "MATCH (m:" + NodeLabel.METHOD + " {class:$cls, signature:$sig}) SET m.configProperties=$props",
                                        Values.parameters("cls", cls, "sig", en.getKey(), "props", en.getValue()));
                            }
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
                                    "MATCH (s:" + NodeLabel.CLASS + " {name:$src}), (t:" + NodeLabel.CLASS + " {name:$tgt}) MERGE (s)-[:DEPENDS_ON]->(t)",
                                    Values.parameters("src", cls, "tgt", depName));
                        }

                        for (String use : usesDeps) {
                            session.run(
                                    "MERGE (u:" + NodeLabel.CLASS + " {name:$dep})",
                                    Values.parameters("dep", use));
                            session.run(
                                    "MATCH (s:" + NodeLabel.CLASS + " {name:$src}), (t:" + NodeLabel.CLASS + " {name:$tgt}) MERGE (s)-[:USES]->(t)",
                                    Values.parameters("src", cls, "tgt", use));
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
