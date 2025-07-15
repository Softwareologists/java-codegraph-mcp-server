plugins {
    id("org.jetbrains.intellij") version "1.17.3"
}

intellij {
    pluginName.set("CodeGraphMcp")
    // Target IntelliJ IDEA 2024.1 to support recent IDE versions
    version.set("2024.1")
    type.set("IC")
    plugins.set(listOf("java"))
    // Keep manually specified since/until-build values from plugin.xml
    updateSinceUntilBuild.set(false)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    main {
        resources.srcDir("resources")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.neo4j.test:neo4j-harness:5.19.0") {
        exclude(group = "org.neo4j", module = "arrow-bom")
    }
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("org.json:json:20240303")
    testImplementation(project(":core"))
}

configurations.all {
    exclude(group = "org.neo4j", module = "arrow-bom")
}

tasks {
    patchPluginXml {
        changeNotes.set(provider {
            val f = file("CHANGELOG.md")
            if (f.exists()) f.readText() else ""
        })
    }
}
