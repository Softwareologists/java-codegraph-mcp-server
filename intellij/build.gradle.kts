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
    implementation("org.json:json:20240303")
    testImplementation(project(":core"))
}
