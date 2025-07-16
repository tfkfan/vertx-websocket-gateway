package io.github.tfkfan.stomp;

import io.github.tfkfan.config.Constants;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class StompWebsocketServer {
    private final StompServer stompServer;
    private final Set<StompServerConnection> clients = new CopyOnWriteArraySet<>();
    private final Map<String, BiConsumer<StompWebsocketServer, String>> wsSubscriptions = new HashMap<>();

    public StompWebsocketServer(Vertx vertx) {
        stompServer = StompServer.create(vertx, new StompServerOptions()
                        .setPort(-1)
                        .setWebsocketBridge(true)
                        .setWebsocketPath(Constants.WEBSOCKET_PATH))
                .handler(StompServerHandler.create(vertx)
                      /*  .connectHandler(frame -> {
                            log.trace("New client connected");
                            clients.add(frame.connection());
                        })*/
                        .closeHandler(frame -> {
                            log.trace("New client disconnected");
                            clients.remove(frame);
                        })
                        .destinationFactory((v, name) -> {
                            if (name.startsWith("/example_input") || name.startsWith("/example_output"))
                                return Destination.queue(vertx, name);
                            return null;
                        })
                        .sendHandler(frame -> {
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
                .setBody(Buffer.buffer(message))
                .addHeader("broadcast", "true")
                .addHeader("timestamp", String.valueOf(System.currentTimeMillis()));

        clients.forEach(connection -> {
            try {
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
