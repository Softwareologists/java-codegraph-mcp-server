package tech.softwareologists.cli;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JarWatcherTest {
    @Test
    public void jarCreation_logged() throws Exception {
        Path dir = Files.createTempDirectory("watch-test");

        Logger logger = Logger.getLogger(JarWatcher.class.getName());
        logger.setUseParentHandlers(false);
        RecordingHandler handler = new RecordingHandler();
        logger.addHandler(handler);

        JarWatcher watcher = new JarWatcher(dir);
        watcher.start();

        Files.createFile(dir.resolve("sample.jar"));

        for (int i = 0; i < 10 && handler.records.isEmpty(); i++) {
            Thread.sleep(100);
        }

        watcher.close();
        logger.removeHandler(handler);

        if (handler.records.isEmpty()) {
            throw new AssertionError("No log records emitted");
        }
        String msg = handler.records.get(0).getMessage();
        if (!msg.contains("sample.jar")) {
            throw new AssertionError("Log did not contain jar name: " + msg);
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
