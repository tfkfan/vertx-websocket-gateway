package io.github.tfkfan.kafka;

import io.github.tfkfan.config.Constants;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

@Slf4j
public class GatewayKafkaConsumer {
    private final Vertx vertx;
    private final KafkaConsumer<String, GatewayMessage> consumer;

    public GatewayKafkaConsumer(Vertx vertx, JsonObject config, String bootstrapServers) {
        this.vertx = vertx;
        consumer = kafkaConsumer(vertx, config, bootstrapServers)
                .exceptionHandler(err -> log.error("Kafka consumer error", err));
    }

    public void subscribe(final String topic, Handler<KafkaConsumerRecord<String, GatewayMessage>> handler) {
        consumer.handler(handler);
        consumer.subscribe(topic);
    }

    private KafkaConsumer<String, GatewayMessage> kafkaConsumer(Vertx vertx, JsonObject cnf, String bootstrapServers) {
        return KafkaConsumer.create(vertx, Map.of(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP,
                cnf.getString(Constants.KAFKA_BOOTSTRAPSERVERS_ENV, bootstrapServers),
                Constants.KAFKA_KEY_DESERIALIZER_PROP, StringDeserializer.class.getName(),
                Constants.KAFKA_VALUE_DESERIALIZER_PROP, GatewayMessageDeserializer.class.getName(),
                Constants.KAFKA_GROUP_ID_PROP, "my_group",
                Constants.KAFKA_AUTO_OFFSET_RESET_PROP, "earliest",
                Constants.KAFKA_AUTO_COMMIT_PROP, "true"));
    }
}
