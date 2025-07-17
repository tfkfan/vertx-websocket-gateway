package io.github.tfkfan.stomp;

import io.vertx.ext.stomp.ServerFrame;

@FunctionalInterface
public interface SubscriptionCallback {
    void onMessage(ServerFrame serverFrame);
}
