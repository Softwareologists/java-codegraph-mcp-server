package tech.softwareologists.ij;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.junit.Test;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

import java.util.List;

public class PsiClassImportServiceTest extends LightJavaCodeInsightFixtureTestCase {

    @Test
    public void importProjectClasses_detectsClass() {
        myFixture.addClass("package foo; public class Bar {}");
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            PsiClassImportService service = new PsiClassImportService(driver);
            int count = service.importProjectClasses(getProject());
            if (count != 1) {
                throw new AssertionError("Expected 1 class but was: " + count);
            }
            try (Session session = driver.session()) {
                List<Record> res = session.run("MATCH (c:" + NodeLabel.CLASS + " {name:'foo.Bar'}) RETURN c").list();
                if (res.isEmpty()) {
                    throw new AssertionError("Node foo.Bar not persisted");
                }
            }
        }
    }
}
