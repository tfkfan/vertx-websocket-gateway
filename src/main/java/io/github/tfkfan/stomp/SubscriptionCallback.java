package io.github.tfkfan.stomp;

import io.vertx.ext.stomp.ServerFrame;

public interface SubscriptionCallback {
    void onMessage(ServerFrame serverFrame);
}
