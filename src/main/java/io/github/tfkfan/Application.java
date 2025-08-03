package io.github.tfkfan;

import io.github.tfkfan.config.Constants;
import io.github.tfkfan.metrics.MonitorableVertx;
import io.github.tfkfan.verticle.WebsocketGatewayVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

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
        new MonitorableVertx().build()
                .flatMap(vertx -> loadConfig(vertx)
                        .flatMap(config -> vertx.deployVerticle(WebsocketGatewayVerticle.class,
                                new DeploymentOptions().setConfig(config))
                        ))
                .onFailure(throwable -> startupErrorHandler(Vertx.currentContext().owner(), throwable));
    }
}
