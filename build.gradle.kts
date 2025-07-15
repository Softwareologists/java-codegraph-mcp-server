plugins {
    `java-library`
    id("com.diffplug.spotless") version "7.1.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("junit:junit:4.13.2")
    }

    java {
        // Compile with JDK 21 but target Java 17 bytecode
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    spotless {
        java {
            target("src/**/*.java")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
