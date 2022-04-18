plugins {
    id("com.scofu.common-build.base") version "1.0-SNAPSHOT"
}

dependencies {
    api(project(":network-instance-api"))
    implementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
    implementation("net.renfei:cloudflare:0.0.3")
    implementation("com.discord4j:discord4j-core:3.2.0")
    testImplementation("com.scofu:app-bootstrap-api:1.0-SNAPSHOT")
}

app {
    shadowFirstLevel();
    mainClass.set("com.scofu.network.instance.discord.NetworkInstanceDiscord")
}