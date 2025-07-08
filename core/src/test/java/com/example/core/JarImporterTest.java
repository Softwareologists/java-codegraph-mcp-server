package com.example.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;

import org.junit.Test;

public class JarImporterTest {
    @Test
    public void importJar_emptyJar_noException() throws IOException {
        File jar = File.createTempFile("empty", ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            // create empty jar
        }
        JarImporter.importJar(jar);
    }
}
