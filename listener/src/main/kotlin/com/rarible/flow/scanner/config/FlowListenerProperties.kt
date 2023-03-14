package com.rarible.flow.scanner.config

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("flow-api")
data class FlowListenerProperties(
    val flowAccessUrl: String,
    val flowAccessPort: Int,
    val chainId: FlowChainId,
    val serviceAccount: FlowNetworkServiceAccount = FlowNetworkServiceAccount(FlowAddress("0x00"), ""),
    val scannerLogRecordDaemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    val scannerLogRecordListeners: Map<String, Int> = emptyMap(),
    val cleanup: CleanUpProperties = CleanUpProperties(),
    val startEndWorker: StartEndWorkerProperties = StartEndWorkerProperties()
)

data class FlowNetworkServiceAccount(
    val address: FlowAddress,
    val privateKey: String
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

