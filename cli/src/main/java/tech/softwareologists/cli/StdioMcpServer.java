package tech.softwareologists.cli;

import tech.softwareologists.core.ManifestGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Very small MCP server implementation that communicates over STDIN/STDOUT.
 * <p>
 * The server prints the manifest JSON on startup and then echoes back any JSON
 * requests it receives. If a request contains the word {@code "manifest"}, the
 * manifest is returned again.
 */
public class StdioMcpServer implements Runnable {
    private final BufferedReader in;
    private final PrintStream out;

    /** Creates a server that reads from {@link System#in} and writes to {@link System#out}. */
    public StdioMcpServer() {
        this(System.in, System.out);
    }

    /**
     * Creates a server with the given input and output streams. This is mainly
     * used for unit testing.
     */
    StdioMcpServer(InputStream in, PrintStream out) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = out;
    }

    /**
     * Starts the server loop. This method blocks until the input stream is
     * closed.
     */
    @Override
    public void run() {
        String manifest = ManifestGenerator.generate();
        out.println(manifest);
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.contains("\"manifest\"")) {
                    out.println(manifest);
                } else {
                    out.println(trimmed);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
