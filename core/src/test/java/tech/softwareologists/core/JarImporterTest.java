package tech.softwareologists.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;

import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

public class JarImporterTest {
    @Test
    public void importJar_emptyJar_noException() throws IOException {
        File jar = File.createTempFile("empty", ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            // create empty jar
        }
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            JarImporter.importJar(jar, db.getDriver());
        }
    }

    @Test
    public void importJar_sampleJar_logsClasses() throws Exception {
        Path srcDir = Files.createTempDirectory("src");
        Path pkgDir = srcDir.resolve("foo");
        Files.createDirectories(pkgDir);
        Path srcFile = pkgDir.resolve("Bar.java");
        Files.write(srcFile, "package foo; public class Bar {}".getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available");
        }
        int result = compiler.run(null, null, null, srcFile.toString());
        if (result != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        File jar = File.createTempFile("sample", ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            JarEntry entry = new JarEntry("foo/Bar.class");
            jos.putNextEntry(entry);
            Files.copy(pkgDir.resolve("Bar.class"), jos);
            jos.closeEntry();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(out, new SimpleFormatter());
        Logger logger = Logger.getLogger(JarImporter.class.getName());
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            JarImporter.importJar(jar, db.getDriver());
        }

        handler.flush();
        logger.removeHandler(handler);
        String logs = out.toString(StandardCharsets.UTF_8.name());
        if (!logs.contains("foo.Bar")) {
            throw new AssertionError("Expected class name not found in logs: " + logs);
        }
    }

    @Test
    public void importJar_sampleJar_persistsNodes() throws Exception {
        Path srcDir = Files.createTempDirectory("src2");
        Path pkgDir = srcDir.resolve("baz");
        Files.createDirectories(pkgDir);
        Path srcFile = pkgDir.resolve("Qux.java");
        Files.write(srcFile, "package baz; public class Qux {}".getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available");
        }
        int result = compiler.run(null, null, null, srcFile.toString());
        if (result != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        File jar = File.createTempFile("qux", ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            JarEntry entry = new JarEntry("baz/Qux.class");
            jos.putNextEntry(entry);
            Files.copy(pkgDir.resolve("Qux.class"), jos);
            jos.closeEntry();
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            try (Session session = driver.session()) {
                java.util.List<Record> res = session.run("MATCH (c:" + NodeLabel.CLASS + " {name:'baz.Qux'}) RETURN c").list();
                if (res.isEmpty()) {
                    throw new AssertionError("Node baz.Qux not persisted");
                }
            }
        }
    }

    @Test
    public void importJar_dependency_createsEdge() throws Exception {
        Path srcDir = Files.createTempDirectory("srcdep");
        Path pkgDir = srcDir.resolve("dep");
        Files.createDirectories(pkgDir);

        Path aFile = pkgDir.resolve("A.java");
        Files.write(aFile, "package dep; public class A {}".getBytes(StandardCharsets.UTF_8));

        Path bFile = pkgDir.resolve("B.java");
        Files.write(bFile, "package dep; public class B { A a; }".getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available");
        }
        int res = compiler.run(null, null, null, aFile.toString(), bFile.toString());
        if (res != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        File jar = File.createTempFile("dep", ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            JarEntry entryA = new JarEntry("dep/A.class");
            jos.putNextEntry(entryA);
            Files.copy(pkgDir.resolve("A.class"), jos);
            jos.closeEntry();
            JarEntry entryB = new JarEntry("dep/B.class");
            jos.putNextEntry(entryB);
            Files.copy(pkgDir.resolve("B.class"), jos);
            jos.closeEntry();
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            try (Session session = driver.session()) {
                java.util.List<Record> rel = session.run(
                                "MATCH (s:" + NodeLabel.CLASS + " {name:'dep.B'})-[:DEPENDS_ON]->(t:" + NodeLabel.CLASS + " {name:'dep.A'}) RETURN t")
                        .list();
                if (rel.isEmpty()) {
                    throw new AssertionError("Dependency edge not created");
                }
            }
        }
    }

    @Test
    public void importJar_methodCall_createsEdge() throws Exception {
        Path srcDir = Files.createTempDirectory("srcmethod");
        Path pkgDir = srcDir.resolve("calls");
        Files.createDirectories(pkgDir);

        Path calleeFile = pkgDir.resolve("Callee.java");
        Files.write(calleeFile, "package calls; public class Callee { public void methodB() {} }".getBytes(StandardCharsets.UTF_8));

        Path callerFile = pkgDir.resolve("Caller.java");
        Files.write(callerFile, "package calls; public class Caller { public void methodA() { new Callee().methodB(); } }".getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available");
        }
        int res = compiler.run(null, null, null, calleeFile.toString(), callerFile.toString());
        if (res != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        File jar = File.createTempFile("calls", ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            for (String n : new String[]{"calls/Callee.class", "calls/Caller.class"}) {
                jos.putNextEntry(new JarEntry(n));
                Files.copy(pkgDir.resolve(n.substring(n.lastIndexOf('/') + 1)), jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            try (Session session = driver.session()) {
                java.util.List<Record> caller = session.run(
                                "MATCH (m:" + NodeLabel.METHOD + " {class:'calls.Caller', signature:'methodA()V'}) RETURN m")
                        .list();
                if (caller.isEmpty()) {
                    throw new AssertionError("Caller method node not created");
                }

                java.util.List<Record> callee = session.run(
                                "MATCH (m:" + NodeLabel.METHOD + " {class:'calls.Callee', signature:'methodB()V'}) RETURN m")
                        .list();
                if (callee.isEmpty()) {
                    throw new AssertionError("Callee method node not created");
                }

                java.util.List<Record> rel = session.run(
                                "MATCH (s:" + NodeLabel.METHOD + " {class:'calls.Caller', signature:'methodA()V'})-[:CALLS]->(t:" + NodeLabel.METHOD + " {class:'calls.Callee', signature:'methodB()V'}) RETURN t")
                        .list();
                if (rel.isEmpty()) {
                    throw new AssertionError("CALLS edge not created");
                }
            }
        }
    }
}
