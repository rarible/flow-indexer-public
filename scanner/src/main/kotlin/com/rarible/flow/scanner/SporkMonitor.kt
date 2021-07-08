package com.rarible.flow.scanner

import com.rarible.flow.log.Log
import com.rarible.flow.scanner.repo.FlowBlockRepository
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.runBlocking
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDateTime
import javax.annotation.PostConstruct

/**
 * Created by TimochkinEA at 01.07.2021
 *
 * Monitoring spork
 */
class SporkMonitor(private val sporkInfo: SporkInfo) {

    private val client: AccessAPIGrpc.AccessAPIBlockingStub

    @Autowired
    private lateinit var blockRepository: FlowBlockRepository

    @Autowired
    private lateinit var processor: ReactiveBlockProcessor

    private val log by Log()

    init {
        val channel = ManagedChannelBuilder.forTarget(sporkInfo.nodeUrl).usePlaintext().build()
        client = AccessAPIGrpc.newBlockingStub(channel)
    }


    @EventListener(ApplicationReadyEvent::class)
    fun monitor() {
        Flux.interval(Duration.ZERO, Duration.ofMinutes(1L), Schedulers.newParallel(sporkInfo.name)).retry(5L)
            .subscribe {
                log.info("Start watch ${sporkInfo.name} at ${LocalDateTime.now()}")
                val from = sporkInfo.firstBlockHeight
                val to = if (sporkInfo.lastBlockHeight > 0) {
                    sporkInfo.lastBlockHeight
                } else {
                    runBlocking {
                        blockRepository.maxHeight().awaitFirstOrElse {
                            client.getLatestBlockHeader(
                                Access.GetLatestBlockHeaderRequest.newBuilder()
                                    .setIsSealed(true).build()
                            ).block.height

                        }
                    }

                }
                LongRange(from, to).reversed().chunked(1000).map {
                    Pair(it.last(), it.first())
                }.forEach {
                    doRead(it.first, it.second)
                }

                log.info("Stop watch ${sporkInfo.name} at ${LocalDateTime.now()}")

            }
    }

    private fun doRead(from: Long, to: Long) {
        val range = LongRange(from, to)
        blockRepository.heightsBetween(range.first, range.last).map { it.height }.collectList().subscribe {
            val heights = range.minus(it).toList()
            if (heights.isNotEmpty()) {
                processor.doRead(client, heights)
            }
        }
    }
}
