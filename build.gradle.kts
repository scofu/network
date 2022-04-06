plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

nexusPublishing {
    repositories {
        create("scofu") {
            useStaging.set(false)
            nexusUrl.set(uri("https://repo.scofu.com/"))
            snapshotRepositoryUrl.set(uri("https://repo.scofu.com/repository/maven-snapshots"))
        }
    }
}