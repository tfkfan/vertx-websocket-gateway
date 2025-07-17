package io.github.tfkfan.stomp;

import io.vertx.ext.stomp.Frame;
import io.vertx.ext.stomp.StompServerConnection;

public interface SubscriptionCallback {
    void onMessage(StompWebsocketServer srv, StompServerConnection connection, Frame message);
}
