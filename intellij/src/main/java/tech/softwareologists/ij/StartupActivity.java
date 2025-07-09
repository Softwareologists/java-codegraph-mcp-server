package tech.softwareologists.ij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;

/**
 * Project-level startup activity that logs when a project is opened.
 */
public class StartupActivity implements com.intellij.openapi.startup.StartupActivity, DumbAware {

    private static final Logger LOG = Logger.getInstance(StartupActivity.class);

    @Override
    public void runActivity(Project project) {
        LOG.info("CodeGraph MCP plugin initialized for project: " + project.getName());
    }
}
