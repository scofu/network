plugins {
    id("com.scofu.common-build.base") version "1.0-SNAPSHOT"
}

dependencies {
    api(project(":network-message-api"))
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}