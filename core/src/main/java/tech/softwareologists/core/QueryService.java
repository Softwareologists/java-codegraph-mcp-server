package tech.softwareologists.core;

import java.util.List;

public interface QueryService {
    List<String> findCallers(String className);

    List<String> findImplementations(String interfaceName);

    List<String> findSubclasses(String className, int depth);
}
