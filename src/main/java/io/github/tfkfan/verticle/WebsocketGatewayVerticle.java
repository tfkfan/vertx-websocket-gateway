package io.github.tfkfan.verticle;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.kafka.GatewayKafkaConsumer;
import io.github.tfkfan.kafka.GatewayKafkaProducer;
import io.github.tfkfan.kafka.message.GatewayOutputMessage;
import io.github.tfkfan.metrics.MonitorEndpoint;
import io.github.tfkfan.stomp.StompWebsocketAdapter;
import io.github.tfkfan.stomp.impl.StompWebsocketAdapterImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public final class WebsocketGatewayVerticle extends AbstractVerticle {
    private HttpServer httpServer;
    private StompWebsocketAdapter srv;
    private GatewayKafkaConsumer kafkaConsumer;
    private GatewayKafkaProducer kafkaProducer;

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            final JsonObject kafkaProps = config().getJsonObject(Constants.KAFKA_PROP);
            final Map<String, String> appInputMapping = config()
                    .getJsonArray(Constants.APP_INPUT_MAPPING_ENV, new JsonArray().add("%s:%s".formatted(Constants.STOMP_DEFAULT_INPUT_CHANNEL, Constants.KAFKA_DEFAULT_INPUT_TOPIC)))
                    .stream()
                    .filter(it -> it instanceof String)
                    .map(it -> ((String) it).split(Constants.DIVIDER))
                    .collect(Collectors.toMap(it -> it[0], it -> it[1]));
            srv = new StompWebsocketAdapterImpl(vertx, Constants.WEBSOCKET_PATH);
            kafkaConsumer = new GatewayKafkaConsumer(vertx, config(), kafkaProps.getString(Constants.BOOTSTRAP_SERVERS_PROP));
            kafkaProducer = new GatewayKafkaProducer(vertx, config(), kafkaProps.getString(Constants.BOOTSTRAP_SERVERS_PROP));

            kafkaConsumer.subscribe(Constants.KAFKA_WEBSOCKET_OUTPUT_TOPIC, record -> {
                /*
                Publish reply to all vertx instances with cluster manager.
                Only one node contain required ws session, so all nodes should process the message.
                Another option - create dynamic consumers per session and use 'eventBus.send' method
                 */
                vertx.eventBus().publish(Constants.VERTX_WS_BROADCAST_CHANNEL, JsonObject.mapFrom(record.value()));
            });
            vertx.eventBus().<JsonObject>consumer(Constants.VERTX_WS_BROADCAST_CHANNEL, message -> {
                final GatewayOutputMessage msg = message.body().mapTo(GatewayOutputMessage.class);
                log.info("Message from: {}", msg.getSender());
                srv.send(msg.getRecipient(), msg.getStompChannel(), msg.getMessage());
            });
            appInputMapping.forEach((key, value) -> {
                srv.subscribe(key, (frame) -> kafkaProducer.send(value, frame.frame().getBodyAsString()));
                log.info("Application mapping stomp {} to kafka {} has been set", key, value);
            });
            srv.meterBinder().bindTo(BackendRegistries.getDefaultNow());
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
        new MonitorEndpoint().create(router);
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
