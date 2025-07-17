package io.github.tfkfan.stomp;

import io.github.tfkfan.config.Constants;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.stomp.*;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class StompWebsocketServer {
    private final StompServer stompServer;
    private final Map<String, StompServerConnection> clients = new HashMap<>();
    private final Map<String, Map<String, Destination>> destinationsMap = new HashMap<>();
    private final Map<String, SubscriptionCallback> wsSubscriptions = new HashMap<>();

    public StompWebsocketServer(Vertx vertx) {
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
                                clients.put(frame.connection().session(), frame.connection());
                            }
                        })
                        .closeHandler(frame -> {
                            log.trace("Client disconnected");
                            clients.remove(frame.session());
                        })
                        .subscribeHandler(new DefaultSubscribeHandler() {
                            @Override
                            public void handle(ServerFrame serverFrame) {
                                super.handle(serverFrame);
                                final String destValue = serverFrame.frame().getDestination();
                                final Destination dest = serverFrame.connection().handler().getDestination(destValue);
                                final Map<String, Destination> destinations = destinationsMap.computeIfAbsent(destValue, k -> new HashMap<>());
                                destinations.put(serverFrame.connection().session(), dest);
                            }
                        })
                        .receivedFrameHandler(frame -> {
                            final SubscriptionCallback wsConsumer = wsSubscriptions.get(frame.frame().getDestination());
                            if (wsConsumer != null)
                                wsConsumer.onMessage(this, frame.connection(),
                                        frame.frame());
                            else log.debug("Destination not found");
                        }));

    }

    public void subscribe(String destination, SubscriptionCallback messageConsumer) {
        wsSubscriptions.put(destination, messageConsumer);
    }

    public void send(String session, String destination, String message) {
        final Map<String, Destination> destinations = destinationsMap.get(destination);

        if (destinations == null) {
            log.debug("Destination not found");
            return;
        }

        final StompServerConnection connection = clients.get(session);
        if (connection == null) {
            log.debug("Connection not found");
            return;
        }

        final Destination dest = destinations.get(session);
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

    public void broadcast(String destination, String message) {
        final Map<String, Destination> destinations = destinationsMap.get(destination);

        if (destinations == null) {
            log.debug("Destination not found");
            return;
        }

        final Frame frame = new Frame()
                .setDestination(destination)
                .setCommand(Command.MESSAGE)
                .setBody(Buffer.buffer(message));

        clients.forEach((session, connection) -> {
            try {
                final Destination dest = destinations.get(session);
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

    public HttpServer initialize(HttpServer server) {
        return server.webSocketHandshakeHandler(stompServer.webSocketHandshakeHandler())
                .webSocketHandler(stompServer.webSocketHandler());
    }
}
