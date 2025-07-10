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
        this.neo4j = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .build();
        this.driver = GraphDatabase.driver(neo4j.boltURI(), AuthTokens.none());
        // ensure index on Class name
        try (Session session = driver.session()) {
            session.run("CREATE INDEX class_name IF NOT EXISTS FOR (c:" + NodeLabel.CLASS + ") ON (c.name)");
            session.run(
                    "CREATE INDEX method_identity IF NOT EXISTS FOR (m:" + NodeLabel.METHOD + ") ON (m.class, m.signature)");
            session.run(
                    "CREATE INDEX package_name IF NOT EXISTS FOR (p:" + NodeLabel.PACKAGE + ") ON (p.name)");
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
