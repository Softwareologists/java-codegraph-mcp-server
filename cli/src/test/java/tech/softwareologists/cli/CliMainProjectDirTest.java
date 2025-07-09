package tech.softwareologists.cli;

import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class CliMainProjectDirTest {
    @Test
    public void projectDirFlag_importsClasses() throws Exception {
        Path srcDir = Files.createTempDirectory("cliProjSrc");
        Path pkgDir = srcDir.resolve("pd");
        Files.createDirectories(pkgDir);
        Path srcFile = pkgDir.resolve("Foo.java");
        Files.writeString(srcFile, "package pd; public class Foo {}", StandardCharsets.UTF_8);

        Path outDir = Files.createTempDirectory("cliProjClasses");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available");
        }
        int res = compiler.run(null, null, null, "-d", outDir.toString(), srcFile.toString());
        if (res != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        Path watch = Files.createTempDirectory("watch");

        Logger logger = Logger.getLogger(CliMain.class.getName());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        ByteArrayOutputStream logOut = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(logOut, new SimpleFormatter());
        logger.addHandler(handler);

        int code = CliMain.run(new String[]{"--watch-dir", watch.toString(), "--project-dir", outDir.toString()}, new PrintStream(new ByteArrayOutputStream()));

        handler.flush();
        logger.removeHandler(handler);
        String logs = logOut.toString(StandardCharsets.UTF_8.name());
        if (code != 0) {
            throw new AssertionError("CLI exited with code " + code);
        }
        if (!logs.contains("Imported 1")) {
            throw new AssertionError("Import log not found: " + logs);
        }
    }
}
