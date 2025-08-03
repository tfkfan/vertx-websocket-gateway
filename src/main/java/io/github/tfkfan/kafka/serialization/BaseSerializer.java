package io.github.tfkfan.kafka.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.json.jackson.DatabindCodec;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public abstract class BaseSerializer<T> implements Serializer<T> {
    @Override
    public byte[] serialize(String s, T gatewayOutputMessage) {
        try {
            if (gatewayOutputMessage == null)
                return null;

            return DatabindCodec.mapper().writeValueAsBytes(gatewayOutputMessage);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error when serializing GatewayKafkaMessage to byte[] due to json processing error: " + e.getMessage(), e);
        }
    }
}
