package io.github.tfkfan.stomp;

import io.github.tfkfan.config.Constants;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class StompWebsocketServer {
    private final StompServer stompServer;
    private final Set<StompServerConnection> clients = new HashSet<>();
    private final Map<String, Map<String, String>> subscriptionsMap = new HashMap<>();
    private final Map<String, BiConsumer<StompWebsocketServer, String>> wsSubscriptions = new HashMap<>();

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
                                clients.add(frame.connection());
                            }
                        })
                        .closeHandler(frame -> {
                            log.trace("New client disconnected");
                            clients.remove(frame);
                            subscriptionsMap.remove(frame.session());
                        })
                        .destinationFactory((v, name) -> (name.startsWith("/example_input") || name.startsWith("/example_output")) ?
                                Destination.topic(vertx, name) : null)
                        .subscribeHandler(frame -> {
                            log.trace("New client subscribed");
                            Map<String, String> subscriptions = subscriptionsMap.get(frame.connection().session());
                            if (subscriptions == null)
                                subscriptions = new HashMap<>();
                            subscriptions.put(frame.frame().getDestination(), frame.frame().getHeader("id"));
                            subscriptionsMap.put(frame.connection().session(), subscriptions);
                        })
                        .receivedFrameHandler(frame -> {
                            final BiConsumer<StompWebsocketServer, String> wsConsumer = wsSubscriptions.get(frame.frame().getDestination());
                            if (wsConsumer != null)
                                wsConsumer.accept(this, frame.frame().getBodyAsString());
                            else log.debug("Destination not found");
                        }));

    }

    public void subscribe(String destination, BiConsumer<StompWebsocketServer, String> messageConsumer) {
        wsSubscriptions.put(destination, messageConsumer);
    }

    public void broadcast(String destination, String message) {
        final Frame broadcastFrame = new Frame()
                .setDestination(destination)
                .setCommand(Command.MESSAGE)
                .setBody(Buffer.buffer(message));

        clients.forEach((connection) -> {
            try {
                final String subscriptionId = subscriptionsMap.get(connection.session())
                        .get(destination);

                broadcastFrame.setHeaders(Map.of("subscription", subscriptionId));
                connection.write(broadcastFrame);
            } catch (Exception e) {
                log.warn("Error broadcasting frame to client: {}", e.getMessage());
            }
        });
    }

    public StompServer getStompServer() {
        return stompServer;
    }
}
