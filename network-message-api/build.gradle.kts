plugins {
    id("com.scofu.common-build.base") version "1.0-SNAPSHOT"
}

dependencies {
    api("com.scofu:common-inject:1.0-SNAPSHOT")
    api("com.scofu:common-json:1.0-SNAPSHOT")
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}