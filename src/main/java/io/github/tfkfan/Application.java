package io.github.tfkfan;

import com.sun.net.httpserver.HttpServer;
import io.github.tfkfan.config.Constants;
import io.github.tfkfan.stomp.StompWebsocketServer;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.stomp.Destination;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerHandler;
import io.vertx.ext.stomp.StompServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class Application {
    public static Future<JsonObject> loadConfig(Vertx vertx) {
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions()
                        .setType("file")
                        .setFormat("yaml")
                        .setConfig(new JsonObject().put("path", "src/main/resources/application.yaml")))
                .addStore(new ConfigStoreOptions()
                        .setType("env"));
        return ConfigRetriever.create(vertx, options).getConfig();
    }

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions())
                .flatMap(vertx -> loadConfig(vertx)
                        .flatMap(config -> {
                            final StompWebsocketServer stompWebsocketServer = new StompWebsocketServer(vertx);

                            stompWebsocketServer.subscribe("/example_input", (srv, msg) -> {
                                log.info("MSG: {}", msg);
                                srv.broadcast("/example_output", msg);
                            });

                            final Router router = Router.router(vertx);
                            router.route().handler(StaticHandler.create(Constants.STATIC_FOLDER_PATH));
                            router.get(Constants.HEALTH_PATH).handler(HealthCheckHandler.create(vertx));
                            router.get(Constants.READINESS_PATH).handler(HealthCheckHandler.create(vertx));

                            return vertx.createHttpServer(new HttpServerOptions().setWebSocketSubProtocols(Arrays.asList("v10.stomp", "v11.stomp")))
                                    .requestHandler(router)
                                    .webSocketHandshakeHandler(stompWebsocketServer.getStompServer().webSocketHandshakeHandler())
                                    .webSocketHandler(stompWebsocketServer.getStompServer().webSocketHandler())
                                    .exceptionHandler(throwable -> {
                                        log.error("Internal server error", throwable);
                                    })
                                    .listen(8080);
                        }))
                .onSuccess(srv -> log.info("Server started at {}", srv.actualPort()));
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
