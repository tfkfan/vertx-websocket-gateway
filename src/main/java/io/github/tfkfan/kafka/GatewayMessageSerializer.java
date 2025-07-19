package io.github.tfkfan.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.json.jackson.DatabindCodec;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class GatewayMessageSerializer implements Serializer<GatewayMessage> {
    @Override
    public byte[] serialize(String s, GatewayMessage gatewayMessage) {
        try {
            if (gatewayMessage == null)
                return null;

            return DatabindCodec.mapper().writeValueAsBytes(gatewayMessage);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error when serializing GatewayKafkaMessage to byte[] due to json processing error: " + e.getMessage(), e);
        }
    }
}
