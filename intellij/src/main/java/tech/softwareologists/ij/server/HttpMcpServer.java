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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal HTTP server exposing MCP endpoints.
 */
public class HttpMcpServer {
    private static final Pattern FIND_CALLERS =
            Pattern.compile("\\\"findCallers\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

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
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String response;
            Matcher m = FIND_CALLERS.matcher(body);
            if (m.find()) {
                String cls = m.group(1);
                List<String> callers = queryService.findCallers(cls);
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (int i = 0; i < callers.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append('"').append(callers.get(i)).append('"');
                }
                sb.append(']');
                response = sb.toString();
            } else {
                response = "[]";
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
