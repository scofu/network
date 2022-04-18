plugins {
    id("com.scofu.common-build.base") version "1.0-SNAPSHOT"
}

dependencies {
    api(project(":network-message-api"))
    api("com.rabbitmq:amqp-client:5.12.0")
}

app {
    shadowFirstLevel()
}