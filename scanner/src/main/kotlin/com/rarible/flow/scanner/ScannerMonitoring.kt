package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowBlock
import com.rarible.blockchain.scanner.framework.model.Block
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.flow.core.repository.ExtendedFlowBlockRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.math.max

@Component
class ScannerMonitoring(
    meter: MeterRegistry,
    val blockRepository: ExtendedFlowBlockRepository
): SequentialDaemonWorker(meter, DaemonWorkerProperties(), "flow-scanner-monitor") {

    @Volatile private var lastSeenBlockHead: FlowBlock? = null
    @Volatile private var firstSeenBlockHead: FlowBlock? = null
    @Volatile private var blocksCount: Long = 0
    @Volatile private var errorBlocksCount: Long = 0
    @Volatile private var pendingBlocksCount: Long = 0

    init {
        registerGauge()
    }

    override suspend fun handle() {
        firstSeenBlockHead = blockRepository.findFirstByOrderByIdAsc().awaitFirstOrNull()
        lastSeenBlockHead = blockRepository.findFirstByOrderByIdDesc().awaitFirstOrNull()
        blocksCount = blockRepository.count().awaitFirst()
        errorBlocksCount = blockRepository.countByStatus(Block.Status.ERROR).awaitFirstOrDefault(0L)
        pendingBlocksCount = blockRepository.countByStatus(Block.Status.PENDING).awaitFirstOrDefault(0L)

        delay(pollingPeriod.toMillis())
    }

    private fun registerGauge() {
        val blockchain = "FLOW"
        
        Gauge.builder("flow.listener.block.delay", this::getBlockDelay)
            .tag("blockchain", blockchain)
            .register(meterRegistry)

        Gauge.builder("flow.listener.block.error") { this.errorBlocksCount.toDouble() }
            .tag("blockchain", blockchain)
            .register(meterRegistry)

        Gauge.builder("flow.listener.block.pending") { pendingBlocksCount.toDouble() }
            .tag("blockchain", blockchain)
            .register(meterRegistry)

        Gauge.builder("flow.listener.block.missing", this::getMissingBlockCount)
            .tag("blockchain", blockchain)
            .register(meterRegistry)
    }

    private fun getMissingBlockCount(): Double {
        val last = (lastSeenBlockHead?.id ?: 0)
        val first = (firstSeenBlockHead?.id ?: 0)
        val count = (blocksCount)
        return (count - (last - first)).toDouble()
    }


    private fun getBlockDelay(): Double? {
        val lastSeenBlockTimestamp = lastSeenBlockHead?.timestamp ?: return null
        val currentTimestamp = Instant.now().toEpochMilli()
        return max(currentTimestamp - lastSeenBlockTimestamp, 0).toDouble()
    }

}