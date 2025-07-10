package tech.softwareologists.core;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import tech.softwareologists.core.db.NodeLabel;

import java.util.List;

/**
 * Default implementation of {@link QueryService} backed by a Neo4j driver.
 */
public class QueryServiceImpl implements QueryService {
    private final Driver driver;

    public QueryServiceImpl(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<String> findCallers(String className) {
        try (Session session = driver.session()) {
            return session.run(
                    "MATCH (c:" + NodeLabel.CLASS + ")-[:DEPENDS_ON]->(t:" + NodeLabel.CLASS + " {name:$name}) RETURN c.name AS name",
                    Values.parameters("name", className))
                    .list(r -> r.get("name").asString());
        }
    }

    @Override
    public List<String> findImplementations(String interfaceName) {
        try (Session session = driver.session()) {
            return session.run(
                            "MATCH (c:" + NodeLabel.CLASS + ")-[:IMPLEMENTS]->(i:" + NodeLabel.CLASS + " {name:$name}) RETURN c.name AS name",
                            Values.parameters("name", interfaceName))
                    .list(r -> r.get("name").asString());
        }
    }

    @Override
    public List<String> findSubclasses(String className, int depth) {
        try (Session session = driver.session()) {
            String query = "MATCH (sub:" + NodeLabel.CLASS + ")-[:EXTENDS*1.." + depth + "]->(sup:" + NodeLabel.CLASS + " {name:$name}) RETURN DISTINCT sub.name AS name";
            return session.run(query, Values.parameters("name", className))
                    .list(r -> r.get("name").asString());
        }
    }

    @Override
    public List<String> findDependencies(String className, Integer depth) {
        try (Session session = driver.session()) {
            String query =
                    "MATCH (c:" + NodeLabel.CLASS + " {name:$name})-[:DEPENDS_ON*]->(dep:" + NodeLabel.CLASS + ") " +
                            "RETURN DISTINCT dep.name AS name";
            if (depth != null) {
                query += " LIMIT " + depth;
            }
            return session.run(query, Values.parameters("name", className))
                    .list(r -> r.get("name").asString());
        }
    }

    @Override
    public List<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth) {
        try (Session session = driver.session()) {
            String query = "MATCH p=shortestPath((s:" + NodeLabel.CLASS + " {name:$from})-[:DEPENDS_ON*]->(t:" + NodeLabel.CLASS + " {name:$to})) " +
                    "RETURN [n IN nodes(p) | n.name] AS path";
            List<List<String>> paths = session.run(query,
                            Values.parameters("from", fromClass, "to", toClass))
                    .list(r -> r.get("path").asList(v -> v.asString()));
            if (paths.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            List<String> result = paths.get(0);
            if (maxDepth != null && result.size() - 1 > maxDepth) {
                return java.util.Collections.emptyList();
            }
            return result;
        }
    }

    @Override
    public List<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit) {
        try (Session session = driver.session()) {
            String query =
                    "MATCH (caller:" + NodeLabel.METHOD + ")-[:CALLS]->(target:" + NodeLabel.METHOD + " {class:$class, signature:$sig}) " +
                            "RETURN caller.signature AS sig";
            var params = Values.parameters("class", className, "sig", methodSignature);
            if (limit != null) {
                query += " LIMIT $limit";
                params = Values.parameters("class", className, "sig", methodSignature, "limit", limit);
            }
            return session.run(query, params)
                    .list(r -> r.get("sig").asString());
        }
    }

    @Override
    public List<String> findBeansWithAnnotation(String annotation) {
        return searchByAnnotation(annotation, "class");
    }

    @Override
    public List<String> searchByAnnotation(String annotation, String targetType) {
        try (Session session = driver.session()) {
            boolean method = "method".equalsIgnoreCase(targetType);
            String label = method ? NodeLabel.METHOD.toString() : NodeLabel.CLASS.toString();
            String returnProp = method ? "signature" : "name";
            String query =
                    "MATCH (n:" + label + ") WHERE ANY(a IN n.annotations WHERE a = $ann) " +
                            "RETURN n." + returnProp + " AS name";
            return session.run(query, Values.parameters("ann", annotation))
                    .list(r -> r.get("name").asString());
        }
    }

    @Override
    public List<String> findHttpEndpoints(String basePath, String httpMethod) {
        try (Session session = driver.session()) {
            String query =
                    "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.httpRoute STARTS WITH $base " +
                            "AND m.httpMethod = $verb RETURN m.class + '|' + m.signature AS ep";
            return session.run(query, Values.parameters("base", basePath, "verb", httpMethod))
                    .list(r -> r.get("ep").asString());
        }
    }

    @Override
    public List<String> findControllersUsingService(String serviceClassName) {
        try (Session session = driver.session()) {
            String query =
                    "MATCH (svc:" + NodeLabel.CLASS + " {name:$svc})<-[:USES]-(c:" + NodeLabel.CLASS + ") " +
                            "WHERE ANY(a IN c.annotations WHERE a IN ['org.springframework.stereotype.Controller','org.springframework.web.bind.annotation.RestController']) " +
                            "RETURN c.name AS name";
            return session.run(query, Values.parameters("svc", serviceClassName))
                    .list(r -> r.get("name").asString());
        }
    }

    @Override
    public List<String> findEventListeners(String eventType) {
        try (Session session = driver.session()) {
            String query =
                    "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.eventType = $type RETURN m.class + '|' + m.signature AS m";
            return session.run(query, Values.parameters("type", eventType))
                    .list(r -> r.get("m").asString());
        }
    }

    @Override
    public List<String> findScheduledTasks() {
        try (Session session = driver.session()) {
            String query =
                    "MATCH (m:" + NodeLabel.METHOD + ") WHERE m.cron IS NOT NULL RETURN m.class + '|' + m.signature + '|' + m.cron AS m";
            return session.run(query, Values.parameters())
                    .list(r -> r.get("m").asString());
        }
    }

    @Override
    public List<String> findConfigPropertyUsage(String propertyKey) {
        try (Session session = driver.session()) {
            String query =
                    "MATCH (c:" + NodeLabel.CLASS + ") WHERE $key IN c.configProperties RETURN c.name AS loc " +
                            "UNION MATCH (m:" + NodeLabel.METHOD + ") WHERE $key IN m.configProperties RETURN m.class + '|' + m.signature AS loc";
            return session.run(query, Values.parameters("key", propertyKey))
                    .list(r -> r.get("loc").asString());
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
}
