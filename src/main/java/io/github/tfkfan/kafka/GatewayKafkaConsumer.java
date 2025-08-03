package io.github.tfkfan.kafka;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.kafka.message.GatewayOutputMessage;
import io.github.tfkfan.kafka.serialization.GatewayOutputMessageDeserializer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;

@Slf4j
public class GatewayKafkaConsumer {
    private final KafkaConsumer<String, GatewayOutputMessage> consumer;

    public GatewayKafkaConsumer(Vertx vertx, JsonObject config, String bootstrapServers) {
        consumer = kafkaConsumer(vertx, config, bootstrapServers)
                .exceptionHandler(err -> log.error("Kafka consumer error", err));
    }

    public void subscribe(final String topic, Handler<KafkaConsumerRecord<String, GatewayOutputMessage>> handler) {
        consumer.handler(record -> {
            if (validateMessage(record.value()))
                handler.handle(record);
        });
        consumer.subscribe(topic);
    }

    private boolean validateMessage(GatewayOutputMessage message) {
        if (message.getRecipient() == null) {
            log.warn("Message recipient is null");
            return false;
        }
        if (message.getSender() == null) {
            log.warn("Message sender is null");
            return false;
        }
        if (message.getStompChannel() == null) {
            log.warn("Message stomp channel is null");
            return false;
        }
        return true;
    }

    private KafkaConsumer<String, GatewayOutputMessage> kafkaConsumer(Vertx vertx, JsonObject cnf, String bootstrapServers) {
        return KafkaConsumer.create(vertx, Map.of(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP,
                cnf.getString(Constants.KAFKA_BOOTSTRAPSERVERS_ENV, bootstrapServers),
                Constants.KAFKA_KEY_DESERIALIZER_PROP, StringDeserializer.class.getName(),
                Constants.KAFKA_VALUE_DESERIALIZER_PROP, GatewayOutputMessageDeserializer.class.getName(),
                Constants.KAFKA_GROUP_ID_PROP, "my_group",
                Constants.KAFKA_AUTO_OFFSET_RESET_PROP, "earliest",
                Constants.KAFKA_AUTO_COMMIT_PROP, "true"));
    }
}
