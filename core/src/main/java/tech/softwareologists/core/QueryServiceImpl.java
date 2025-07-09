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
}
