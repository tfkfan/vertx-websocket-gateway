package io.github.tfkfan.kafka.message;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GatewayOutputMessage {
    private String sender;
    private String stompChannel;
    private String message;
    private String createdAt;
    private String recipient;
}
