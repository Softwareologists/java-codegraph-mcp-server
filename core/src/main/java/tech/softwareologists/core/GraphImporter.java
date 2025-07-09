package tech.softwareologists.core;

import org.neo4j.driver.Driver;

public interface GraphImporter {
    void importJar(java.io.File jar, Driver driver);
}
