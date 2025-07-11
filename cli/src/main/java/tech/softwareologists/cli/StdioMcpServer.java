package tech.softwareologists.cli;

import tech.softwareologists.core.ManifestGenerator;
import tech.softwareologists.core.QueryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Very small MCP server implementation that communicates over STDIN/STDOUT.
 * <p>
 * The server prints the manifest JSON on startup and then echoes back any JSON
 * requests it receives. If a request contains the word {@code "manifest"}, the
 * manifest is returned again.
 */
public class StdioMcpServer implements Runnable {

    private final QueryService queryService;
    private final BufferedReader in;
    private final PrintStream out;

    /** Creates a server that reads from {@link System#in} and writes to {@link System#out}. */
    public StdioMcpServer(QueryService service) {
        this(service, System.in, System.out);
    }

    /**
     * Creates a server with the given input and output streams. This is mainly
     * used for unit testing.
     */
    StdioMcpServer(QueryService service, InputStream in, PrintStream out) {
        this.queryService = service;
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = out;
    }

    /**
     * Starts the server loop. This method blocks until the input stream is
     * closed.
     */
    @Override
    public void run() {
        String manifest = ManifestGenerator.generate();
        out.println(manifest);
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                JSONObject req = new JSONObject(trimmed);
                if (req.has("manifest")) {
                    out.println(manifest);
                    continue;
                }

                if (req.has("findCallers")) {
                    String cls = req.getString("findCallers");
                    printArray(queryService.findCallers(cls));
                    continue;
                }
                if (req.has("findImplementations")) {
                    String iface = req.getString("findImplementations");
                    printArray(queryService.findImplementations(iface));
                    continue;
                }
                if (req.has("findSubclasses")) {
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
                    printArray(queryService.findSubclasses(cls, depth));
                    continue;
                }
                if (req.has("findDependencies")) {
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
                    printArray(queryService.findDependencies(cls, depth));
                    continue;
                }
                if (req.has("findPathBetweenClasses")) {
                    JSONObject o = req.getJSONObject("findPathBetweenClasses");
                    String from = o.getString("fromClass");
                    String to = o.getString("toClass");
                    Integer max = o.has("maxDepth") ? o.getInt("maxDepth") : null;
                    printArray(queryService.findPathBetweenClasses(from, to, max));
                    continue;
                }
                if (req.has("findMethodsCallingMethod")) {
                    JSONObject o = req.getJSONObject("findMethodsCallingMethod");
                    String cls = o.getString("className");
                    String sig = o.getString("methodSignature");
                    Integer lim = o.has("limit") ? o.getInt("limit") : null;
                    printArray(queryService.findMethodsCallingMethod(cls, sig, lim));
                    continue;
                }
                if (req.has("findBeansWithAnnotation")) {
                    String ann = req.getString("findBeansWithAnnotation");
                    printArray(queryService.findBeansWithAnnotation(ann));
                    continue;
                }
                if (req.has("searchByAnnotation")) {
                    JSONObject o = req.getJSONObject("searchByAnnotation");
                    String ann = o.getString("annotation");
                    String target = o.optString("targetType", "class");
                    printArray(queryService.searchByAnnotation(ann, target));
                    continue;
                }
                if (req.has("findHttpEndpoints")) {
                    JSONObject o = req.getJSONObject("findHttpEndpoints");
                    String base = o.getString("basePath");
                    String verb = o.getString("httpMethod");
                    printArray(queryService.findHttpEndpoints(base, verb));
                    continue;
                }
                if (req.has("findControllersUsingService")) {
                    String svc = req.getString("findControllersUsingService");
                    printArray(queryService.findControllersUsingService(svc));
                    continue;
                }
                if (req.has("findEventListeners")) {
                    String ev = req.getString("findEventListeners");
                    printArray(queryService.findEventListeners(ev));
                    continue;
                }
                if (req.has("findScheduledTasks")) {
                    printArray(queryService.findScheduledTasks());
                    continue;
                }
                if (req.has("findConfigPropertyUsage")) {
                    String key = req.getString("findConfigPropertyUsage");
                    printArray(queryService.findConfigPropertyUsage(key));
                    continue;
                }
                if (req.has("getPackageHierarchy")) {
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
                    out.println(queryService.getPackageHierarchy(pkg, depth));
                    continue;
                }
                if (req.has("getGraphStatistics")) {
                    Integer top = req.optInt("getGraphStatistics", -1);
                    out.println(queryService.getGraphStatistics(top == -1 ? null : top));
                    continue;
                }
                if (req.has("exportGraph")) {
                    JSONObject o = req.getJSONObject("exportGraph");
                    String format = o.getString("format");
                    String path = o.getString("outputPath");
                    queryService.exportGraph(format, path);
                    out.println("{}");
                    continue;
                }

                out.println(trimmed);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printArray(List<String> list) {
        out.println(new JSONArray(list).toString());
    }
}
