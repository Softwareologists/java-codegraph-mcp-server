package tech.softwareologists.core;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

import java.nio.file.Files;
import java.nio.file.Path;

public class GraphExportTest {
    @Test
    public void exportGraph_smallGraph_writesFiles() throws Exception {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'A'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'B'})");
                session.run("MATCH (a:" + NodeLabel.CLASS + " {name:'A'}), (b:" + NodeLabel.CLASS + " {name:'B'}) CREATE (a)-[:DEPENDS_ON]->(b)");
            }

            QueryService svc = new QueryServiceImpl(driver);
            Path dir = Files.createTempDirectory("export");
            for (String fmt : new String[]{"DOT", "CSV", "JSON"}) {
                Path out = dir.resolve("graph." + fmt.toLowerCase());
                svc.exportGraph(fmt, out.toString());
                String content = Files.readString(out);
                if (!content.contains("A") || !content.contains("B")) {
                    throw new AssertionError("Missing nodes in " + fmt + " export: " + content);
                }
            }
        }
    }
}
