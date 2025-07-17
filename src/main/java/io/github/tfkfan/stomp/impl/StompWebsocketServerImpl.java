package io.github.tfkfan.stomp.impl;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.stomp.StompWebsocketServer;
import io.github.tfkfan.stomp.SubscriptionCallback;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.stomp.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class StompWebsocketServerImpl implements StompWebsocketServer {
    private final StompServer stompServer;
    private final Map<String, StompServerConnection> sessionsMap = new HashMap<>(1000);
    private final Map<String, SubscriptionCallback> subscriptionsMap = new HashMap<>();

    public StompWebsocketServerImpl(Vertx vertx) {
        stompServer = StompServer.create(vertx, new StompServerOptions()
                        .setPort(-1)
                        .setWebsocketBridge(true)
                        .setWebsocketPath(Constants.WEBSOCKET_PATH))
                .handler(StompServerHandler.create(vertx)
                        .connectHandler(new DefaultConnectHandler() {
                            @Override
                            public void handle(ServerFrame frame) {
                                log.trace("New client connected");
                                super.handle(frame);
                                sessionsMap.put(frame.connection().session(), frame.connection());
                            }
                        })
                        .closeHandler(frame -> {
                            log.trace("Client disconnected");
                            sessionsMap.remove(frame.session());
                        })
                        .receivedFrameHandler(frame -> {
                            final SubscriptionCallback wsConsumer = subscriptionsMap.get(frame.frame().getDestination());
                            if (wsConsumer != null)
                                wsConsumer.onMessage(frame);
                            else log.debug("Destination not found");
                        }));
    }

    @Override
    public void subscribe(String destination, SubscriptionCallback messageConsumer) {
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
        final Frame frame = new Frame()
                .setDestination(destination)
                .setCommand(Command.MESSAGE)
                .setBody(Buffer.buffer(message));

        sessionsMap.forEach((session, connection) -> {
            try {
                final Destination dest = connection.handler().getDestination(destination);
                if (dest == null) {
                    log.debug("Destination not found for session {}", session);
                    return;
                }

                dest.dispatch(connection, frame);
            } catch (Exception e) {
                log.warn("Error broadcasting frame to client: {}", e.getMessage());
            }
        });
    }

    @Override
    public HttpServer initialize(HttpServer server) {
        return server.webSocketHandshakeHandler(stompServer.webSocketHandshakeHandler())
                .webSocketHandler(stompServer.webSocketHandler());
    }
}
