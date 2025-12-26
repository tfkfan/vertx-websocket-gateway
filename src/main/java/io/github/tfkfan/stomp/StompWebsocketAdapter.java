package io.github.tfkfan.stomp;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.vertx.core.http.HttpServer;

public interface StompWebsocketAdapter {
    MeterBinder meterBinder();

    void subscribe(String destination, StompMessageConsumer messageConsumer);

    void send(String session, String destination, String message);

    void send(String id, String webSocketSessionId, String destination, Object message);

    void broadcast(String destination, String message);

    HttpServer initialize(HttpServer server);
}
