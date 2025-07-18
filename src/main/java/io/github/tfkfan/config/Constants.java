package io.github.tfkfan.config;

public interface Constants {
    String HEALTH_PATH = "/health";
    String READINESS_PATH = "/readiness";
    String WEBSOCKET_PATH = "/ws";
    String STATIC_FOLDER_PATH = "static";
    String FILE = "file";
    String YAML = "yaml";
    String PATH = "path";
    String ENV = "env";
    String VERTX_WS_BROADCAST_CHANNEL = "ws.broadcast.channel";

    String KAFKA_BOOTSTRAPSERVERS_ENV = "KAFKA_BOOTSTRAPSERVERS";

    String KAFKA_KEY_SERIALIZER_PROP = "key.serializer";
    String KAFKA_VALUE_SERIALIZER_PROP = "value.serializer";
    String KAFKA_GROUP_ID_PROP = "group.id";
    String KAFKA_AUTO_OFFSET_RESET_PROP = "auto.offset.reset";
    String KAFKA_AUTO_COMMIT_PROP = "enable.auto.commit";
    String KAFKA_ACKS_PROP = "acks";
    String KAFKA_BOOTSTRAP_SERVERS_PROP = "bootstrap.servers";
    String SERVER_PORT_PROP = "server.port";
}
