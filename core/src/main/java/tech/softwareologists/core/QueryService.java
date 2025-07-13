package tech.softwareologists.core;

import java.util.List;
import tech.softwareologists.core.QueryResult;

/**
 * Service API for querying the code graph.
 */

public interface QueryService {
    /**
     * Find classes that reference the given class.
     *
     * @param className fully qualified class name
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return callers of the class
     */
    QueryResult<String> findCallers(String className, Integer limit, Integer page, Integer pageSize);

    /**
     * Find classes implementing the specified interface.
     *
     * @param interfaceName fully qualified interface name
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return implementing class names
     */
    QueryResult<String> findImplementations(String interfaceName, Integer limit, Integer page, Integer pageSize);

    /**
     * Find subclasses of a class up to the given depth.
     *
     * @param className fully qualified base class name
     * @param depth search depth
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return subclass names
     */
    QueryResult<String> findSubclasses(String className, int depth, Integer limit, Integer page, Integer pageSize);

    /**
     * Find the classes that the given class depends on.
     *
     * @param className name of the source class
     * @param depth maximum dependency search depth
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return dependent class names
     */
    QueryResult<String> findDependencies(String className, Integer depth, Integer limit, Integer page, Integer pageSize);

    /**
     * Find the shortest dependency path from one class to another.
     *
     * @param fromClass starting class name
     * @param toClass target class name
     * @param maxDepth maximum path length, or {@code null} for no limit
     * @return ordered list of class names from source to target, or empty if no path
     */
    QueryResult<String> findPathBetweenClasses(String fromClass, String toClass, Integer maxDepth);

    /**
     * Find methods that invoke a given target method.
     *
     * @param className fully qualified class name containing the target method
     * @param methodSignature JVM signature of the target method
     * @param limit optional maximum number of caller methods
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return caller method signatures
     */
    QueryResult<String> findMethodsCallingMethod(String className, String methodSignature, Integer limit, Integer page, Integer pageSize);

    /**
     * Find classes annotated with the given annotation.
     *
     * @param annotation fully qualified annotation name
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return class names
     */
    QueryResult<String> findBeansWithAnnotation(String annotation, Integer limit, Integer page, Integer pageSize);

    /**
     * Generic search for nodes annotated with the given annotation.
     *
     * @param annotation fully qualified annotation name
     * @param targetType "class" or "method" to indicate target node label
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return class names or method signatures
     */
    QueryResult<String> searchByAnnotation(String annotation, String targetType, Integer limit, Integer page, Integer pageSize);

    /**
     * Find HTTP endpoint methods matching the given path prefix and verb.
     *
     * @param basePath base path to match against the stored route
     * @param httpMethod HTTP verb such as GET or POST
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return "class|signature" pairs
     */
    QueryResult<String> findHttpEndpoints(String basePath, String httpMethod, Integer limit, Integer page, Integer pageSize);

    /**
     * Find controller classes that use the specified service class via injection.
     *
     * @param serviceClassName fully qualified service class name
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return controller class names
     */
    QueryResult<String> findControllersUsingService(String serviceClassName, Integer limit, Integer page, Integer pageSize);

    /**
     * Find methods annotated with {@code @EventListener} for the given event type.
     *
     * @param eventType fully qualified event class name
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return "class|signature" pairs
     */
    QueryResult<String> findEventListeners(String eventType, Integer limit, Integer page, Integer pageSize);

    /**
     * Find methods annotated with {@code @Scheduled}.
     *
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return "class|signature|cron" entries
     */
    QueryResult<String> findScheduledTasks(Integer limit, Integer page, Integer pageSize);

    /**
     * Find classes or methods that reference the given configuration property key.
     *
     * @param propertyKey configuration property key
     * @param limit optional maximum number of results
     * @param page result page number starting at 1
     * @param pageSize number of items per page
     * @return class names or "class|signature" for methods
     */
    QueryResult<String> findConfigPropertyUsage(String propertyKey, Integer limit, Integer page, Integer pageSize);

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
