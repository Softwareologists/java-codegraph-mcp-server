package tech.softwareologists.ij;

/**
 * Holds the runtime status of the embedded MCP server.
 */
public class McpServerStatus {
    private static volatile String status = "Server not started";

    /** Returns the current server status message. */
    public static String getStatus() {
        return status;
    }

    /** Sets the current server status message. */
    public static void setStatus(String s) {
        status = s == null ? "" : s;
    }
}
