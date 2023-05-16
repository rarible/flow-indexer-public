package com.rarible.flow.scanner.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.flow.scanner.config.StartEndWorkerProperties
import com.rarible.flow.scanner.service.order.OrderStartEndCheckerHandler
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.time.delay

@ExperimentalCoroutinesApi
class OrderStartEndCheckerWorker(
    private val handler: OrderStartEndCheckerHandler,
    private val properties: StartEndWorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "start-end-orders-check-job"
) {

    override suspend fun handle() {
        handler.handle()
        delay(properties.pollingPeriod)
    }
}
