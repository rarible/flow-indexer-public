package com.rarible.flow.scanner

import com.rarible.flow.scanner.events.*
import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.repo.FlowBlockRepository
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.client.inject.GrpcClient
import org.bouncycastle.util.encoders.Hex
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import javax.annotation.PostConstruct

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Component
class FlowScanner(
    private val publisher: ApplicationEventPublisher,
    private val blockRepository: FlowBlockRepository,
    private val messageTemplate: SimpMessagingTemplate
) {

    private val log: Logger = LoggerFactory.getLogger(FlowScanner::class.java)

    @Volatile
    private var latestBlockHeight: Long = 0L

    @GrpcClient("flow")
    private lateinit var client: AccessAPIGrpc.AccessAPIStub

    @PostConstruct
    fun postCreate() {
        blockRepository.findTopByOrderByHeightDesc().blockOptional().ifPresentOrElse({ block -> latestBlockHeight = block.height }, {
            client.getLatestBlockHeader(Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build(), object :
                StreamObserver<Access.BlockHeaderResponse> {
                override fun onNext(value: Access.BlockHeaderResponse) {
                    latestBlockHeight = value.block.height - 1
                }

                override fun onError(t: Throwable) {
                    throw t
                }

                override fun onCompleted() {
                    log.info("Initialization request complete")
                }
            })
        })
    }

    @Scheduled(fixedDelay = 2000L, initialDelay = 2000L)
    fun anotherScan() {
        client.getLatestBlockHeader(
            Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build(),
            object : StreamObserver<Access.BlockHeaderResponse> {
                override fun onNext(value: Access.BlockHeaderResponse) {
                    val current = latestBlockHeight
                    latestBlockHeight = value.block.height
                    publisher.publishEvent(FlowBlockRangeRequest(current, latestBlockHeight))
                }

                override fun onError(t: Throwable?) {
                    log.error("AnotherScan tick error!")
                    log.error(t?.message, t)
                }

                override fun onCompleted() {
                    log.info("AnotherScan tick done!")
                }

            }
        )
    }

    @EventListener(FlowBlockRangeRequest::class)
    fun blockRangeRequest(event: FlowBlockRangeRequest) {
        val range = LongRange(event.from, event.to)
        range.forEach {
            readBlock(it)
        }
    }

    @Async
    fun readBlock(height: Long) {
        client.getBlockByHeight(Access.GetBlockByHeightRequest.newBuilder().setHeight(height).build(), object : StreamObserver<Access.BlockResponse> {
            override fun onNext(value: Access.BlockResponse) {
                val block = value.block
                val fb = FlowBlock(
                    id = Hex.toHexString(block.id.toByteArray()),
                    parentId = Hex.toHexString(block.parentId.toByteArray()),
                    height = block.height,
                    timestamp = Instant.ofEpochSecond(block.timestamp.seconds),
                    collectionsCount = block.collectionGuaranteesCount
                )
                publisher.publishEvent(FlowBlockReceived(fb))
            }

            override fun onError(t: Throwable) {
                log.error(t.message, t)
            }

            override fun onCompleted() {
                log.info("read block [$height] complete!")
            }

        })
    }

    @EventListener(FlowBlockReceived::class)
    fun receiveBlockListener(event: FlowBlockReceived) {
        val block = event.block
        if (block.collectionsCount > 0) {
            publisher.publishEvent(CalculateTransactionsCount(block))
        } else {
            publisher.publishEvent(FlowBlockReadyForPersist(block))
        }
    }

    @EventListener(FlowBlockReadyForPersist::class)
    fun persistBlock(event: FlowBlockReadyForPersist) {
        blockRepository.save(event.block).subscribe {
            publisher.publishEvent(FlowBlockPersisted(it))
            messageTemplate.convertAndSend("/topic/block", it)
        }
    }
}
