plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("tech.softwareologists.cli.CliMain")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("cli-all.jar")
    manifest.attributes(mapOf("Main-Class" to "tech.softwareologists.cli.CliMain"))
    isZip64 = true
}

dependencies {
    implementation(project(":core"))
    implementation("org.neo4j.test:neo4j-harness:5.19.0")
    implementation("org.neo4j.driver:neo4j-java-driver:5.19.0")
    implementation("io.github.classgraph:classgraph:4.8.180")
}

