package com.rarible.flow.scanner.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("flow-api")
data class FlowListenerProperties(
    val scannerLogRecordDaemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    val scannerLogRecordListeners: Map<String, Int> = emptyMap(),
    val cleanup: CleanUpProperties = CleanUpProperties(),
    val startEndWorker: StartEndWorkerProperties = StartEndWorkerProperties(),
    val flowty: FlowtyProperties = FlowtyProperties(),
)

data class CleanUpProperties(
    val enabled: Boolean = false,
    val batchSize: Int = 100,
)

data class StartEndWorkerProperties(
    val enabled: Boolean = true,
    val pollingPeriod: Duration = Duration.ofMinutes(1),
    val errorDelay: Duration = Duration.ofMinutes(2)
)

data class FlowtyProperties(
    val enabled: Boolean = false,
    val endpoint: String = "https://api2.flowty.io",
    val proxy: String? = null,
)
