package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import com.rarible.flow.scanner.repo.RariEventRepository
import net.devh.boot.grpc.client.inject.GrpcClient
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Component
class FlowScanner(
    private val blockRepository: FlowBlockRepository,
    private val txRepository: FlowTransactionRepository,
    private val rariEventRepository: RariEventRepository
) {
    private var latestBlockHeight: Long = 0L

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

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
                txRepository.saveAll(it.second).subscribe {
                    findRariEvents(it)
                }
            }
        }
    }

    private fun findRariEvents(tx: FlowTransaction) {
        tx.events.forEach { flowEvent ->
            when {
                flowEvent.type.contains("1cd85950d20f05b2", true) -> {
                    parseAndSend(flowEvent)
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

    private fun parseAndSend(flowEvent: FlowEvent) {
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
           name to value
        }.toMap()
        val m = EventMessage(id, fields)
        log.info("$m")
        // TODO send to kafka
    }
}

data class EventMessage(
    val id: String,
    val fields: Map<String, Any?>
)

