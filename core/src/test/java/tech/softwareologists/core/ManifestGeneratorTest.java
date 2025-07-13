package tech.softwareologists.core;

import org.junit.Test;

public class ManifestGeneratorTest {
    @Test
    public void generate_containsFindCallersCapability() {
        String manifest = ManifestGenerator.generate();
        if (!manifest.contains("findCallers")) {
            throw new AssertionError("Manifest missing findCallers capability: " + manifest);
        }
        if (!manifest.contains("findImplementations")) {
            throw new AssertionError("Manifest missing findImplementations capability: " + manifest);
        }
        if (!manifest.contains("findSubclasses")) {
            throw new AssertionError("Manifest missing findSubclasses capability: " + manifest);
        }
        if (!manifest.contains("findDependencies")) {
            throw new AssertionError("Manifest missing findDependencies capability: " + manifest);
        }
        if (!manifest.contains("findPathBetweenClasses")) {
            throw new AssertionError("Manifest missing findPathBetweenClasses capability: " + manifest);
        }
        if (!manifest.contains("findMethodsCallingMethod")) {
            throw new AssertionError("Manifest missing findMethodsCallingMethod capability: " + manifest);
        }
        if (!manifest.contains("findBeansWithAnnotation")) {
            throw new AssertionError("Manifest missing findBeansWithAnnotation capability: " + manifest);
        }
        if (!manifest.contains("searchByAnnotation")) {
            throw new AssertionError("Manifest missing searchByAnnotation capability: " + manifest);
        }
        if (!manifest.contains("findHttpEndpoints")) {
            throw new AssertionError("Manifest missing findHttpEndpoints capability: " + manifest);
        }
        if (!manifest.contains("findControllersUsingService")) {
            throw new AssertionError("Manifest missing findControllersUsingService capability: " + manifest);
        }
        if (!manifest.contains("findEventListeners")) {
            throw new AssertionError("Manifest missing findEventListeners capability: " + manifest);
        }
        if (!manifest.contains("findScheduledTasks")) {
            throw new AssertionError("Manifest missing findScheduledTasks capability: " + manifest);
        }
        if (!manifest.contains("findConfigPropertyUsage")) {
            throw new AssertionError("Manifest missing findConfigPropertyUsage capability: " + manifest);
        }
        if (!manifest.contains("getPackageHierarchy")) {
            throw new AssertionError("Manifest missing getPackageHierarchy capability: " + manifest);
        }
        if (!manifest.contains("getGraphStatistics")) {
            throw new AssertionError("Manifest missing getGraphStatistics capability: " + manifest);
        }
        if (!manifest.contains("exportGraph")) {
            throw new AssertionError("Manifest missing exportGraph capability: " + manifest);
        }

        if (!manifest.contains("\"defaults\"")) {
            throw new AssertionError("Manifest missing defaults section: " + manifest);
        }
        if (!manifest.contains("\"limit\":100")) {
            throw new AssertionError("Manifest missing default limit: " + manifest);
        }
        if (!manifest.contains("\"pageSize\":50")) {
            throw new AssertionError("Manifest missing default pageSize: " + manifest);
        }
        if (!manifest.contains("\"targetType\":\"class\"")) {
            throw new AssertionError("Manifest missing searchByAnnotation targetType default: " + manifest);
        }
    }
}
