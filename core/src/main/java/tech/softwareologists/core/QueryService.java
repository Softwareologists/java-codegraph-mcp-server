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
}
