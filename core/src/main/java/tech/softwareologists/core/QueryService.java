package tech.softwareologists.core;

import java.util.List;

public interface QueryService {
    List<String> findCallers(String className);

    List<String> findImplementations(String interfaceName);

    List<String> findSubclasses(String className, int depth);

    /**
     * Find the classes that the given class depends on.
     *
     * @param className name of the source class
     * @param depth maximum number of dependency classes to return, or {@code null} for no limit
     * @return list of dependent class names
    */
    List<String> findDependencies(String className, Integer depth);

    /**
     * Find the shortest dependency path from one class to another.
     *
     * @param fromClass starting class name
     * @param toClass target class name
     * @param maxDepth maximum path length, or {@code null} for no limit
     * @return ordered list of class names from source to target, or empty if no path
     */
    List<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth);

    /**
     * Find methods that invoke a given target method.
     *
     * @param className fully qualified class name containing the target method
     * @param methodSignature JVM signature of the target method
     * @param limit maximum number of caller methods to return, or {@code null} for no limit
     * @return list of caller method signatures
     */
    List<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit);

    /**
     * Find classes annotated with the given annotation.
     *
     * @param annotation fully qualified annotation name
     * @return list of class names
     */
    List<String> findBeansWithAnnotation(String annotation);

    /**
     * Generic search for nodes annotated with the given annotation.
     *
     * @param annotation fully qualified annotation name
     * @param targetType "class" or "method" to indicate target node label
     * @return list of class names or method signatures
     */
    List<String> searchByAnnotation(String annotation, String targetType);

    /**
     * Find HTTP endpoint methods matching the given path prefix and verb.
     *
     * @param basePath base path to match against the stored route
     * @param httpMethod HTTP verb such as GET or POST
     * @return list of "class|signature" pairs
     */
    List<String> findHttpEndpoints(String basePath, String httpMethod);

    /**
     * Find controller classes that use the specified service class via injection.
     *
     * @param serviceClassName fully qualified service class name
     * @return list of controller class names
     */
    List<String> findControllersUsingService(String serviceClassName);

    /**
     * Find methods annotated with {@code @EventListener} for the given event type.
     *
     * @param eventType fully qualified event class name
     * @return list of "class|signature" pairs
     */
    List<String> findEventListeners(String eventType);

    /**
     * Find methods annotated with {@code @Scheduled}.
     *
     * @return list of "class|signature|cron" entries
     */
    List<String> findScheduledTasks();

    /**
     * Find classes or methods that reference the given configuration property key.
     *
     * @param propertyKey configuration property key
     * @return list of class names or "class|signature" for methods
     */
    List<String> findConfigPropertyUsage(String propertyKey);

    /**
     * Return a JSON tree of packages and classes starting from the given root package.
     *
     * @param rootPackage root package name
     * @param depth maximum depth of sub-packages to include, or {@code null} for no limit
     * @return nested JSON describing the package hierarchy
     */
    String getPackageHierarchy(String rootPackage, Integer depth);

    /**
     * Return basic statistics about the graph.
     *
     * <p>The JSON response includes total node and edge counts and a list of
     * the top connected classes sorted by degree.</p>
     *
     * @param topN maximum number of classes to return, or {@code null} for the default
     * @return JSON statistics string
     */
    String getGraphStatistics(Integer topN);

    /**
     * Export the entire graph to the given file in the specified format.
     *
     * <p>Supported formats are {@code "DOT"}, {@code "CSV"}, and {@code "JSON"}.
     * The implementation may use Neo4j APOC procedures if available or
     * perform custom serialization.</p>
     *
     * @param format one of "DOT", "CSV", or "JSON"
     * @param outputPath file system path to write the export file
     */
    void exportGraph(String format, String outputPath);
}
