package tech.softwareologists.cli;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProjectDirImporterTest {
    @Test
    public void importDir_compiledProject_persistsNodes() throws Exception {
        Path srcDir = Files.createTempDirectory("projsrc");
        Path pkgDir = srcDir.resolve("foo");
        Files.createDirectories(pkgDir);
        Path srcFile = pkgDir.resolve("Bar.java");
        Files.write(srcFile, "package foo; public class Bar {}".getBytes(StandardCharsets.UTF_8));

        Path outDir = Files.createTempDirectory("projclasses");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available");
        }
        int res = compiler.run(null, null, null, "-d", outDir.toString(), srcFile.toString());
        if (res != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            int count = ProjectDirImporter.importDirectory(outDir.toFile(), driver);
            if (count != 1) {
                throw new AssertionError("Expected 1 class but was: " + count);
            }

            try (Session session = driver.session()) {
                List<Record> result = session.run("MATCH (c:" + NodeLabel.CLASS + " {name:'foo.Bar'}) RETURN c").list();
                if (result.isEmpty()) {
                    throw new AssertionError("Node foo.Bar not persisted");
                }
            }
        }
    }

    @Test
    public void importDir_methodCall_createsEdge() throws Exception {
        Path srcDir = Files.createTempDirectory("srcmethoddir");
        Path pkgDir = srcDir.resolve("calls");
        Files.createDirectories(pkgDir);
        Path calleeFile = pkgDir.resolve("Callee.java");
        Files.write(calleeFile, "package calls; public class Callee { public void m() {} }".getBytes(StandardCharsets.UTF_8));
        Path callerFile = pkgDir.resolve("Caller.java");
        Files.write(callerFile, "package calls; public class Caller { public void n() { new Callee().m(); } }".getBytes(StandardCharsets.UTF_8));

        Path outDir = Files.createTempDirectory("methodclasses");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available");
        }
        int res = compiler.run(null, null, null, "-d", outDir.toString(), calleeFile.toString(), callerFile.toString());
        if (res != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            ProjectDirImporter.importDirectory(outDir.toFile(), driver);

            try (Session session = driver.session()) {
                List<Record> rel = session.run(
                        "MATCH (s:" + NodeLabel.METHOD + " {class:'calls.Caller', signature:'n()V'})-[:CALLS]->(t:" + NodeLabel.METHOD + " {class:'calls.Callee', signature:'m()V'}) RETURN t")
                        .list();
                if (rel.isEmpty()) {
                    throw new AssertionError("CALLS edge not created");
                }
            }
        }
    }
}
