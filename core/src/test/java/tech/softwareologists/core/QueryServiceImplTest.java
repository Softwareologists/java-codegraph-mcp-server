package tech.softwareologists.core;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

import java.util.List;

public class QueryServiceImplTest {
    @Test
    public void findCallers_singleDependency_returnsCaller() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'dep.A'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'dep.B'})");
                session.run("MATCH (b:" + NodeLabel.CLASS + " {name:'dep.B'}), (a:" + NodeLabel.CLASS + " {name:'dep.A'}) CREATE (b)-[:DEPENDS_ON]->(a)");
            }

            QueryService service = new QueryServiceImpl(driver);
            List<String> callers = service.findCallers("dep.A");
            if (callers.size() != 1 || !callers.get(0).equals("dep.B")) {
                throw new AssertionError("Unexpected callers: " + callers);
            }
        }
    }
}
