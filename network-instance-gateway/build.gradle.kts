plugins {
    id("base-conventions")
}

repositories {
    maven("https://repo.spongepowered.org/maven")
    maven("https://jitpack.io")
}

dependencies {
    api(project(":network-instance-api"))
    implementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
    implementation("com.scofu:text-api:1.0-SNAPSHOT")
    implementation("com.github.Minestom:Minestom:821063addf")
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}

app {
    shadowFirstLevel();
    mainClass.set("com.scofu.network.instance.gateway.NetworkInstanceGateway")
}