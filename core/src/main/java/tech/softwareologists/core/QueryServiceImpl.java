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
}
