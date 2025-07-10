plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("io.github.classgraph:classgraph:4.8.180")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.neo4j.test:neo4j-harness:5.19.0")
    api("org.neo4j.driver:neo4j-java-driver:5.19.0")
}

