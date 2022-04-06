plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    implementation("io.papermc.paperweight.userdev:io.papermc.paperweight.userdev.gradle.plugin:1.3.5")
    implementation("xyz.jpenilla:run-paper:1.0.6")
    implementation("net.minecrell.plugin-yml.bukkit:net.minecrell.plugin-yml.bukkit.gradle.plugin:0.5.1")
    implementation("net.minecrell.plugin-yml.bungee:net.minecrell.plugin-yml.bungee.gradle.plugin:0.5.1")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}