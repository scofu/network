plugins {
    id("com.scofu.common-build.bukkit") version "1.0-SNAPSHOT"
}

dependencies {
    api(project(":network-instance-api"))
    implementation("com.scofu:command-api:1.0-SNAPSHOT")
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}