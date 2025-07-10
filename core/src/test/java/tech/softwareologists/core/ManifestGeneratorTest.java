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
        if (!manifest.contains("findMethodsCallingMethod")) {
            throw new AssertionError("Manifest missing findMethodsCallingMethod capability: " + manifest);
        }
        if (!manifest.contains("findBeansWithAnnotation")) {
            throw new AssertionError("Manifest missing findBeansWithAnnotation capability: " + manifest);
        }
        if (!manifest.contains("searchByAnnotation")) {
            throw new AssertionError("Manifest missing searchByAnnotation capability: " + manifest);
        }
        if (!manifest.contains("findControllersUsingService")) {
            throw new AssertionError("Manifest missing findControllersUsingService capability: " + manifest);
        }
    }
}
