package io.github.tfkfan.stomp.impl;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.stomp.StompWebsocketAdapter;
import io.github.tfkfan.stomp.StompMessageConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.stomp.*;
import io.vertx.ext.stomp.impl.Topic;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class StompWebsocketAdapterImpl implements StompWebsocketAdapter {
    private final StompServer stompServer;
    private final Map<String, Set<String>> connectionToSessionMap = new HashMap<>(1000);
    private final Map<String, StompMessageConsumer> serverSubscriptions = new HashMap<>();
    private final Map<String, Map<String, StompServerConnection>> sessionsMap = new HashMap<>(1000);

    public StompWebsocketAdapterImpl(Vertx vertx, JsonObject cnf, String wsPath) {
        final List<String> idHeaders = Arrays.asList(cnf.getString(Constants.STOMP_ID_HEADERS, "email").split(Constants.LINE_DIVIDER));

        stompServer = StompServer.create(vertx, new StompServerOptions()
                        .setPort(-1)
                        .setWebsocketBridge(true)
                        .setWebsocketPath(wsPath))
                .handler(StompServerHandler.create(vertx)
                        .destinationFactory(Topic::new)
                        .connectHandler(new DefaultConnectHandler() {
                            @Override
                            public void handle(ServerFrame frame) {
                                final String webSocketSessionId = frame.connection().session();
                                final String id = buildId(idHeaders, frame.frame().getHeaders());
                                log.debug("New client connected: {}/{}", id, frame.connection().session());
                                super.handle(frame);
                                putConnection(id, webSocketSessionId, frame.connection());
                            }
                        })
                        .closeHandler(this::removeConnectionsByWebSocketId)
                        .receivedFrameHandler(frame -> {
                            try {
                                final StompMessageConsumer wsConsumer = serverSubscriptions.get(frame.frame().getDestination());
                                if (wsConsumer != null) {
                                    final String wsId = frame.connection().session();
                                    final String id = buildId(idHeaders, frame.frame().getHeaders());
                                    log.trace("Consumer found for: {}", frame.frame().getDestination());
                                    if (id == null) {
                                        log.trace("Session id not found for: {}", frame.frame().getDestination());
                                        return;
                                    }
                                    wsConsumer.onMessage(wsId, id, frame);
                                }
                            } catch (Exception e) {
                                frame.connection().close();
                                throw new RuntimeException(e);
                            }
                        }));
    }

    @Override
    public MeterBinder meterBinder() {
        return meterRegistry -> {
            Gauge.builder("ws.sessions.count", sessionsMap::size)
                    .description("websocket sessions count")
                    .strongReference(true)
                    .register(meterRegistry);
            Gauge.builder("ws.stomp.subscriptions.count", serverSubscriptions::size)
                    .description("websocket stomp subscriptions count")
                    .strongReference(true)
                    .register(meterRegistry);
        };
    }

    @Override
    public void subscribe(String destination, StompMessageConsumer messageConsumer) {
        serverSubscriptions.put(destination, messageConsumer);
    }

    protected String buildId(List<String> idHeaders, Map<String, String> headers) {
        return headers.entrySet().stream()
                .filter(it -> idHeaders.contains(it.getKey()))
                .map(Map.Entry::getValue)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(Constants.HASH));
    }

    private StompServerConnection getConnection(String id, String webSocketSessionId) {
        final Map<String, StompServerConnection> sessions = sessionsMap.get(id);
        if (sessions == null)
            return null;
        if (!sessions.isEmpty() && webSocketSessionId == null)
            return sessions.values().iterator().next();
        return sessions.get(webSocketSessionId);
    }

    private void removeConnectionsByWebSocketId(StompServerConnection connection) {
        final Set<String> sessionIds = connectionToSessionMap.get(connection.session());
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.warn("Session Id not found for ws id: {}", connection.session());
            return;
        }
        sessionIds.forEach(sessionId -> {
            log.debug("Client disconnected: {}", sessionId);
            sessionsMap.remove(sessionId);
        });
    }

    private void putConnection(String id, String webSocketSessionId, StompServerConnection connection) {
        final Map<String, StompServerConnection> connections = sessionsMap.containsKey(id) ? sessionsMap.get(id) : new HashMap<>(2);
        connections.put(webSocketSessionId, connection);
        sessionsMap.put(id, connections);

        final Set<String> sessionIds = connectionToSessionMap.containsKey(id) ?
                connectionToSessionMap.get(id) : new HashSet<>(2);
        sessionIds.add(id);
        connectionToSessionMap.put(webSocketSessionId, sessionIds);
    }

    @Override
    public void send(String id, String destination, String message) {
        send(id, null, destination, message);
    }

    @Override
    public void send(String id, String webSocketSessionId, String destination, Object message) {
        final StompServerConnection connection = getConnection(id, webSocketSessionId);
        if (connection == null) {
            log.debug("Connection not found");
            return;
        }

        final Destination dest = getDestination(destination);
        if (dest == null) {
            log.debug("Destination not found for id {}", id);
            return;
        }

        dispatch(dest, connection, message);
    }

    private Destination getDestination(String destination) {
        return stompServer.stompHandler().getDestination(destination);
    }

    protected void dispatch(Destination destination, StompServerConnection connection, Object message) {
        try {
            final Buffer buff = Buffer.buffer();
            buff.appendString(message instanceof String ? message.toString() :
                    DatabindCodec.mapper().writeValueAsString(message));

            final Frame frame = new Frame()
                    .setDestination(destination.destination())
                    .setCommand(Command.MESSAGE)
                    .setBody(buff);
            destination.dispatch(connection, frame);
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
