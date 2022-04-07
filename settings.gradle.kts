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
        maven("https://repo.spongepowered.org/maven")
        maven("https://jitpack.io")
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
    "network-instance-api",
    "network-instance-bukkit",
    "network-instance-bungee",
    "network-instance-gateway"
).forEach {
    include(it)
    project(":$it").projectDir = file(it)
}
