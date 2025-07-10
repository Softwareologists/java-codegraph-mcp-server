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
}
