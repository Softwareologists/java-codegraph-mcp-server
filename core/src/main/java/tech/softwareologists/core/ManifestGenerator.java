package tech.softwareologists.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import tech.softwareologists.core.QueryDefaults;

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
        sb.append("  \"version\": \"1.0.1\",\n");
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
            sb.append("]");

            // insert defaults for common query options
            String name = m.getName();
            switch (name) {
                case "findCallers":
                case "findImplementations":
                case "findSubclasses":
                case "findDependencies":
                case "findMethodsCallingMethod":
                case "findBeansWithAnnotation":
                case "searchByAnnotation":
                case "findHttpEndpoints":
                case "findControllersUsingService":
                case "findEventListeners":
                case "findScheduledTasks":
                case "findConfigPropertyUsage":
                    sb.append(",\"defaults\":{\"limit\":")
                            .append(QueryDefaults.DEFAULT_LIMIT)
                            .append(",\"page\":")
                            .append(QueryDefaults.DEFAULT_PAGE)
                            .append(",\"pageSize\":")
                            .append(QueryDefaults.DEFAULT_PAGE_SIZE);
                    if ("searchByAnnotation".equals(name)) {
                        sb.append(",\"targetType\":\"class\"");
                    }
                    sb.append('}');
                    break;
                case "getGraphStatistics":
                    sb.append(",\"defaults\":{\"topN\":")
                            .append(QueryDefaults.DEFAULT_TOP_N)
                            .append('}');
                    break;
                default:
                    // no defaults
            }

            sb.append('}');
            if (i < methods.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
