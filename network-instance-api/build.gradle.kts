plugins {
    id("com.scofu.common-build.base") version "1.0-SNAPSHOT"
}

dependencies {
    api(project(":network-document-api"))
    api("com.scofu:text-api:1.0-SNAPSHOT")
    api("com.scofu:text-json:1.0-SNAPSHOT")
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}