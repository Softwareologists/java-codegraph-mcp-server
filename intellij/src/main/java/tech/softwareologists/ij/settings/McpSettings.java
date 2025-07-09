package tech.softwareologists.ij.settings;

import com.intellij.ide.util.PropertiesComponent;

/**
 * Accessor for persistent MCP plugin settings.
 */
public class McpSettings {
    private static final String PORT_KEY = "codegraph.mcp.port";
    private static final String FILTERS_KEY = "codegraph.mcp.packageFilters";

    private final PropertiesComponent props = PropertiesComponent.getInstance();

    /** Returns the configured HTTP port or 9090 by default. */
    public int getPort() {
        return props.getInt(PORT_KEY, 9090);
    }

    /** Sets the HTTP port. */
    public void setPort(int port) {
        props.setValue(PORT_KEY, String.valueOf(port));
    }

    /** Returns comma-separated package filters. */
    public String getPackageFilters() {
        return props.getValue(FILTERS_KEY, "");
    }

    /** Sets comma-separated package filters. */
    public void setPackageFilters(String filters) {
        props.setValue(FILTERS_KEY, filters == null ? "" : filters);
    }

    /** Singleton instance. */
    public static McpSettings getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final McpSettings INSTANCE = new McpSettings();
    }
}
