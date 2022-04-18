plugins {
    id("com.scofu.common-build.base") version "1.0-SNAPSHOT"
}

dependencies {
    api(project(":network-instance-api"))
    implementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
    implementation("io.fabric8:kubernetes-client:5.4.1")
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}

app {
    shadowFirstLevel();
    mainClass.set("com.scofu.network.instance.service.NetworkInstanceService")
}