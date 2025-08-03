package io.github.tfkfan.stomp.impl;

import io.github.tfkfan.stomp.StompWebsocketAdapter;
import io.github.tfkfan.stomp.StompMessageConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.stomp.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class StompWebsocketAdapterImpl implements StompWebsocketAdapter {
    private final StompServer stompServer;
    private final Map<String, StompServerConnection> sessionsMap = new HashMap<>(1000);
    private final Map<String, StompMessageConsumer> subscriptionsMap = new HashMap<>();

    public StompWebsocketAdapterImpl(Vertx vertx, String wsPath) {
        stompServer = StompServer.create(vertx, new StompServerOptions()
                        .setPort(-1)
                        .setWebsocketBridge(true)
                        .setWebsocketPath(wsPath))
                .handler(StompServerHandler.create(vertx)
                        .connectHandler(new DefaultConnectHandler() {
                            @Override
                            public void handle(ServerFrame frame) {
                                final String session = frame.connection().session();
                                log.trace("New client connected: {}", session);
                                super.handle(frame);
                                sessionsMap.put(session, frame.connection());
                            }
                        })
                        .closeHandler(frame -> {
                            final String session = frame.session();
                            log.trace("Client disconnected: {}", session);
                            sessionsMap.remove(session);
                        })
                        .receivedFrameHandler(frame -> {
                            final StompMessageConsumer wsConsumer = subscriptionsMap.get(frame.frame().getDestination());
                            if (wsConsumer != null)
                                wsConsumer.onMessage(frame);
                            else log.debug("Destination not found");
                        }));
    }

    @Override
    public MeterBinder meterBinder() {
        return meterRegistry -> {
            Gauge.builder("ws.sessions.count", sessionsMap::size)
                    .description("websocket sessions count")
                    .strongReference(true)
                    .register(meterRegistry);
            Gauge.builder("ws.stomp.subscriptions.count", subscriptionsMap::size)
                    .description("websocket stomp subscriptions count")
                    .strongReference(true)
                    .register(meterRegistry);
        };
    }

    @Override
    public void subscribe(String destination, StompMessageConsumer messageConsumer) {
        subscriptionsMap.put(destination, messageConsumer);
    }

    @Override
    public void send(String session, String destination, String message) {
        final StompServerConnection connection = sessionsMap.get(session);
        if (connection == null) {
            log.debug("Connection not found");
            return;
        }

        final Destination dest = connection.handler().getDestination(destination);
        if (dest == null) {
            log.debug("Destination not found for session {}", session);
            return;
        }
        try {
            final Frame frame = new Frame()
                    .setDestination(destination)
                    .setCommand(Command.MESSAGE)
                    .setBody(Buffer.buffer(message));
            dest.dispatch(connection, frame);
        } catch (Exception e) {
            log.warn("Error broadcasting frame to client: {}", e.getMessage());
        }
    }

    @Override
    public void broadcast(String destination, String message) {
        sessionsMap.forEach((session, connection) -> send(session, destination, message));
    }

    @Override
    public HttpServer initialize(HttpServer server) {
        return server.webSocketHandshakeHandler(stompServer.webSocketHandshakeHandler())
                .webSocketHandler(stompServer.webSocketHandler());
    }
}
