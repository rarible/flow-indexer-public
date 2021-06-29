package com.rarible.flow.scanner

import com.rarible.flow.scanner.repo.FlowBlockRepository
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.annotation.PostConstruct

/**
 * Created by TimochkinEA at 15.06.2021
 *
 * Block's range watcher. Need to check if we have "holes" on long progression of block height
 */
@Component
@Profile("testnet")
class FlowBlockRangeWatch(
    private val blockRepository: FlowBlockRepository,
    private val blockProcessor: ReactiveBlockProcessor
)
{

    private val from: Long = 36290422L //TODO брать из конфига

    @Autowired
    @Qualifier("flowClient")
    private lateinit var client: AccessAPIGrpc.AccessAPIBlockingStub

    @Volatile
    private var lastRead: Long = 0L

    @PostConstruct
    private fun postCreate() {
        val lastBlockInDB = blockRepository.findTopByOrderByHeightDesc().block()
        val lastBlockOnChain = client.getLatestBlockHeader(Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()).block

        lastRead = if (lastBlockInDB != null) {
            val ld = lastBlockInDB.timestamp
            val bld = Instant.ofEpochSecond(lastBlockOnChain.timestamp.seconds)
            val diff = ChronoUnit.SECONDS.between(ld, bld)
            if (diff > 60L)  {
                lastBlockOnChain.height - 1
            } else {
                lastBlockInDB.height
            }
        } else {
            lastBlockOnChain.height - 1
        }
    }

    @Scheduled(fixedDelay = 1000L)
    private fun forwardRead() {
        val onChain = client.getLatestBlockHeader(Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()).block.height
        doRead(lastRead, onChain)
        lastRead = onChain
    }

    @Scheduled(fixedDelay = 60_000L, initialDelay = 20_000L)
    private fun start() {
        val last = blockRepository.maxHeight().blockOptional().orElse(
            client.getLatestBlockHeader(Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()).block.height
        )
        LongRange(from, last).reversed().chunked(1000) {
            return@chunked Pair(it.last(), it.first())
        }.forEach {
            doRead(it.first, it.second)
        }
    }

    fun doRead(from: Long, to: Long) {
        val range = LongRange(from, to)
        val have = blockRepository.heightsBetween(range.first, range.last).map { it.height }.collectList().blockOptional().orElse(
            listOf()
        )
        val heights = range.minus(have).toList()
        if (heights.isNotEmpty()) {
            blockProcessor.doRead(heights)
        }
    }

}
