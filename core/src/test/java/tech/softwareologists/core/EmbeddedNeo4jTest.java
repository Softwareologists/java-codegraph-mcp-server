package tech.softwareologists.core;

import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;
import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;

import java.util.List;

public class EmbeddedNeo4jTest {
    @Test
    public void createNode_queryByLabel_returnsNode() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (c:" + NodeLabel.CLASS + " {name:'Foo'})");
                List<Record> result = session.run("MATCH (c:" + NodeLabel.CLASS + ") RETURN c").list();
                if (result.isEmpty()) {
                    throw new AssertionError("Node not found");
                }
            }
        }
    }
}
