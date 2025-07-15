package tech.softwareologists.ij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.Disposable;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.QueryService;
import tech.softwareologists.core.QueryServiceImpl;
import tech.softwareologists.ij.server.HttpMcpServer;
import tech.softwareologists.ij.settings.McpSettings;
import tech.softwareologists.ij.McpServerStatus;

/**
 * Project-level startup activity that logs when a project is opened.
 */
public class StartupActivity implements com.intellij.openapi.startup.StartupActivity, DumbAware {

    private static final Logger LOG = Logger.getInstance(StartupActivity.class);

    @Override
    public void runActivity(Project project) {
        LOG.info("CodeGraph MCP plugin initialized for project: " + project.getName());

        try {
            EmbeddedNeo4j db = new EmbeddedNeo4j();
            McpSettings settings = McpSettings.getInstance();
            PsiClassImportService service = new PsiClassImportService(db.getDriver(), settings.getPackageFilters());
            int count = service.importProjectClasses(project);
            LOG.info("Imported " + count + " classes from project");
            PsiManager.getInstance(project).addPsiTreeChangeListener(
                    new PsiClassChangeListener(project, service),
                    (Disposable) project
            );

            QueryService queryService = new QueryServiceImpl(db.getDriver());
            HttpMcpServer server = new HttpMcpServer(settings.getPort(), queryService);
            server.start();
            McpServerStatus.setStatus("Running on port " + server.getPort());
            LOG.info("MCP HTTP server started on port " + server.getPort());
        } catch (Exception e) {
            McpServerStatus.setStatus("Failed to start server: " + e.getMessage());
            LOG.warn("Failed to import project classes", e);
        }
    }
}
