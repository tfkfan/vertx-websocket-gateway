package io.github.tfkfan.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GatewayMessage {
    private String sender;
    private String stompChannel;
    private String message;
    private String createdAt;
    private String recipient;
}
