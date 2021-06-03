package com.rarible.flow.scanner

import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import net.devh.boot.grpc.client.inject.GrpcClient
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
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
    private val txRepository: FlowTransactionRepository
) {
    private var latestBlockHeight: Long = 0L

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    @GrpcClient("flow")
    private lateinit var client: AccessAPIGrpc.AccessAPIBlockingStub

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
        val tasks = range.map { ReadTask(it) }
        val features = executorService.invokeAll(tasks)
        features.map { it.get() }.forEach {
            blockRepository.save(it.first)
            if (it.second.isNotEmpty()) {
                txRepository.saveAll(it.second)
            }
        }
    }
}

