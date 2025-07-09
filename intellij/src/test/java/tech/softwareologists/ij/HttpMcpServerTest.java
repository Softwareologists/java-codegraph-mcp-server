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
        QueryService qs = cls -> Collections.singletonList("Caller");
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
        server.stop();
    }
}
