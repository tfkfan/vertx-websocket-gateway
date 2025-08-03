package io.github.tfkfan.kafka.serialization;

import io.github.tfkfan.kafka.message.GatewayOutputMessage;

public class GatewayOutputMessageDeserializer extends BaseDeserializer<GatewayOutputMessage> {

    public GatewayOutputMessageDeserializer() {
        super(GatewayOutputMessage.class);
    }
}