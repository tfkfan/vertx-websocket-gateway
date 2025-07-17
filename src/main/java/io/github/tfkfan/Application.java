package io.github.tfkfan;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.stomp.StompWebsocketServer;
import io.github.tfkfan.stomp.impl.StompWebsocketServerImpl;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
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
public class Application {
    public static Future<JsonObject> loadConfig(Vertx vertx) {
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions()
                        .setType(Constants.FILE)
                        .setFormat(Constants.YAML)
                        .setConfig(new JsonObject().put(Constants.PATH, "src/main/resources/application.yaml")))
                .addStore(new ConfigStoreOptions()
                        .setType(Constants.ENV));
        return ConfigRetriever.create(vertx, options).getConfig();
    }

    public static void startupErrorHandler(Vertx vertx, Throwable e) {
        if (vertx != null)
            vertx.close();
        log.error(e.getMessage(), e);
        throw new RuntimeException(e);
    }

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions())
                .flatMap(vertx -> loadConfig(vertx)
                        .flatMap(config -> {
                            final EventBus eventBus = vertx.eventBus();
                            final StompWebsocketServer srv = new StompWebsocketServerImpl(vertx);

                            eventBus.<String>consumer(Constants.VERTX_WS_BROADCAST_CHANNEL, message -> {
                                log.info("Message from client: {}", message.body());
                                srv.broadcast("/example_output", message.body() + " - BROADCAST");
                            });
                            srv.subscribe("/example_input", (frame) -> {
                                //Publish reply to all vertx instances with cluster manager
                                eventBus.publish(Constants.VERTX_WS_BROADCAST_CHANNEL, frame.frame().getBodyAsString());
                            });

                            final Router router = Router.router(vertx);
                            router.route().handler(StaticHandler.create(Constants.STATIC_FOLDER_PATH));
                            router.get(Constants.HEALTH_PATH).handler(HealthCheckHandler.create(vertx));
                            router.get(Constants.READINESS_PATH).handler(HealthCheckHandler.create(vertx));

                            return srv.initialize(
                                            vertx.createHttpServer(new HttpServerOptions().setWebSocketSubProtocols(Arrays.asList("v10.stomp", "v11.stomp")))
                                    )
                                    .requestHandler(router)
                                    .exceptionHandler(throwable -> log.error("Internal server error", throwable))
                                    .listen(8080);
                        }))
                .onSuccess(srv -> log.info("Server started at {}", srv.actualPort()))
                .onFailure(throwable -> startupErrorHandler(Vertx.currentContext().owner(), throwable));
    }

    private static KafkaConsumer<String, String> kafkaConsumer(Vertx vertx, JsonObject cnf, String bootstrapServers) {
        return KafkaConsumer.create(vertx, Map.of(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP,
                cnf.getString(Constants.KAFKA_BOOTSTRAPSERVERS_ENV, bootstrapServers),
                Constants.KAFKA_KEY_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_VALUE_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_GROUP_ID_PROP, "my_group",
                Constants.KAFKA_AUTO_OFFSET_RESET_PROP, "earliest",
                Constants.KAFKA_AUTO_COMMIT_PROP, "true"));
    }

    private static KafkaProducer<String, String> kafkaProducer(Vertx vertx, JsonObject cnf, String bootstrapServers) {
        return KafkaProducer.create(vertx, Map.of(Constants.KAFKA_BOOTSTRAP_SERVERS_PROP,
                cnf.getString(Constants.KAFKA_BOOTSTRAPSERVERS_ENV, bootstrapServers),
                Constants.KAFKA_KEY_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_VALUE_SERIALIZER_PROP, StringSerializer.class.getName(),
                Constants.KAFKA_ACKS_PROP, "1"));
    }
}
