package tech.softwareologists.cli;

import java.io.PrintStream;

/**
 * Simple command line entry point for the MCP CLI.
 */
public class CliMain {
    /** Usage string shown when arguments are missing or --help is supplied. */
    public static final String USAGE = "Usage: cli --watch-dir <dir> [--stdio]";

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
        boolean stdio = false;
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

        // Placeholder for actual functionality
        out.println("Starting with watchDir=" + watchDir + " stdio=" + stdio);
        return 0;
    }
}
