package tech.softwareologists.ij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import tech.softwareologists.core.db.EmbeddedNeo4j;

/**
 * Project-level startup activity that logs when a project is opened.
 */
public class StartupActivity implements com.intellij.openapi.startup.StartupActivity, DumbAware {

    private static final Logger LOG = Logger.getInstance(StartupActivity.class);

    @Override
    public void runActivity(Project project) {
        LOG.info("CodeGraph MCP plugin initialized for project: " + project.getName());

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            PsiClassImportService service = new PsiClassImportService(db.getDriver());
            int count = service.importProjectClasses(project);
            LOG.info("Imported " + count + " classes from project");
        } catch (Exception e) {
            LOG.warn("Failed to import project classes", e);
        }
    }
}
