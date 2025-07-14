plugins {
    id("org.jetbrains.intellij") version "1.17.3"
}

intellij {
    pluginName.set("CodeGraphMcp")
    version.set("2021.3")
    type.set("IC")
    plugins.set(listOf("java"))
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
