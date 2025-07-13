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
            public tech.softwareologists.core.QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
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
            public tech.softwareologists.core.QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.singletonList("B"),1,1,1);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
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

    @Test
    public void findCallersRequest_pagingParametersPassed() {
        String request = "{\"findCallers\":{\"className\":\"A\",\"limit\":10,\"page\":2,\"pageSize\":5}}\n";
        ByteArrayInputStream in = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        QueryService qs = new QueryService() {
            @Override
            public tech.softwareologists.core.QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) {
                if (!"A".equals(className) || limit == null || limit != 10 || page == null || page != 2 || pageSize == null || pageSize != 5) {
                    throw new AssertionError("Paging parameters not forwarded");
                }
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
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
    }

    @Test
    public void findSubclassesRequest_pagingParametersPassed() {
        String request = "{\"findSubclasses\":{\"className\":\"B\",\"depth\":2,\"limit\":7,\"page\":3,\"pageSize\":4}}\n";
        ByteArrayInputStream in = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        QueryService qs = new QueryService() {
            @Override
            public tech.softwareologists.core.QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize) {
                if (!"B".equals(className) || depth != 2 || limit == null || limit != 7 || page == null || page != 3 || pageSize == null || pageSize != 4) {
                    throw new AssertionError("Paging parameters not forwarded");
                }
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.emptyList(),1,0,0);
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
    }
}
