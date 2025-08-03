package io.github.tfkfan.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.tfkfan.config.Constants;
import io.github.tfkfan.kafka.message.GatewayInputMessage;
import io.github.tfkfan.kafka.serialization.GatewayOutputMessageSerializer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.impl.KafkaProducerRecordImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

@Slf4j
public class GatewayKafkaProducer {
    private final Vertx vertx;
    private final KafkaProducer<String, GatewayInputMessage> producer;

    public GatewayKafkaProducer(Vertx vertx, JsonObject config, String bootstrapServers) {
        this.vertx = vertx;
        producer = kafkaProducer(vertx, config, bootstrapServers);
    }

    public void send(String topic, String message){
        try {
            send(topic, DatabindCodec.mapper().readValue(message, GatewayInputMessage.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String topic, GatewayInputMessage message) {
        producer.send(new KafkaProducerRecordImpl<>(topic, message));
    }

    private KafkaProducer<String, GatewayInputMessage> kafkaProducer(Vertx vertx, JsonObject cnf, String bootstrapServers) {
        return KafkaProducer.create(vertx, Map.of(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP,
                cnf.getString(Constants.KAFKA_BOOTSTRAPSERVERS_ENV, bootstrapServers),
                Constants.KAFKA_KEY_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_VALUE_SERIALIZER_PROP, GatewayOutputMessageSerializer.class.getName(),
                Constants.KAFKA_ACKS_PROP, "1"));
    }
}
