package io.github.tfkfan.stomp;

import io.vertx.core.http.HttpServer;

public interface StompWebsocketAdapter {
    void subscribe(String destination, SubscriptionCallback messageConsumer);

    void send(String session, String destination, String message);

    void broadcast(String destination, String message);

    HttpServer initialize(HttpServer server);
}
