package tech.softwareologists.core;

import java.util.List;

public interface QueryService {
    List<String> findCallers(String className);
}
