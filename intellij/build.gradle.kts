plugins {
    id("org.jetbrains.intellij") version "1.17.3"
}

intellij {
    pluginName.set("CodeGraphMcp")
    version.set("2021.3")
    type.set("IC")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":core"))
    testImplementation(project(":core"))
}
