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
    }
}
