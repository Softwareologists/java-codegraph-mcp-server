plugins {
    application
}

application {
    mainClass.set("tech.softwareologists.cli.Main")
}

dependencies {
    implementation(project(":core"))
}

