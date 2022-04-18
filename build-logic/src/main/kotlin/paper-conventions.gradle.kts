import org.gradle.kotlin.dsl.invoke

plugins {
    id("base-conventions")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("net.minecrell.plugin-yml.bukkit")
}

dependencies {
    paperDevBundle("1.18.1-R0.1-SNAPSHOT")
}

val app = the<AppExtension>()
// Default plugin.yml
bukkit {
    version = project.version as String
    description = project.description
    apiVersion = "1.18"
    authors = listOf("jesper@scofu.com")
    main = app.mainClass.getOrElse(".")
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    // Implicitly changes the jar to the re-obfuscated one and removes the 'dev' classifier.
    register("publishReObfuscated") {
        dependsOn(reobfJar)
        finalizedBy(publish)
        group = "Publishing"
        description = "Publishes the re-obfuscated jar in a *hacky* way."
        doFirst {
            publishing {
                publications {
                    named<MavenPublication>("mavenJava") {
                        artifactId = artifactId + "-reobf"
                    }
                }
            }
            project.tasks.named<Jar>("jar") {
                archiveClassifier.set("")
                from(reobfJar)
            }
        }
    }
}

