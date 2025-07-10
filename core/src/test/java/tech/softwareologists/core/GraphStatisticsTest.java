package tech.softwareologists.core;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

public class GraphStatisticsTest {
    @Test
    public void getGraphStatistics_returnsCountsAndTopClasses() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'A'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'B'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'C'})");
                session.run("MATCH (a:" + NodeLabel.CLASS + " {name:'A'}), (b:" + NodeLabel.CLASS + " {name:'B'}) CREATE (a)-[:DEPENDS_ON]->(b)");
                session.run("MATCH (b:" + NodeLabel.CLASS + " {name:'B'}), (c:" + NodeLabel.CLASS + " {name:'C'}) CREATE (b)-[:DEPENDS_ON]->(c)");
                session.run("MATCH (b:" + NodeLabel.CLASS + " {name:'B'}), (a:" + NodeLabel.CLASS + " {name:'A'}) CREATE (b)-[:DEPENDS_ON]->(a)");
            }

            QueryService svc = new QueryServiceImpl(driver);
            String stats = svc.getGraphStatistics(2);
            String expected = "{\"nodes\":3,\"edges\":3,\"topClasses\":[{\"name\":\"B\",\"degree\":3},{\"name\":\"A\",\"degree\":2}]}";
            if (!stats.equals(expected)) {
                throw new AssertionError("Unexpected stats: " + stats);
            }
        }
    }
}
