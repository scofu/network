plugins {
    id("bungee-conventions")
}

dependencies {
    api(project(":network-instance-api"))
    implementation("com.scofu:command-api:1.0-SNAPSHOT")
    implementation("io.fabric8:kubernetes-client:5.4.1")
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}

app {
    shadowFirstLevel();
}