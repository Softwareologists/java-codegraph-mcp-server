package tech.softwareologists.ij;

import org.junit.Test;
import tech.softwareologists.core.ManifestGenerator;
import tech.softwareologists.core.QueryService;
import tech.softwareologists.ij.server.HttpMcpServer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class HttpMcpServerTest {
    @Test
    public void manifestAndQuery_returnJson() throws Exception {
        QueryService qs = new QueryService() {
            @Override
            public tech.softwareologists.core.QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) {
                return new tech.softwareologists.core.QueryResult<>(java.util.Collections.singletonList("Caller"),1,1,1);
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
                return "{\"nodes\":1}";
            }

            @Override
            public void exportGraph(String format, String outputPath) {
                // no-op for HTTP server tests
            }
        };
        HttpMcpServer server = new HttpMcpServer(0, qs);
        server.start();
        int port = server.getPort();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest manifestReq = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/mcp/manifest"))
                .GET()
                .build();
        HttpResponse<String> manifestResp = client.send(manifestReq, HttpResponse.BodyHandlers.ofString());
        if (!manifestResp.body().equals(ManifestGenerator.generate())) {
            throw new AssertionError("Manifest mismatch: " + manifestResp.body());
        }

        HttpRequest queryReq = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/mcp/query"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"findCallers\":\"X\"}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> queryResp = client.send(queryReq, HttpResponse.BodyHandlers.ofString());
        if (!queryResp.body().equals("[\"Caller\"]")) {
            throw new AssertionError("Unexpected query response: " + queryResp.body());
        }

        HttpRequest statsReq = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/mcp/query"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"getGraphStatistics\":1}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> statsResp = client.send(statsReq, HttpResponse.BodyHandlers.ofString());
        if (!statsResp.body().equals("{\"nodes\":1}")) {
            throw new AssertionError("Unexpected stats response: " + statsResp.body());
        }
        server.stop();
    }

    @Test
    public void findCallersRequest_pagingParametersPassed() throws Exception {
        QueryService qs = new QueryService() {
            @Override
            public tech.softwareologists.core.QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) {
                if (!"A".equals(className) || limit == null || limit != 10 || page == null || page != 2 || pageSize == null || pageSize != 5) {
                    throw new AssertionError("Paging parameters not forwarded");
                }
                return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public String getPackageHierarchy(String rootPackage, Integer depth) { return "{}"; }

            @Override
            public String getGraphStatistics(Integer topN) { return "{}"; }

            @Override
            public void exportGraph(String format, String outputPath) {}
        };

        HttpMcpServer server = new HttpMcpServer(0, qs);
        server.start();
        int port = server.getPort();

        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"findCallers\":{\"className\":\"A\",\"limit\":10,\"page\":2,\"pageSize\":5}}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/mcp/query"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString());
        server.stop();
    }

    @Test
    public void findSubclassesRequest_pagingParametersPassed() throws Exception {
        QueryService qs = new QueryService() {
            @Override
            public tech.softwareologists.core.QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize) {
                if (!"B".equals(className) || depth != 2 || limit == null || limit != 7 || page == null || page != 3 || pageSize == null || pageSize != 4) {
                    throw new AssertionError("Paging parameters not forwarded");
                }
                return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0);
            }

            @Override
            public tech.softwareologists.core.QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public tech.softwareologists.core.QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize) { return new tech.softwareologists.core.QueryResult<>(Collections.emptyList(),1,0,0); }

            @Override
            public String getPackageHierarchy(String rootPackage, Integer depth) { return "{}"; }

            @Override
            public String getGraphStatistics(Integer topN) { return "{}"; }

            @Override
            public void exportGraph(String format, String outputPath) {}
        };

        HttpMcpServer server = new HttpMcpServer(0, qs);
        server.start();
        int port = server.getPort();

        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"findSubclasses\":{\"className\":\"B\",\"depth\":2,\"limit\":7,\"page\":3,\"pageSize\":4}}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/mcp/query"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString());
        server.stop();
    }
}
