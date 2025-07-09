package tech.softwareologists.cli;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
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
            ProjectDirImporter.importDir(outDir.toFile(), driver);

            try (Session session = driver.session()) {
                List<Record> result = session.run("MATCH (c:" + NodeLabel.CLASS + " {name:'foo.Bar'}) RETURN c").list();
                if (result.isEmpty()) {
                    throw new AssertionError("Node foo.Bar not persisted");
                }
            }
        }
    }
}
