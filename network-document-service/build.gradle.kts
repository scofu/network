plugins {
    id("base-conventions")
}

dependencies {
    implementation(project(":network-document-api"))
    implementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
    implementation("org.mongodb:mongo-java-driver:3.12.10")
}
app {
    shadowFirstLevel();
    mainClass.set("com.scofu.network.document.service.NetworkDocumentService")
}