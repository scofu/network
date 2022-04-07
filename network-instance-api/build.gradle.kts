plugins {
    id("base-conventions")
}

dependencies {
    api(project(":network-document-api"))
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}