plugins {
    `java-library`
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

description = "Nexus-Core Paper plugin implementation."

// Mockito must be attached as a Java agent explicitly on modern JDKs (dynamic self-attach is deprecated).
val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    mockitoAgent(libs.mockito.core) { isTransitive = false }

    paperweight.paperDevBundle(libs.versions.paper)

    implementation(project(":nexus-api"))
    compileOnly(libs.placeholderapi)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.launcher)
}

// Paper 26.x runs on Mojang's (unobfuscated) names, so no reobfuscation step is needed.
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    jar {
        archiveClassifier = "slim"
    }

    shadowJar {
        archiveBaseName = "NexusCore"
        archiveClassifier = ""
        mergeServiceFiles()
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        val agentJar = mockitoAgent.elements.map { files -> files.single().asFile.absolutePath }
        jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-javaagent:${agentJar.get()}") })
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = Charsets.UTF_8.name()
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion("26.3")
        downloadPlugins {
            modrinth("placeholderapi", libs.versions.placeholderapi.get())
        }
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }

    runPaper.folia.registerTask()
}
