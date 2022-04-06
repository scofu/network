plugins {
    `java-library`
    `maven-publish`
    checkstyle
}

apply<AppPlugin>()

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        name = "scofu"
        url = uri("https://repo.scofu.com/repository/maven-snapshots")
        credentials(PasswordCredentials::class)
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

checkstyle {
    toolVersion = "10.2-SNAPSHOT"
    config =
        resources.text.fromUri(uri("https://raw.githubusercontent.com/checkstyle/checkstyle/8f6e6f341450b5de264b5fd67d0777fbeaaee58a/src/main/resources/google_checks.xml"))
    println(configFile)
    maxWarnings = 0
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
        options.compilerArgs.add("-Xlint:deprecation") // Enable deprecation messages
        options.compilerArgs.add("-Xlint:unchecked") // Enable unchecked/unsafe messages
        options.compilerArgs.add("-parameters") // Preserve parameter names
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    test {
        useJUnitPlatform()
        maxHeapSize = "1G"
        failFast = true
    }

    jar {
        val app = project.the<AppExtension>();
        if (app.mainClass.isPresent) {
            manifest {
                attributes["Specification-Title"] = project.group
                attributes["Specification-Version"] = project.version
                attributes["Specification-Vendor"] = "Scofu"
                attributes["Main-Class"] = app.mainClass.get()
            }
        }
        doFirst {
            val exclusions =
                if (app.skipExclusion.get()) {
                    emptyList<String>()
                } else {
                    configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
                        .filter { it.moduleVersion.id.group.startsWith("com.scofu") }
                        .map { it.file.name }
                }
            when (app.shadow.get()) {
                AppShadowing.FULL -> {
                    duplicatesStrategy = DuplicatesStrategy.WARN

                    val filteredClasspath = configurations.runtimeClasspath.get()
                        .filter { !exclusions.contains(it.name) }
                        .map { if (it.isDirectory) it else if (it.exists()) zipTree(it) else it }
                    from(filteredClasspath)
                }
                AppShadowing.FIRST_LEVEL -> {
                    duplicatesStrategy = DuplicatesStrategy.WARN

                    val firstLevelDependencies =
                        configurations.compileClasspath.get().resolvedConfiguration.firstLevelModuleDependencies
                            .filter { !it.module.id.group.startsWith("com.scofu") }
                            .flatMap { it.allModuleArtifacts }
                            .map { it.file.name }

                    println("${project.name}-fld: ${firstLevelDependencies}")

                    val filteredClasspath = configurations.runtimeClasspath.get()
                        .filter { !exclusions.contains(it.name) }
                        .filter { firstLevelDependencies.contains(it.name) }
                        .map { if (it.isDirectory) it else if (it.exists()) zipTree(it) else it }

                    from(filteredClasspath)
                }
                else -> {}
            }
        }
    }
}