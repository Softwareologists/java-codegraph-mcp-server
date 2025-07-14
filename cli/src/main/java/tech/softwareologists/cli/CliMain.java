package tech.softwareologists.cli;

import tech.softwareologists.core.JarImporter;
import tech.softwareologists.core.QueryService;
import tech.softwareologists.core.QueryServiceImpl;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.cli.ProjectDirImporter;
import tech.softwareologists.cli.SseMcpServer;

import java.util.logging.Logger;

import java.io.PrintStream;
import java.nio.file.Paths;

/**
 * Simple command line entry point for the MCP CLI.
 */
public class CliMain {
    /** Usage string shown when arguments are missing or --help is supplied. */
    public static final String USAGE = "Usage: cli --watch-dir <dir> [--stdio] [--sse-port <port>] [--project-dir <dir>]";
    private static final Logger LOGGER = Logger.getLogger(CliMain.class.getName());

    public static void main(String[] args) {
        int code = run(args, System.out);
        if (code != 0) {
            System.exit(code);
        }
    }

    /**
     * Runs the CLI with the given arguments and output stream.
     *
     * @param args command line arguments
     * @param out where to print output
     * @return exit code
     */
    static int run(String[] args, PrintStream out) {
        String watchDir = null;
        String projectDir = null;
        boolean stdio = false;
        int ssePort = -1;
        boolean help = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help":
                case "-h":
                    help = true;
                    break;
                case "--watch-dir":
                    if (i + 1 >= args.length) {
                        out.println(USAGE);
                        return 1;
                    }
                    watchDir = args[++i];
                    break;
                case "--stdio":
                    stdio = true;
                    break;
                case "--sse-port":
                    if (i + 1 >= args.length) {
                        out.println(USAGE);
                        return 1;
                    }
                    ssePort = Integer.parseInt(args[++i]);
                    break;
                case "--project-dir":
                    if (i + 1 >= args.length) {
                        out.println(USAGE);
                        return 1;
                    }
                    projectDir = args[++i];
                    break;
                default:
                    out.println("Unknown option: " + arg);
                    out.println(USAGE);
                    return 1;
            }
        }

        if (help || watchDir == null) {
            out.println(USAGE);
            return help ? 0 : 1;
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j();
             JarWatcher watcher = new JarWatcher(Paths.get(watchDir), p -> JarImporter.importJar(p.toFile(), db.getDriver()))) {
            java.nio.file.Files.list(Paths.get(watchDir))
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> JarImporter.importJar(p.toFile(), db.getDriver()));

            if (projectDir != null) {
                int imported = ProjectDirImporter.importDirectory(Paths.get(projectDir).toFile(), db.getDriver());
                LOGGER.info("Imported " + imported + " classes from project directory");
            }

            watcher.start();

            QueryService service = new QueryServiceImpl(db.getDriver());
            SseMcpServer sse = null;
            if (ssePort > 0) {
                sse = new SseMcpServer(ssePort, service);
                sse.start();
                out.println("SSE server started on port " + sse.getPort());
            }
            if (stdio) {
                new StdioMcpServer(service, System.in, out).run();
            } else if (sse != null) {
                try {
                    new java.util.concurrent.CountDownLatch(1).await();
                } catch (InterruptedException ignored) {}
            }
            if (sse != null) {
                sse.stop();
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace(out);
            return 1;
        }
    }
}
