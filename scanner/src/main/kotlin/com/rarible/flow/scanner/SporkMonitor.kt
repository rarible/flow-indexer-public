package com.rarible.flow.scanner

import com.rarible.flow.log.Log
import com.rarible.flow.scanner.repo.FlowBlockRepository
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.runBlocking
import org.onflow.sdk.FlowAccessApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDateTime

/**
 *
 * Monitoring spork
 */
class SporkMonitor(
    private val sporkInfo: SporkInfo,
    private val flowApi: FlowAccessApi
) {


    @Autowired
    private lateinit var blockRepository: FlowBlockRepository

    @Autowired
    private lateinit var processor: ReactiveBlockProcessor


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
                            flowApi.getLatestBlock(true).height
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
                processor.doRead(heights)
            }
        }
    }

    companion object {
        private val log by Log()
    }
}
