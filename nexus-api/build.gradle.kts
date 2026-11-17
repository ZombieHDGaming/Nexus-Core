plugins {
    `java-library`
    `maven-publish`
}

description = "Public API for Nexus-Core. Depend on this (compileOnly) from add-ons and other plugins."

java {
    withJavadocJar()
}

dependencies {
    compileOnlyApi(libs.paper.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "nexus-api"
            from(components["java"])
        }
    }
}
