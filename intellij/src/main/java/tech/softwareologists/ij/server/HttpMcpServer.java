package tech.softwareologists.ij.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import tech.softwareologists.core.ManifestGenerator;
import tech.softwareologists.core.QueryService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Minimal HTTP server exposing MCP endpoints.
 */
public class HttpMcpServer {
    private final HttpServer server;
    private final QueryService queryService;

    public HttpMcpServer(int port, QueryService queryService) throws IOException {
        this.queryService = queryService;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/mcp/manifest", new ManifestHandler());
        server.createContext("/mcp/query", new QueryHandler());
    }

    /** Starts the server. */
    public void start() {
        server.start();
    }

    /** Stops the server. */
    public void stop() {
        server.stop(0);
    }

    /** Returns the bound port. */
    public int getPort() {
        return server.getAddress().getPort();
    }

    private class ManifestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] bytes = ManifestGenerator.generate().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class QueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            String response = "[]";

            if (!body.isEmpty()) {
                JSONObject req = new JSONObject(body);

                if (req.has("findCallers")) {
                    String cls = req.getString("findCallers");
                    response = new JSONArray(queryService.findCallers(cls)).toString();
                } else if (req.has("findImplementations")) {
                    String iface = req.getString("findImplementations");
                    response = new JSONArray(queryService.findImplementations(iface)).toString();
                } else if (req.has("findSubclasses")) {
                    Object val = req.get("findSubclasses");
                    String cls;
                    int depth = 1;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        cls = o.getString("className");
                        depth = o.optInt("depth", 1);
                    } else {
                        cls = val.toString();
                    }
                    response = new JSONArray(queryService.findSubclasses(cls, depth)).toString();
                } else if (req.has("findDependencies")) {
                    Object val = req.get("findDependencies");
                    String cls;
                    Integer depth = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        cls = o.getString("className");
                        depth = o.has("depth") ? o.getInt("depth") : null;
                    } else {
                        cls = val.toString();
                    }
                    response = new JSONArray(queryService.findDependencies(cls, depth)).toString();
                } else if (req.has("findPathBetweenClasses")) {
                    JSONObject o = req.getJSONObject("findPathBetweenClasses");
                    String from = o.getString("fromClass");
                    String to = o.getString("toClass");
                    Integer max = o.has("maxDepth") ? o.getInt("maxDepth") : null;
                    response = new JSONArray(queryService.findPathBetweenClasses(from, to, max)).toString();
                } else if (req.has("findMethodsCallingMethod")) {
                    JSONObject o = req.getJSONObject("findMethodsCallingMethod");
                    String cls = o.getString("className");
                    String sig = o.getString("methodSignature");
                    Integer lim = o.has("limit") ? o.getInt("limit") : null;
                    response = new JSONArray(queryService.findMethodsCallingMethod(cls, sig, lim)).toString();
                } else if (req.has("findBeansWithAnnotation")) {
                    String ann = req.getString("findBeansWithAnnotation");
                    response = new JSONArray(queryService.findBeansWithAnnotation(ann)).toString();
                } else if (req.has("searchByAnnotation")) {
                    JSONObject o = req.getJSONObject("searchByAnnotation");
                    String ann = o.getString("annotation");
                    String target = o.optString("targetType", "class");
                    response = new JSONArray(queryService.searchByAnnotation(ann, target)).toString();
                } else if (req.has("findHttpEndpoints")) {
                    JSONObject o = req.getJSONObject("findHttpEndpoints");
                    String base = o.getString("basePath");
                    String verb = o.getString("httpMethod");
                    response = new JSONArray(queryService.findHttpEndpoints(base, verb)).toString();
                } else if (req.has("findControllersUsingService")) {
                    String svc = req.getString("findControllersUsingService");
                    response = new JSONArray(queryService.findControllersUsingService(svc)).toString();
                } else if (req.has("findEventListeners")) {
                    String ev = req.getString("findEventListeners");
                    response = new JSONArray(queryService.findEventListeners(ev)).toString();
                } else if (req.has("findScheduledTasks")) {
                    response = new JSONArray(queryService.findScheduledTasks()).toString();
                } else if (req.has("findConfigPropertyUsage")) {
                    String key = req.getString("findConfigPropertyUsage");
                    response = new JSONArray(queryService.findConfigPropertyUsage(key)).toString();
                } else if (req.has("getPackageHierarchy")) {
                    Object val = req.get("getPackageHierarchy");
                    String pkg;
                    Integer depth = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        pkg = o.getString("rootPackage");
                        depth = o.has("depth") ? o.getInt("depth") : null;
                    } else {
                        pkg = val.toString();
                    }
                    response = queryService.getPackageHierarchy(pkg, depth);
                } else if (req.has("getGraphStatistics")) {
                    Integer top = req.optInt("getGraphStatistics", -1);
                    response = queryService.getGraphStatistics(top == -1 ? null : top);
                } else if (req.has("exportGraph")) {
                    JSONObject o = req.getJSONObject("exportGraph");
                    String format = o.getString("format");
                    String path = o.getString("outputPath");
                    queryService.exportGraph(format, path);
                    response = "{}";
                }
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
