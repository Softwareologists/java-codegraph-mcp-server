package tech.softwareologists.cli;

import org.junit.Test;
import tech.softwareologists.cli.JarWatcher;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CliIntegrationTest {
    @Test
    public void importAndQuery_sampleJar_returnsCaller() throws Exception {
        Path watch = Files.createTempDirectory("watch");

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(JarWatcher.class.getName());
        logger.setUseParentHandlers(false);
        logger.setLevel(java.util.logging.Level.INFO);
        java.io.ByteArrayOutputStream logOut = new java.io.ByteArrayOutputStream();
        java.util.logging.Handler handler = new java.util.logging.StreamHandler(logOut, new java.util.logging.SimpleFormatter());
        logger.addHandler(handler);

        Path jar = watch.resolve("callers.jar");
        createSampleJar(jar);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("{\"findCallers\":\"dep.A\"}\n".getBytes(StandardCharsets.UTF_8)));
        CliMain.run(new String[]{"--watch-dir", watch.toString(), "--stdio"}, new PrintStream(out, true));
        System.setIn(originalIn);
        handler.flush();
        logger.removeHandler(handler);
        System.out.println("LOGS:" + logOut.toString());

        String output = out.toString(StandardCharsets.UTF_8.name());
        System.out.println(output);
        if (!output.contains("\"dep.B\"")) {
            throw new AssertionError("Expected dep.B in output but was:\n" + output);
        }
    }

    private static void createSampleJar(Path jar) throws Exception {
        Path srcDir = Files.createTempDirectory("src");
        Path pkg = srcDir.resolve("dep");
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve("A.java"), "package dep; public class A {}");
        Files.writeString(pkg.resolve("B.java"), "package dep; public class B { A a; }");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int res = compiler.run(null, null, null, "-d", srcDir.toString(),
                pkg.resolve("A.java").toString(), pkg.resolve("B.java").toString());
        if (res != 0) {
            throw new IllegalStateException("Compilation failed");
        }

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar.toFile()))) {
            JarEntry a = new JarEntry("dep/A.class");
            jos.putNextEntry(a);
            Files.copy(pkg.resolve("A.class"), jos);
            jos.closeEntry();
            JarEntry b = new JarEntry("dep/B.class");
            jos.putNextEntry(b);
            Files.copy(pkg.resolve("B.class"), jos);
            jos.closeEntry();
        }
    }
}
