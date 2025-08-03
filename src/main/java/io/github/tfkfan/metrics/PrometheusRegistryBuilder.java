package io.github.tfkfan.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

public class PrometheusRegistryBuilder {
    private MeterFilter meterFilter;

    public PrometheusRegistryBuilder withMeterFilter(MeterFilter meterFilter) {
        this.meterFilter = meterFilter;
        return this;
    }

    public PrometheusMeterRegistry build() {
        final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        if (meterFilter == null)
            meterFilter = new MeterFilter() {
                @Override
                public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                    return DistributionStatisticConfig.builder().percentilesHistogram(true).build().merge(config);
                }
            };
        registry.config().meterFilter(meterFilter);
        return registry;
    }
}
