package tech.softwareologists.cli;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches a directory for newly created JAR files and logs their paths.
 */
import java.util.function.Consumer;

public class JarWatcher implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(JarWatcher.class.getName());

    private final Path directory;
    private final WatchService watchService;
    private final Consumer<Path> onJar;
    private Thread thread;

    /**
     * Creates a watcher for the given directory. The watcher is not started
     * until {@link #start()} is called.
     */
    public JarWatcher(Path directory) throws IOException {
        this(directory, p -> {});
    }

    public JarWatcher(Path directory, Consumer<Path> onJar) throws IOException {
        this.directory = directory;
        this.onJar = onJar;
        this.watchService = directory.getFileSystem().newWatchService();
        directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    }

    /** Starts watching in a background thread. */
    public void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this::process);
        thread.setDaemon(true);
        thread.start();
    }

    private void process() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path created = directory.resolve((Path) event.context());
                        if (created.toString().endsWith(".jar")) {
                            Path abs = created.toAbsolutePath();
                            LOGGER.info("Detected JAR: " + abs);
                            onJar.accept(abs);
                        }
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException ignored) {
            // exit
        }
    }

    @Override
    public void close() throws IOException {
        if (thread != null) {
            thread.interrupt();
        }
        watchService.close();
    }
}
