package io.github.tfkfan.stomp;

import io.vertx.ext.stomp.ServerFrame;

@FunctionalInterface
public interface StompMessageConsumer {
    void onMessage(ServerFrame serverFrame);
}
