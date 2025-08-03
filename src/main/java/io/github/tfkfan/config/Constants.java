package io.github.tfkfan.config;

public interface Constants {
    String DIVIDER = ":";
    String HEALTH_PATH = "/health";
    String READINESS_PATH = "/readiness";
    String WEBSOCKET_PATH = "/ws";
    String STATIC_FOLDER_PATH = "static";
    String FILE = "file";
    String YAML = "yaml";
    String PATH = "path";
    String ENV = "env";

    String VERTX_WS_BROADCAST_CHANNEL = "ws.broadcast.channel";
    String KAFKA_WEBSOCKET_OUTPUT_TOPIC = "websocket_gateway_output_topic";
    String STOMP_DEFAULT_INPUT_CHANNEL = "/example_input";
    String KAFKA_DEFAULT_INPUT_TOPIC = "ebsocket_gateway_example_input_topic";

    String KAFKA_BOOTSTRAPSERVERS_ENV = "KAFKA_BOOTSTRAPSERVERS";
    String APP_INPUT_MAPPING_ENV = "APP_INPUT_STOMP_KAFKA_MAPPING";

    String KAFKA_KEY_SERIALIZER_PROP = "key.serializer";
    String KAFKA_VALUE_SERIALIZER_PROP = "value.serializer";
    String KAFKA_KEY_DESERIALIZER_PROP = "key.deserializer";
    String KAFKA_VALUE_DESERIALIZER_PROP = "value.deserializer";
    String KAFKA_GROUP_ID_PROP = "group.id";
    String KAFKA_AUTO_OFFSET_RESET_PROP = "auto.offset.reset";
    String KAFKA_AUTO_COMMIT_PROP = "enable.auto.commit";
    String KAFKA_ACKS_PROP = "acks";
    String KAFKA_PROP = "kafka";
    String KAFKA_BOOTSTRAP_SERVERS_PROP = "bootstrap.servers";
    String BOOTSTRAP_SERVERS_PROP = "bootstrap-servers";
    String SERVER_PROP = "server";
    String PORT_PROP = "port";
}
