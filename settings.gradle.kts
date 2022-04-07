pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
        maven {
            name = "scofu"
            url = uri("https://repo.scofu.com/repository/maven-snapshots")
            credentials(PasswordCredentials::class)
        }
    }
}

dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
        maven {
            name = "scofu"
            url = uri("https://repo.scofu.com/repository/maven-snapshots")
            credentials(PasswordCredentials::class)
        }
    }
}

rootProject.name = "network-parent"

sequenceOf(
    "network-api",
    "network-message-api",
    "network-message-rabbitmq",
    "network-document-api",
    "network-document-service",
    "network-instance-api"
).forEach {
    include(it)
    project(":$it").projectDir = file(it)
}
