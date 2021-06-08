package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import com.rarible.flow.scanner.repo.RariEventRepository
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Component
class FlowScanner(
    private val blockRepository: FlowBlockRepository,
    private val txRepository: FlowTransactionRepository,
    private val rariEventRepository: RariEventRepository,
    private val kafkaProducer: RaribleKafkaProducer<EventMessage>
) {
    private var latestBlockHeight: Long = 0L

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    @GrpcClient("flow")
    private lateinit var client: AccessAPIGrpc.AccessAPIBlockingStub

    private val log: Logger = LoggerFactory.getLogger(FlowScanner::class.java)

    @PostConstruct
    fun postCreate() {
        calculateLatestBlock()
    }

    @PreDestroy
    fun preDestroy() {
        executorService.shutdown()
    }

    private fun calculateLatestBlock() {

        latestBlockHeight =
            client.getLatestBlockHeader(
                Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()
            ).block.height
    }

    /**
     * Request latest block with 2 sec. delay
     */
    @Scheduled(fixedDelay = 2000L, initialDelay = 2000L)
    fun scan() {
        val current = latestBlockHeight
        latestBlockHeight = client.getLatestBlockHeader(
            Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()
        ).block.height

        readBlocks(current..latestBlockHeight)
    }

    private fun readBlocks(range: LongRange) {
        val tasks = range.map { ReadTask(it, client) }
        val features = executorService.invokeAll(tasks)
        features.map { it.get() }.forEach {
            blockRepository.save(it.first).subscribe()
            if (it.second.isNotEmpty()) {
                txRepository.saveAll(it.second).subscribe { tx ->
                    findRariEvents(tx)
                }
            }
        }
    }

    private fun findRariEvents(tx: FlowTransaction) = runBlocking {
        tx.events.forEachIndexed { index, flowEvent ->
            when {
                flowEvent.type.contains("1cd85950d20f05b2", true) -> {
                    parseAndSend(flowEvent, tx.id, index)
                }
                /*flowEvent.type.contains("9a0766d93b6608b7", true) -> {
                    parseAndSend(flowEvent)
                }
                flowEvent.type.contains("0287aa0a33a82d7a", true) -> {
                    parseAndSend(flowEvent)
                }
                flowEvent.type.contains("9837bf0b0b963a4a", true) -> {
                    parseAndSend(flowEvent)
                }
                flowEvent.type.contains("e3750a9bc4137f3f", true) -> {
                    parseAndSend(flowEvent)
                }
                flowEvent.type.contains("f9a15cf06773248c", true) -> {
                    parseAndSend(flowEvent)
                }*/
            }
        }
    }

    private suspend fun parseAndSend(flowEvent: FlowEvent, txId: String, eventIndex: Int) {
        val obj = ObjectMapper().readTree(flowEvent.data)
        val e = obj["value"]
        val id = e["id"].asText()
        val fields = e.get("fields").asIterable().map {
            val name = it["name"].asText()
            val type = it["value"]["type"].asText()

            val value = if ("Optional" == type) {
                it["value"]["value"]["value"]
            } else {
                it["value"]["value"]
            }
           name to value.textValue()
        }.toMap()
        val m = EventMessage(id, fields)
        log.info("$m")
        kafkaProducer.send(
            KafkaMessage(
                key = "$txId.$eventIndex", //todo check collisions
                value = m,
                headers = emptyMap(),
                id = "$txId.$eventIndex"
            )
        )
    }
}

