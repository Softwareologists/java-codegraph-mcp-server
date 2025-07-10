package tech.softwareologists.core;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import tech.softwareologists.core.db.EmbeddedNeo4j;

import java.util.List;

public class MethodIndexTest {
    @Test
    public void startup_createsMethodIndex() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                List<org.neo4j.driver.Record> result = session.run(
                        "SHOW INDEXES YIELD name WHERE name = 'method_identity' RETURN name")
                        .list();
                if (result.isEmpty()) {
                    throw new AssertionError("method_identity index not found");
                }
            }
        }
    }
}
