package io.github.tfkfan.verticle;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.stomp.StompWebsocketAdapter;
import io.github.tfkfan.stomp.impl.StompWebsocketAdapterImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Arrays;
import java.util.Map;

@Slf4j
public final class WebsocketGatewayVerticle extends AbstractVerticle {
    private HttpServer httpServer;
    private StompWebsocketAdapter srv;

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            final EventBus eventBus = vertx.eventBus();
            srv = new StompWebsocketAdapterImpl(vertx, Constants.WEBSOCKET_PATH);
            eventBus.<String>consumer(Constants.VERTX_WS_BROADCAST_CHANNEL, message -> {
                log.info("Message from client: {}", message.body());
                srv.broadcast("/example_output", message.body() + " - BROADCAST");
            });
            srv.subscribe("/example_input", (frame) -> {
                //Publish reply to all vertx instances with cluster manager
                eventBus.publish(Constants.VERTX_WS_BROADCAST_CHANNEL, frame.frame().getBodyAsString());
            });

           /*
           final JsonObject kafkaProps = config.getJsonObject(Constants.KAFKA_PROP);
           final KafkaProducer<String, String> kafkaProducer = kafkaProducer(vertx, config, kafkaProps.getString(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP));
        final KafkaConsumer<String, String> kafkaConsumer = kafkaConsumer(vertx, config, kafkaProps.getString(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP)).handler(record -> {
            eventBus.publish(Constants.VERTX_WS_BROADCAST_CHANNEL, record.value());
        });*/

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

    private KafkaConsumer<String, String> kafkaConsumer(Vertx vertx, JsonObject cnf, String bootstrapServers) {
        return KafkaConsumer.create(vertx, Map.of(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP,
                cnf.getString(Constants.KAFKA_BOOTSTRAPSERVERS_ENV, bootstrapServers),
                Constants.KAFKA_KEY_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_VALUE_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_GROUP_ID_PROP, "my_group",
                Constants.KAFKA_AUTO_OFFSET_RESET_PROP, "earliest",
                Constants.KAFKA_AUTO_COMMIT_PROP, "true"));
    }

    private KafkaProducer<String, String> kafkaProducer(Vertx vertx, JsonObject cnf, String bootstrapServers) {
        return KafkaProducer.create(vertx, Map.of(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP,
                cnf.getString(Constants.KAFKA_BOOTSTRAPSERVERS_ENV, bootstrapServers),
                Constants.KAFKA_KEY_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_VALUE_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_ACKS_PROP, "1"));
    }
}
