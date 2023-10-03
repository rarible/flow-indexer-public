package com.rarible.flow.scanner.config

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.flow.core.config.AppProperties
import com.rarible.protocol.order.listener.metric.OrderMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsCountersConfiguration(
    private val appProperties: AppProperties,
    private val meterRegistry: MeterRegistry
) {

    @Bean
    fun orderStartedMetric(): RegisteredCounter {
        return OrderMetric(appProperties.metricRootPath, "started").bind(meterRegistry)
    }

    @Bean
    fun orderExpiredMetric(): RegisteredCounter {
        return OrderMetric(appProperties.metricRootPath, "expired").bind(meterRegistry)
    }
}
