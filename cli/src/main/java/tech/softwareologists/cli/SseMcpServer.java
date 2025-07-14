package tech.softwareologists.cli;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import tech.softwareologists.core.ManifestGenerator;
import tech.softwareologists.core.QueryService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP SSE server exposing MCP endpoints.
 */
public class SseMcpServer {
    private final HttpServer server;
    private final QueryService queryService;

    public SseMcpServer(int port, QueryService queryService) throws IOException {
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

    private static byte[] formatEvent(String data) {
        String event = "data: " + data.replace("\n", "") + "\n\n";
        return event.getBytes(StandardCharsets.UTF_8);
    }

    private class ManifestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] bytes = formatEvent(ManifestGenerator.generate());
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
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
                    Object val = req.get("findCallers");
                    String cls;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        cls = o.getString("className");
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        cls = val.toString();
                    }
                    response = new JSONArray(queryService.findCallers(cls, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findImplementations")) {
                    Object val = req.get("findImplementations");
                    String iface;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        iface = o.getString("interfaceName");
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        iface = val.toString();
                    }
                    response = new JSONArray(queryService.findImplementations(iface, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findSubclasses")) {
                    Object val = req.get("findSubclasses");
                    String cls;
                    int depth = 1;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        cls = o.getString("className");
                        depth = o.optInt("depth", 1);
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        cls = val.toString();
                    }
                    response = new JSONArray(queryService.findSubclasses(cls, depth, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findDependencies")) {
                    Object val = req.get("findDependencies");
                    String cls;
                    Integer depth = null;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        cls = o.getString("className");
                        depth = o.has("depth") ? o.getInt("depth") : null;
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        cls = val.toString();
                    }
                    response = new JSONArray(queryService.findDependencies(cls, depth, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findPathBetweenClasses")) {
                    JSONObject o = req.getJSONObject("findPathBetweenClasses");
                    String from = o.getString("fromClass");
                    String to = o.getString("toClass");
                    Integer max = o.has("maxDepth") ? o.getInt("maxDepth") : null;
                    response = new JSONArray(queryService.findPathBetweenClasses(from, to, max).getItems()).toString();
                } else if (req.has("findMethodsCallingMethod")) {
                    JSONObject o = req.getJSONObject("findMethodsCallingMethod");
                    String cls = o.getString("className");
                    String sig = o.getString("methodSignature");
                    Integer lim = o.has("limit") ? o.getInt("limit") : null;
                    Integer page = o.has("page") ? o.getInt("page") : null;
                    Integer pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    response = new JSONArray(queryService.findMethodsCallingMethod(cls, sig, lim, page, pageSize).getItems()).toString();
                } else if (req.has("findBeansWithAnnotation")) {
                    Object val = req.get("findBeansWithAnnotation");
                    String ann;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        ann = o.getString("annotation");
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        ann = val.toString();
                    }
                    response = new JSONArray(queryService.findBeansWithAnnotation(ann, limit, page, pageSize).getItems()).toString();
                } else if (req.has("searchByAnnotation")) {
                    JSONObject o = req.getJSONObject("searchByAnnotation");
                    String ann = o.getString("annotation");
                    String target = o.optString("targetType", "class");
                    Integer limit = o.has("limit") ? o.getInt("limit") : null;
                    Integer page = o.has("page") ? o.getInt("page") : null;
                    Integer pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    response = new JSONArray(queryService.searchByAnnotation(ann, target, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findHttpEndpoints")) {
                    JSONObject o = req.getJSONObject("findHttpEndpoints");
                    String base = o.getString("basePath");
                    String verb = o.getString("httpMethod");
                    Integer limit = o.has("limit") ? o.getInt("limit") : null;
                    Integer page = o.has("page") ? o.getInt("page") : null;
                    Integer pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    response = new JSONArray(queryService.findHttpEndpoints(base, verb, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findControllersUsingService")) {
                    Object val = req.get("findControllersUsingService");
                    String svc;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        svc = o.getString("serviceClassName");
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        svc = val.toString();
                    }
                    response = new JSONArray(queryService.findControllersUsingService(svc, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findEventListeners")) {
                    Object val = req.get("findEventListeners");
                    String ev;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        ev = o.getString("eventType");
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        ev = val.toString();
                    }
                    response = new JSONArray(queryService.findEventListeners(ev, limit, page, pageSize).getItems()).toString();
                } else if (req.has("findScheduledTasks")) {
                    JSONObject o = req.optJSONObject("findScheduledTasks");
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (o != null) {
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    }
                    response = new JSONArray(queryService.findScheduledTasks(limit, page, pageSize).getItems()).toString();
                } else if (req.has("findConfigPropertyUsage")) {
                    Object val = req.get("findConfigPropertyUsage");
                    String key;
                    Integer limit = null;
                    Integer page = null;
                    Integer pageSize = null;
                    if (val instanceof JSONObject) {
                        JSONObject o = (JSONObject) val;
                        key = o.getString("propertyKey");
                        limit = o.has("limit") ? o.getInt("limit") : null;
                        page = o.has("page") ? o.getInt("page") : null;
                        pageSize = o.has("pageSize") ? o.getInt("pageSize") : null;
                    } else {
                        key = val.toString();
                    }
                    response = new JSONArray(queryService.findConfigPropertyUsage(key, limit, page, pageSize).getItems()).toString();
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

            byte[] bytes = formatEvent(response);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
