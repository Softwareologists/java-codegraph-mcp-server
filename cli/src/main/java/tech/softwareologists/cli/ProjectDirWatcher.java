package tech.softwareologists.cli;

import org.neo4j.driver.Driver;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches a project directory for .java or .class file changes and re-imports
 * the directory using {@link ProjectDirImporter} after a short debounce period.
 */
public class ProjectDirWatcher implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ProjectDirWatcher.class.getName());
    private static final long DEFAULT_DEBOUNCE_MS = 500;

    private final Path directory;
    private final Driver driver;
    private final WatchService watchService;
    private final Set<Path> registeredDirs = new HashSet<>();
    private final long debounceMs;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> future;
    private Thread thread;

    private void registerRecursive(Path dir) throws IOException {
        Files.walk(dir)
                .filter(Files::isDirectory)
                .forEach(p -> {
                    try {
                        if (registeredDirs.add(p)) {
                            p.register(watchService,
                                    StandardWatchEventKinds.ENTRY_CREATE,
                                    StandardWatchEventKinds.ENTRY_MODIFY);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Creates a watcher for the given directory. The watcher is not started
     * until {@link #start()} is called.
     */
    public ProjectDirWatcher(Path directory, Driver driver) throws IOException {
        this(directory, driver, DEFAULT_DEBOUNCE_MS);
    }

    /**
     * Creates a watcher with a custom debounce delay.
     */
    public ProjectDirWatcher(Path directory, Driver driver, long debounceMs) throws IOException {
        this.directory = directory;
        this.driver = driver;
        this.debounceMs = debounceMs;
        this.watchService = directory.getFileSystem().newWatchService();
        registerRecursive(directory);
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
                boolean trigger = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path p = ((Path) key.watchable()).resolve((Path) event.context());
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(p)) {
                        try {
                            registerRecursive(p);
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to register dir", e);
                        }
                    }
                    String name = p.getFileName().toString();
                    if (name.endsWith(".java") || name.endsWith(".class")) {
                        trigger = true;
                    }
                }
                if (!key.reset()) {
                    break;
                }
                if (trigger) {
                    scheduleImport();
                }
            }
        } catch (InterruptedException ignored) {
            // exit
        }
    }

    private synchronized void scheduleImport() {
        if (future != null) {
            future.cancel(false);
        }
        future = executor.schedule(() -> {
            try {
                int count = ProjectDirImporter.importDirectory(directory.toFile(), driver);
                LOGGER.info("Imported " + count + " classes from project directory");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to import project directory", e);
            }
        }, debounceMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        if (thread != null) {
            thread.interrupt();
        }
        synchronized (this) {
            if (future != null) {
                future.cancel(false);
            }
        }
        executor.shutdownNow();
        watchService.close();
    }
}
