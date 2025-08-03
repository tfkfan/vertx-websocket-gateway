package io.github.tfkfan.kafka.message;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GatewayInputMessage {
    private String sender;
    private String message;
    private String createdAt;
}
