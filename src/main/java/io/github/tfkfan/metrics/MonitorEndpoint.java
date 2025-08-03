package io.github.tfkfan.metrics;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.micrometer.MetricsService;
import io.vertx.micrometer.PrometheusScrapingHandler;

public class MonitorEndpoint {
    private final CorsHandler corsHandler;

    public MonitorEndpoint() {
        this(null);
    }

    public MonitorEndpoint(CorsHandler corsHandler) {
        this.corsHandler = corsHandler;
    }

    protected void wrapCall(RoutingContext context, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            context.fail(throwable);
        }
    }

    public void create(Router router) {
        if (corsHandler != null)
            router.route().handler(corsHandler);
        final MetricsService metricsService = MetricsService.create(Vertx.currentContext().owner());
        router.route("/prometheus").handler(PrometheusScrapingHandler.create());
        router.route("/metrics").handler(rc -> rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .end(metricsService.getMetricsSnapshot().encode()));
    }
}
