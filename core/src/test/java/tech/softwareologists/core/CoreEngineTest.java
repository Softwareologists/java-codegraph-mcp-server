package tech.softwareologists.core;

import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** Tests for {@link CoreEngine}. */
public class CoreEngineTest {
    @Test
    public void importJar_thenQueryServiceFindsCaller() throws Exception {
        Path src = Files.createTempDirectory("coreeng");
        Path pkg = src.resolve("dep");
        Files.createDirectories(pkg);
        Path a = pkg.resolve("A.java");
        Files.write(a, "package dep; public class A {}".getBytes(StandardCharsets.UTF_8));
        Path b = pkg.resolve("B.java");
        Files.write(b, "package dep; public class B { A a; }".getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, a.toString(), b.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        File jar = File.createTempFile("coreeng", ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            for (String n : new String[]{"dep/A.class", "dep/B.class"}) {
                jos.putNextEntry(new JarEntry(n));
                Files.copy(pkg.resolve(n.substring(n.lastIndexOf('/') + 1)), jos);
                jos.closeEntry();
            }
        }

        try (CoreEngine engine = new CoreEngineImpl()) {
            engine.importJar(jar);
            tech.softwareologists.core.QueryResult<String> callers = engine.getQueryService().findCallers("dep.A", null, null, null);
            if (callers.getItems().size() != 1 || !callers.getItems().get(0).equals("dep.B")) {
                throw new AssertionError("Unexpected callers: " + callers.getItems());
            }

            String manifest = engine.getManifest();
            if (!manifest.contains("findCallers")) {
                throw new AssertionError("Manifest missing method name");
            }
        }
    }
}
