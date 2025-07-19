package io.github.tfkfan.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.json.jackson.DatabindCodec;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

public class GatewayMessageDeserializer implements Deserializer<GatewayMessage> {
    @Override
    public GatewayMessage deserialize(String topic, byte[] data) {
        try {
            if (data == null)
                return null;

            return DatabindCodec.mapper().readValue(data, GatewayMessage.class);
        } catch (IOException e) {
            throw new SerializationException("Error when serializing GatewayKafkaMessage to byte[] due to json processing error: " + e.getMessage(), e);
        }
    }
}
