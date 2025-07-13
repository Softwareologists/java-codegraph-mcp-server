package tech.softwareologists.core;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import tech.softwareologists.core.db.NodeLabel;
import tech.softwareologists.core.db.EdgeType;
import tech.softwareologists.core.QueryResult;

import java.util.List;

/**
 * Default implementation of {@link QueryService} backed by a Neo4j driver.
 */
public class QueryServiceImpl implements QueryService {
    private final Driver driver;

    public QueryServiceImpl(Driver driver) {
        this.driver = driver;
    }

    private QueryResult<String> wrap(List<String> items, Integer limit, Integer page, Integer pageSize) {
        int total = items.size();
        int p = page == null ? 1 : page;
        int ps = pageSize == null ? (limit != null ? limit : total) : pageSize;
        int from = Math.max(0, (p - 1) * ps);
        int to = Math.min(from + ps, total);
        List<String> slice = from >= total ? java.util.Collections.emptyList() : new java.util.ArrayList<>(items.subList(from, to));
        if (limit != null && slice.size() > limit) {
            slice = slice.subList(0, limit);
        }
        return new QueryResult<>(slice, p, ps, total);
    }

    private QueryResult<String> runPagedQuery(String listQuery, String countQuery, java.util.Map<String, Object> params,
                                              Integer limit, Integer page, Integer pageSize, String field) {
        try (Session session = driver.session()) {
            int total = session.run(countQuery, params).single().get("total").asInt();

            int p = page == null ? 1 : page;
            int ps = pageSize == null ? (limit != null ? limit : total) : pageSize;
            int skip = Math.max(0, (p - 1) * ps);
            int fetch = ps;
            if (limit != null) {
                if (skip >= limit) {
                    return new QueryResult<>(java.util.Collections.emptyList(), p, ps, total);
                }
                fetch = Math.min(fetch, limit - skip);
            }

            java.util.Map<String, Object> q = new java.util.HashMap<>(params);
            q.put("skip", skip);
            q.put("lim", fetch);

            List<String> items = session.run(listQuery + " ORDER BY " + field + " SKIP $skip LIMIT $lim", q)
                    .list(r -> r.get(field).asString());

            return new QueryResult<>(items, p, ps, total);
        }
    }

    @Override
    public QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize) {
        String list = "MATCH (c:" + NodeLabel.CLASS + ")-[:" + EdgeType.DEPENDS_ON + "]->(t:" + NodeLabel.CLASS + " {name:$name}) RETURN c.name AS name";
        String count = "MATCH (c:" + NodeLabel.CLASS + ")-[:" + EdgeType.DEPENDS_ON + "]->(t:" + NodeLabel.CLASS + " {name:$name}) RETURN count(c) AS total";
        java.util.Map<String, Object> params = java.util.Collections.singletonMap("name", className);
        return runPagedQuery(list, count, params, limit, page, pageSize, "name");
    }

    @Override
    public QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize) {
        String list = "MATCH (c:" + NodeLabel.CLASS + ")-[:" + EdgeType.IMPLEMENTS + "]->(i:" + NodeLabel.CLASS + " {name:$name}) RETURN c.name AS name";
        String count = "MATCH (c:" + NodeLabel.CLASS + ")-[:" + EdgeType.IMPLEMENTS + "]->(i:" + NodeLabel.CLASS + " {name:$name}) RETURN count(c) AS total";
        java.util.Map<String, Object> params = java.util.Collections.singletonMap("name", interfaceName);
        return runPagedQuery(list, count, params, limit, page, pageSize, "name");
    }

    @Override
    public QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize) {
        String base = "MATCH (sub:" + NodeLabel.CLASS + ")-[:" + EdgeType.EXTENDS + "*1.." + depth + "]->(sup:" + NodeLabel.CLASS + " {name:$name}) RETURN DISTINCT sub.name AS name";
        String count = "MATCH (sub:" + NodeLabel.CLASS + ")-[:" + EdgeType.EXTENDS + "*1.." + depth + "]->(sup:" + NodeLabel.CLASS + " {name:$name}) RETURN count(DISTINCT sub) AS total";
        java.util.Map<String, Object> params = java.util.Collections.singletonMap("name", className);
        return runPagedQuery(base, count, params, limit, page, pageSize, "name");
    }

    @Override
    public QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize) {
        String pattern = depth == null ? "*" : "*1.." + depth;
        String list =
                "MATCH (c:" + NodeLabel.CLASS + " {name:$name})-[:" + EdgeType.DEPENDS_ON + pattern + "]->(dep:" + NodeLabel.CLASS + ") RETURN DISTINCT dep.name AS name";
        String count =
                "MATCH (c:" + NodeLabel.CLASS + " {name:$name})-[:" + EdgeType.DEPENDS_ON + pattern + "]->(dep:" + NodeLabel.CLASS + ") RETURN count(DISTINCT dep) AS total";
        java.util.Map<String, Object> params = java.util.Collections.singletonMap("name", className);
        return runPagedQuery(list, count, params, limit, page, pageSize, "name");
    }

    @Override
    public QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
        try (Session session = driver.session()) {
            String query = "MATCH p=shortestPath((s:" + NodeLabel.CLASS + " {name:$from})-[:" + EdgeType.DEPENDS_ON + "*]->(t:" + NodeLabel.CLASS + " {name:$to})) " +
                    "RETURN [n IN nodes(p) | n.name] AS path";
            List<List<String>> paths = session.run(query,
                            Values.parameters("from", fromClass, "to", toClass))
                    .list(r -> r.get("path").asList(v -> v.asString()));
            if (paths.isEmpty()) {
                return wrap(java.util.Collections.emptyList(), null, null, null);
            }
            List<String> result = paths.get(0);
            if (maxDepth != null && result.size() - 1 > maxDepth) {
                return wrap(java.util.Collections.emptyList(), null, null, null);
            }
            return wrap(result, null, null, null);
        }
    }

    @Override
    public QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize) {
        String base =
                "MATCH (caller:" + NodeLabel.METHOD + ")-[:CALLS]->(target:" + NodeLabel.METHOD + " {class:$class, signature:$sig}) " +
                        "RETURN caller.signature AS sig";
        String count =
                "MATCH (caller:" + NodeLabel.METHOD + ")-[:CALLS]->(target:" + NodeLabel.METHOD + " {class:$class, signature:$sig}) RETURN count(caller) AS total";
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("class", className);
        params.put("sig", methodSignature);
        return runPagedQuery(base, count, params, limit, page, pageSize, "sig");
    }

    @Override
    public QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize) {
        return searchByAnnotation(annotation, "class", limit, page, pageSize);
    }

    @Override
    public QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize) {
        boolean method = "method".equalsIgnoreCase(targetType);
        String label = method ? NodeLabel.METHOD.toString() : NodeLabel.CLASS.toString();
        String returnProp = method ? "signature" : "name";
        String base =
                "MATCH (n:" + label + ") WHERE ANY(a IN n.annotations WHERE a = $ann) RETURN n." + returnProp + " AS name";
        String count =
                "MATCH (n:" + label + ") WHERE ANY(a IN n.annotations WHERE a = $ann) RETURN count(n) AS total";
        java.util.Map<String, Object> params = java.util.Collections.singletonMap("ann", annotation);
        return runPagedQuery(base, count, params, limit, page, pageSize, "name");
    }

    @Override
    public QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize) {
        String base =
                "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.httpRoute STARTS WITH $base " +
                        "AND m.httpMethod = $verb RETURN m.class + '|' + m.signature AS ep";
        String count =
                "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.httpRoute STARTS WITH $base AND m.httpMethod = $verb RETURN count(m) AS total";
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("base", basePath);
        params.put("verb", httpMethod);
        return runPagedQuery(base, count, params, limit, page, pageSize, "ep");
    }

    @Override
    public QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize) {
        String base =
                "MATCH (svc:" + NodeLabel.CLASS + " {name:$svc})<-[:" + EdgeType.USES + "]-(c:" + NodeLabel.CLASS + ") " +
                        "WHERE ANY(a IN c.annotations WHERE a IN ['org.springframework.stereotype.Controller','org.springframework.web.bind.annotation.RestController']) " +
                        "RETURN c.name AS name";
        String count =
                "MATCH (svc:" + NodeLabel.CLASS + " {name:$svc})<-[:" + EdgeType.USES + "]-(c:" + NodeLabel.CLASS + ") " +
                        "WHERE ANY(a IN c.annotations WHERE a IN ['org.springframework.stereotype.Controller','org.springframework.web.bind.annotation.RestController']) RETURN count(c) AS total";
        java.util.Map<String, Object> params = java.util.Collections.singletonMap("svc", serviceClassName);
        return runPagedQuery(base, count, params, limit, page, pageSize, "name");
    }

    @Override
    public QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize) {
        String base =
                "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.eventType = $type RETURN m.class + '|' + m.signature AS m";
        String count =
                "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.eventType = $type RETURN count(m) AS total";
        java.util.Map<String, Object> params = java.util.Collections.singletonMap("type", eventType);
        return runPagedQuery(base, count, params, limit, page, pageSize, "m");
    }

    @Override
    public QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize) {
        String base =
                "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.cron IS NOT NULL RETURN m.class + '|' + m.signature + '|' + m.cron AS m";
        String count = "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.cron IS NOT NULL RETURN count(m) AS total";
        java.util.Map<String, Object> params = java.util.Collections.emptyMap();
        return runPagedQuery(base, count, params, limit, page, pageSize, "m");
    }

    @Override
    public QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize) {
        String base =
                "MATCH (c:" + NodeLabel.CLASS + ") WHERE $key IN c.configProperties RETURN c.name AS loc " +
                        "UNION MATCH (m:" + NodeLabel.METHOD + ") WHERE $key IN m.configProperties RETURN m.class + '|' + m.signature AS loc";
        String count =
                "MATCH (c:" + NodeLabel.CLASS + ") WHERE $key IN c.configProperties RETURN count(c) AS ccount UNION ALL MATCH (m:" + NodeLabel.METHOD + ") WHERE $key IN m.configProperties RETURN count(m) AS mcount";
        // run count manually using session to sum two counts
        try (Session session = driver.session()) {
            var res = session.run("MATCH (c:" + NodeLabel.CLASS + ") WHERE $key IN c.configProperties RETURN count(c) AS c", java.util.Collections.singletonMap("key", propertyKey)).single();
            int countClass = res.get("c").asInt();
            var res2 = session.run("MATCH (m:" + NodeLabel.METHOD + ") WHERE $key IN m.configProperties RETURN count(m) AS m", java.util.Collections.singletonMap("key", propertyKey)).single();
            int total = countClass + res2.get("m").asInt();

            int p = page == null ? 1 : page;
            int ps = pageSize == null ? (limit != null ? limit : total) : pageSize;
            int skip = Math.max(0, (p - 1) * ps);
            int fetch = ps;
            if (limit != null) {
                if (skip >= limit) {
                    return new QueryResult<>(java.util.Collections.emptyList(), p, ps, total);
                }
                fetch = Math.min(fetch, limit - skip);
            }
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("key", propertyKey);
            params.put("skip", skip);
            params.put("lim", fetch);
            List<String> items = session.run(base + " ORDER BY loc SKIP $skip LIMIT $lim", params)
                    .list(r -> r.get("loc").asString());
            return new QueryResult<>(items, p, ps, total);
        }
    }

    @Override
    public String getPackageHierarchy(String rootPackage, Integer depth) {
        try (Session session = driver.session()) {
            var results = session.run(
                    "MATCH (p:" + NodeLabel.PACKAGE + ")-[:CONTAINS]->(c:" + NodeLabel.CLASS + ") " +
                            "WHERE p.name STARTS WITH $root RETURN p.name AS pkg, c.name AS cls",
                    Values.parameters("root", rootPackage));

            class PackageNode {
                String name;
                java.util.Map<String, PackageNode> children = new java.util.TreeMap<>();
                java.util.List<String> classes = new java.util.ArrayList<>();

                PackageNode(String name) { this.name = name; }
            }

            String[] rootParts = rootPackage.isEmpty() ? new String[0] : rootPackage.split("\\.");
            PackageNode root = new PackageNode(rootPackage);

            java.util.function.Function<String, Integer> depthCalc = pkg -> {
                if (pkg.isEmpty()) return 0;
                int diff = pkg.split("\\.").length - rootParts.length;
                return diff;
            };

            while (results.hasNext()) {
                var rec = results.next();
                String pkg = rec.get("pkg").asString();
                String cls = rec.get("cls").asString();
                int d = depthCalc.apply(pkg);
                if (depth != null && d > depth) continue;
                PackageNode node = root;
                if (!pkg.equals(rootPackage)) {
                    String rel = pkg.substring(rootPackage.isEmpty() ? 0 : rootPackage.length() + 1);
                    String[] parts = rel.split("\\.");
                    String current = rootPackage;
                    for (int i = 0; i < parts.length && (depth == null || i + 1 <= depth); i++) {
                        current = current.isEmpty() ? parts[i] : current + "." + parts[i];
                        node = node.children.computeIfAbsent(current, PackageNode::new);
                    }
                }
                node.classes.add(cls);
            }

            java.util.function.Function<PackageNode, String> toJson = new java.util.function.Function<PackageNode, String>() {
                @Override
                public String apply(PackageNode n) {
                    StringBuilder sb = new StringBuilder();
                    sb.append('{').append("\"name\":\"").append(n.name).append("\"");
                    if (!n.classes.isEmpty()) {
                        java.util.Collections.sort(n.classes);
                        sb.append(",\"classes\":[");
                        for (int i = 0; i < n.classes.size(); i++) {
                            if (i > 0) sb.append(',');
                            sb.append('"').append(n.classes.get(i)).append('"');
                        }
                        sb.append(']');
                    }
                    if (!n.children.isEmpty()) {
                        sb.append(",\"packages\":[");
                        boolean first = true;
                        for (PackageNode child : n.children.values()) {
                            if (!first) sb.append(',');
                            first = false;
                            sb.append(apply(child));
                        }
                        sb.append(']');
                    }
                    sb.append('}');
                    return sb.toString();
                }
            };

            return toJson.apply(root);
        }
    }

    @Override
    public String getGraphStatistics(Integer topN) {
        int limit = topN == null ? 10 : topN;
        try (Session session = driver.session()) {
            long nodeCount = session.run("MATCH (n) RETURN count(n) AS c")
                    .single()
                    .get("c").asLong();
            long edgeCount = session.run("MATCH ()-[r]->() RETURN count(r) AS c")
                    .single()
                    .get("c").asLong();

            var top = session.run(
                            "MATCH (c:" + NodeLabel.CLASS + ") " +
                                    "RETURN c.name AS name, count{ (c)--() } AS d " +
                                    "ORDER BY d DESC, name LIMIT $limit",
                            Values.parameters("limit", limit))
                    .list(r -> new String[]{r.get("name").asString(), String.valueOf(r.get("d").asInt())});

            StringBuilder sb = new StringBuilder();
            sb.append('{');
            sb.append("\"nodes\":").append(nodeCount).append(',');
            sb.append("\"edges\":").append(edgeCount).append(',');
            sb.append("\"topClasses\":[");
            for (int i = 0; i < top.size(); i++) {
                String[] t = top.get(i);
                if (i > 0) sb.append(',');
                sb.append('{')
                        .append("\"name\":\"").append(t[0]).append("\",")
                        .append("\"degree\":").append(t[1])
                        .append('}');
            }
            sb.append(']');
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public void exportGraph(String format, String outputPath) {
        String upper = format == null ? "" : format.toUpperCase();
        try (Session session = driver.session()) {
            var nodeRecords = session.run(
                    "MATCH (n) RETURN id(n) AS id, labels(n)[0] AS label, n.name AS name, n.class AS cls, n.signature AS sig")
                    .list(r -> new Object[]{r.get("id").asLong(), r.get("label").asString(),
                            r.get("name").isNull() ? null : r.get("name").asString(),
                            r.get("cls").isNull() ? null : r.get("cls").asString(),
                            r.get("sig").isNull() ? null : r.get("sig").asString()});

            java.util.Map<Long, String> names = new java.util.HashMap<>();
            for (Object[] arr : nodeRecords) {
                long id = (Long) arr[0];
                String label = (String) arr[1];
                String name;
                if (NodeLabel.METHOD.toString().equals(label)) {
                    String cls = (String) arr[3];
                    String sig = (String) arr[4];
                    name = cls + '|' + sig;
                } else {
                    name = (String) arr[2];
                }
                names.put(id, name);
            }

            var edgeRecords = session.run(
                    "MATCH (a)-[r]->(b) RETURN id(a) AS from, type(r) AS type, id(b) AS to")
                    .list(r -> new Object[]{r.get("from").asLong(), r.get("type").asString(), r.get("to").asLong()});

            StringBuilder sb = new StringBuilder();
            switch (upper) {
                case "DOT":
                    sb.append("digraph G {");
                    for (var e : names.entrySet()) {
                        sb.append('\n').append("  n").append(e.getKey())
                                .append(" [label=\"").append(e.getValue()).append("\"];");
                    }
                    for (Object[] edge : edgeRecords) {
                        sb.append('\n').append("  n").append(edge[0]).append(" -> n")
                                .append(edge[2]).append(" [label=\"")
                                .append(edge[1]).append("\"];");
                    }
                    sb.append('\n').append('}');
                    break;
                case "CSV":
                    sb.append("from,to,type\n");
                    for (Object[] edge : edgeRecords) {
                        sb.append(names.get((Long) edge[0])).append(',')
                                .append(names.get((Long) edge[2])).append(',')
                                .append(edge[1]).append('\n');
                    }
                    break;
                case "JSON":
                    sb.append('{');
                    sb.append("\"nodes\":[");
                    boolean first = true;
                    for (var e : names.entrySet()) {
                        if (!first) sb.append(',');
                        first = false;
                        sb.append('{')
                                .append("\"id\":").append(e.getKey()).append(',')
                                .append("\"name\":\"").append(e.getValue()).append("\"}");
                    }
                    sb.append("],\"edges\":[");
                    first = true;
                    for (Object[] edge : edgeRecords) {
                        if (!first) sb.append(',');
                        first = false;
                        sb.append('{')
                                .append("\"from\":").append(edge[0]).append(',')
                                .append("\"to\":").append(edge[2]).append(',')
                                .append("\"type\":\"").append(edge[1]).append("\"}");
                    }
                    sb.append(']');
                    sb.append('}');
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }

            java.nio.file.Files.write(java.nio.file.Paths.get(outputPath),
                    sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to export graph", e);
        }
    }
}
