plugins {
    `java-library`
    id("com.diffplug.spotless") version "6.25.0"
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

    tasks.withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    spotless {
        java {
            target("src/**/*.java")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
