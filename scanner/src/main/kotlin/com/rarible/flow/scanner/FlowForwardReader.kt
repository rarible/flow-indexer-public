package com.rarible.flow.scanner

import com.rarible.flow.scanner.repo.FlowBlockRepository
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.asLocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Created by TimochkinEA at 15.06.2021
 *
 * Read Flow network forward
 */
@Component
class FlowForwardReader(
    private val blockRepository: FlowBlockRepository,
    private val blockProcessor: ReactiveBlockProcessor
) {

    @Autowired
    @Qualifier("flowClient")
    private lateinit var client: AccessAPIGrpc.AccessAPIBlockingStub

    @Volatile
    private var lastRead: Long = 0L

    @EventListener(ApplicationReadyEvent::class)
    fun doWork() {
        val lastBlockInDB = blockRepository.findTopByOrderByHeightDesc().block()
        val lastBlockOnChain = client.getLatestBlockHeader(Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()).block

        lastRead = if (lastBlockInDB != null) {
            val ld = lastBlockInDB.timestamp
            val bld = lastBlockOnChain.timestamp.asLocalDateTime()
            val diff = ChronoUnit.SECONDS.between(ld, bld)
            if (diff > 60L)  {
                lastBlockOnChain.height - 1
            } else {
                lastBlockInDB.height
            }
        } else {
            lastBlockOnChain.height - 1
        }
        forwardRead()
    }

    private fun forwardRead() {
        Flux.interval(Duration.ofSeconds(1L), Schedulers.newParallel("${FlowForwardReader::class}")).
        subscribe {
            val onChain = client.getLatestBlockHeader(Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()).block.height
            doRead(lastRead, onChain)
            lastRead = onChain
        }

    }

    fun doRead(from: Long, to: Long) {
        val range = LongRange(from, to)
        blockRepository.heightsBetween(range.first, range.last).map { it.height }.collectList().subscribe {
            val heights = range.minus(it).toList()
            if (heights.isNotEmpty()) {
                blockProcessor.doRead(client, heights)
            }
        }
    }

}
