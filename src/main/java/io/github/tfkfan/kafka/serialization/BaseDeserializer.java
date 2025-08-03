package io.github.tfkfan.kafka.serialization;

import io.vertx.core.json.jackson.DatabindCodec;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

@RequiredArgsConstructor
public abstract class BaseDeserializer<T> implements Deserializer<T> {
    private final Class<T> clazz;

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            if (data == null)
                return null;

            return DatabindCodec.mapper().readValue(data, clazz);
        } catch (IOException e) {
            throw new SerializationException("Error when serializing GatewayKafkaMessage to byte[] due to json processing error: " + e.getMessage(), e);
        }
    }
}
