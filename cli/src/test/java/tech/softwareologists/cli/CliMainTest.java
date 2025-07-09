package tech.softwareologists.cli;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class CliMainTest {
    @Test
    public void help_printsUsage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int code = CliMain.run(new String[] {"--help"}, new PrintStream(baos));
        String output = baos.toString().trim();
        if (!CliMain.USAGE.equals(output)) {
            throw new AssertionError("Expected usage but was: " + output);
        }
        if (code != 0) {
            throw new AssertionError("Expected exit code 0 but was: " + code);
        }
    }
}
