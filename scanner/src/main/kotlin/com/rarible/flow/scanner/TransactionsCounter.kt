package com.rarible.flow.scanner

import com.rarible.flow.scanner.events.CalculateTransactionsCount
import com.rarible.flow.scanner.events.FlowBlockReadyForPersist
import net.devh.boot.grpc.client.inject.GrpcClient
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Component
class TransactionsCounter(private val publisher: ApplicationEventPublisher) {

    @GrpcClient("flow")
    private lateinit var client: AccessAPIGrpc.AccessAPIBlockingStub

    @EventListener(CalculateTransactionsCount::class)
    fun calculate(event: CalculateTransactionsCount) {
        val block = client.getBlockByHeight(Access.GetBlockByHeightRequest.newBuilder().setHeight(event.block.height).build()).block
        block.collectionGuaranteesList.forEach { cgl ->
            val collection = client.getCollectionByID(Access.GetCollectionByIDRequest.newBuilder().setId(cgl.collectionId).build()).collection
            event.block.transactionsCount += collection.transactionIdsCount
        }
        publisher.publishEvent(FlowBlockReadyForPersist(event.block))
    }
}
