plugins {
    id("base-conventions")
}

dependencies {
    api(project(":network-message-api"))
    api("com.rabbitmq:amqp-client:5.12.0")
}

app {
    shadowFirstLevel()
}