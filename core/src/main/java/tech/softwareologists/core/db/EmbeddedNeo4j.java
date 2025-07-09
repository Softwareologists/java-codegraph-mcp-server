package tech.softwareologists.core.db;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import tech.softwareologists.core.db.NodeLabel;

/**
 * Helper class to manage an embedded Neo4j instance.
 */
public class EmbeddedNeo4j implements AutoCloseable {
    private final Neo4j neo4j;
    private final Driver driver;

    public EmbeddedNeo4j() {
        this.neo4j = Neo4jBuilders.newInProcessBuilder().build();
        this.driver = GraphDatabase.driver(neo4j.boltURI(), AuthTokens.none());
        // ensure index on Class name
        try (Session session = driver.session()) {
            session.run("CREATE INDEX class_name IF NOT EXISTS FOR (c:" + NodeLabel.CLASS + ") ON (c.name)");
        }
    }

    public Driver getDriver() {
        return driver;
    }

    @Override
    public void close() {
        driver.close();
        neo4j.close();
    }
}
