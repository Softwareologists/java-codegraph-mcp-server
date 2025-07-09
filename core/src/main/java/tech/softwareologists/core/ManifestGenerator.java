package tech.softwareologists.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Utility to generate the MCP manifest by reflecting on {@link QueryService}.
 */
public class ManifestGenerator {
    private ManifestGenerator() {
        // utility class
    }

    /**
     * Generate the manifest JSON string listing available query capabilities.
     *
     * @return JSON manifest
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"CodeGraph MCP\",\n");
        sb.append("  \"version\": \"0.1.0\",\n");
        sb.append("  \"capabilities\": [");

        Method[] methods = QueryService.class.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            sb.append("{\"name\":\"").append(m.getName()).append("\",\"params\":[");
            Parameter[] params = m.getParameters();
            for (int j = 0; j < params.length; j++) {
                if (j > 0) {
                    sb.append(',');
                }
                sb.append('"').append(params[j].getName()).append('"');
            }
            sb.append("]}");
            if (i < methods.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
