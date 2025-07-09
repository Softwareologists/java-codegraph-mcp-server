package tech.softwareologists.cli;

import org.junit.Test;
import tech.softwareologists.core.ManifestGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class StdioMcpServerTest {
    @Test
    public void manifestRequest_printsManifest() {
        String request = "{\"manifest\":true}\n";
        ByteArrayInputStream in = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new StdioMcpServer(in, new PrintStream(out)).run();

        String output = out.toString(StandardCharsets.UTF_8);
        String manifest = ManifestGenerator.generate();
        int first = output.indexOf(manifest);
        int second = output.indexOf(manifest, first + manifest.length());
        if (first == -1 || second == -1) {
            throw new AssertionError("Manifest not printed twice:\n" + output);
        }
    }
}
