package io.github.tfkfan.kafka.serialization;

import io.github.tfkfan.kafka.message.GatewayInputMessage;

public class GatewayInputMessageDeserializer extends BaseDeserializer<GatewayInputMessage> {
    public GatewayInputMessageDeserializer() {
        super(GatewayInputMessage.class);
    }
}
