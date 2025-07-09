package tech.softwareologists.cli;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ProjectDirWatcherTest {
    @Test
    public void fileChange_triggersImport() throws Exception {
        Path dir = Files.createTempDirectory("projwatch");

        Logger logger = Logger.getLogger(ProjectDirWatcher.class.getName());
        logger.setUseParentHandlers(false);
        RecordingHandler handler = new RecordingHandler();
        logger.addHandler(handler);

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();

            ProjectDirWatcher watcher = new ProjectDirWatcher(dir, driver, 1000);
            watcher.start();

            Path pkgDir = dir.resolve("p");
            Files.createDirectories(pkgDir);
            Path src = pkgDir.resolve("Foo.java");
            Files.writeString(src, "package p; public class Foo {}\n");
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("Java compiler not available");
            }
            int res = compiler.run(null, null, null, "-d", dir.toString(), src.toString());
            if (res != 0) {
                throw new IllegalStateException("Compilation failed");
            }

            for (int i = 0; i < 20 && handler.records.isEmpty(); i++) {
                Thread.sleep(100);
            }

            watcher.close();

            if (handler.records.isEmpty()) {
                throw new AssertionError("No import triggered");
            }

            try (Session session = driver.session()) {
                List<Record> result = session.run("MATCH (c:" + NodeLabel.CLASS + " {name:'p.Foo'}) RETURN c").list();
                if (result.isEmpty()) {
                    throw new AssertionError("Class Foo not imported");
                }
            }
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static class RecordingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                records.add(record);
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
    }
}
