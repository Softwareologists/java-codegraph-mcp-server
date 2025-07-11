package tech.softwareologists.cli;

import org.junit.Test;
import tech.softwareologists.core.ManifestGenerator;
import tech.softwareologists.core.QueryService;

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

        QueryService qs = new QueryService() {
            @Override
            public java.util.List<String> findCallers(String className) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findImplementations(String interfaceName) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findSubclasses(String className, int depth) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findDependencies(String className, Integer depth) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findBeansWithAnnotation(String annotation) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> searchByAnnotation(String annotation, String targetType) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findHttpEndpoints(String basePath, String httpMethod) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findControllersUsingService(String serviceClassName) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findEventListeners(String eventType) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findScheduledTasks() {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findConfigPropertyUsage(String propertyKey) {
                return java.util.Collections.emptyList();
            }

            @Override
            public String getPackageHierarchy(String rootPackage, Integer depth) {
                return "{}";
            }

            @Override
            public String getGraphStatistics(Integer topN) {
                return "{}";
            }

            @Override
            public void exportGraph(String format, String outputPath) {
                // no-op for testing
            }
        };
        new StdioMcpServer(qs, in, new PrintStream(out)).run();

        String output = out.toString(StandardCharsets.UTF_8);
        String manifest = ManifestGenerator.generate();
        int first = output.indexOf(manifest);
        int second = output.indexOf(manifest, first + manifest.length());
        if (first == -1 || second == -1) {
            throw new AssertionError("Manifest not printed twice:\n" + output);
        }
        if (!manifest.contains("findHttpEndpoints") || !manifest.contains("getGraphStatistics")) {
            throw new AssertionError("Manifest missing new capabilities: " + manifest);
        }
    }

    @Test
    public void findCallersRequest_returnsJsonArray() {
        String request = "{\"findCallers\":\"A\"}\n";
        ByteArrayInputStream in = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        QueryService qs = new QueryService() {
            @Override
            public java.util.List<String> findCallers(String className) {
                return java.util.Collections.singletonList("B");
            }

            @Override
            public java.util.List<String> findImplementations(String interfaceName) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findSubclasses(String className, int depth) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findDependencies(String className, Integer depth) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findBeansWithAnnotation(String annotation) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> searchByAnnotation(String annotation, String targetType) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findHttpEndpoints(String basePath, String httpMethod) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findControllersUsingService(String serviceClassName) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findEventListeners(String eventType) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findScheduledTasks() {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> findConfigPropertyUsage(String propertyKey) {
                return java.util.Collections.emptyList();
            }

            @Override
            public String getPackageHierarchy(String rootPackage, Integer depth) {
                return "{}";
            }

            @Override
            public String getGraphStatistics(Integer topN) {
                return "{}";
            }

            @Override
            public void exportGraph(String format, String outputPath) {
                // no-op for testing
            }
        };

        new StdioMcpServer(qs, in, new PrintStream(out)).run();

        String output = out.toString(StandardCharsets.UTF_8).trim();
        int newline = output.lastIndexOf('\n');
        String response = output.substring(newline + 1).trim();
        if (!"[\"B\"]".equals(response)) {
            throw new AssertionError("Unexpected response: " + response);
        }
    }
}
