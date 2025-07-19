package io.github.tfkfan.verticle;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.kafka.GatewayKafkaConsumer;
import io.github.tfkfan.kafka.GatewayKafkaProducer;
import io.github.tfkfan.kafka.GatewayMessage;
import io.github.tfkfan.stomp.StompWebsocketAdapter;
import io.github.tfkfan.stomp.impl.StompWebsocketAdapterImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public final class WebsocketGatewayVerticle extends AbstractVerticle {
    private HttpServer httpServer;
    private StompWebsocketAdapter srv;
    private GatewayKafkaConsumer kafkaConsumer;
    private GatewayKafkaProducer kafkaProducer;

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            final EventBus eventBus = vertx.eventBus();
            final JsonObject config = config();
            final JsonObject kafkaProps = config.getJsonObject(Constants.KAFKA_PROP);
            srv = new StompWebsocketAdapterImpl(vertx, Constants.WEBSOCKET_PATH);
            kafkaConsumer = new GatewayKafkaConsumer(vertx, config, kafkaProps.getString(Constants.BOOTSTRAP_SERVERS_PROP));
            kafkaProducer = new GatewayKafkaProducer(vertx, config, kafkaProps.getString(Constants.BOOTSTRAP_SERVERS_PROP));

            kafkaConsumer.subscribe("websocket_gateway_output_topic", record -> {
                final GatewayMessage message = record.value();
                /*
                Publish reply to all vertx instances with cluster manager.
                Only one node contain required ws session, so all nodes should process the message
                 */
                eventBus.publish(Constants.VERTX_WS_BROADCAST_CHANNEL, JsonObject.mapFrom(message));
            });
            eventBus.<JsonObject>consumer(Constants.VERTX_WS_BROADCAST_CHANNEL, message -> {
                final GatewayMessage msg = message.body().mapTo(GatewayMessage.class);
                log.info("Message from: {}", msg.getSender());
                srv.send(msg.getRecipient(), msg.getStompChannel(), msg.getMessage() + " - BROADCAST");
            });
            srv.subscribe("/example_input", (frame) -> kafkaProducer.send("websocket_gateway_input_topic", frame.frame().getBodyAsString()));

            buildHttpServer(buildRouter(), config().getJsonObject(Constants.SERVER_PROP).getInteger(Constants.PORT_PROP))
                    .onSuccess(srv -> log.info("Server started at {}", srv.actualPort()))
                    .onFailure(startPromise::fail);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            startPromise.fail(e);
        }
    }

    private Router buildRouter() {
        final Router router = Router.router(vertx);
        router.route().handler(StaticHandler.create(Constants.STATIC_FOLDER_PATH));
        router.get(Constants.HEALTH_PATH).handler(HealthCheckHandler.create(vertx));
        router.get(Constants.READINESS_PATH).handler(HealthCheckHandler.create(vertx));
        return router;
    }

    private Future<HttpServer> buildHttpServer(Router router, int port) {
        httpServer = vertx.createHttpServer(buildHttpServerOptions());
        return srv.initialize(httpServer)
                .requestHandler(router)
                .exceptionHandler(throwable -> log.error("Internal server error", throwable))
                .listen(port);
    }

    private HttpServerOptions buildHttpServerOptions() {
        return new HttpServerOptions().setWebSocketSubProtocols(Arrays.asList("v10.stomp", "v11.stomp"));
    }
}
