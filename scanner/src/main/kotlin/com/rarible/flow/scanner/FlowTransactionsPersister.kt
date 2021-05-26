package com.rarible.flow.scanner

import com.rarible.flow.scanner.events.FlowBlockPersisted
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.client.inject.GrpcClient
import org.bouncycastle.util.encoders.Hex
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Component
class FlowTransactionsPersister(
    private val repository: FlowTransactionRepository,
    private val messageTemplate: SimpMessagingTemplate
    ) {

    private val log: Logger = LoggerFactory.getLogger(FlowTransactionsPersister::class.java)

    @GrpcClient("flow")
    private lateinit var client: AccessAPIGrpc.AccessAPIStub

    @EventListener(FlowBlockPersisted::class)
    fun blockSaved(event: FlowBlockPersisted) {
        if (event.block.transactionsCount > 0) {
            client.getBlockByHeight(
                Access.GetBlockByHeightRequest.newBuilder().setHeight(event.block.height).build(),
                object : StreamObserver<Access.BlockResponse> {
                    override fun onNext(value: Access.BlockResponse) {
                        value.block.collectionGuaranteesList.forEach { cgl ->
                            client.getCollectionByID(
                                Access.GetCollectionByIDRequest.newBuilder()
                                    .setId(cgl.collectionId)
                                    .build(),
                                object : StreamObserver<Access.CollectionResponse> {
                                    override fun onNext(value: Access.CollectionResponse) {
                                        value.collection.transactionIdsList.forEach { txId ->
                                            client.getTransaction(
                                                Access.GetTransactionRequest.newBuilder().setId(txId)
                                                    .build(),
                                                object : StreamObserver<Access.TransactionResponse> {
                                                    override fun onNext(value: Access.TransactionResponse) {

                                                        val tx = FlowTransaction(
                                                            id = Hex.toHexString(value.transaction.referenceBlockId.toByteArray()),
                                                            blockHeight = event.block.height,
                                                            proposer = Hex.toHexString(value.transaction.proposalKey.address.toByteArray()),
                                                            payer = Hex.toHexString(value.transaction.payer.toByteArray()),
                                                            authorizers = value.transaction.authorizersList.map { Hex.toHexString(it.toByteArray()) },
                                                            script = value.transaction.script.toStringUtf8()
                                                        )
                                                        client.getTransactionResult(
                                                            Access.GetTransactionRequest.newBuilder().setId(txId).build(),
                                                            object : StreamObserver<Access.TransactionResultResponse> {
                                                                override fun onNext(value: Access.TransactionResultResponse) {
                                                                    value.eventsList.forEach { e ->
                                                                        tx.events.add(
                                                                            FlowEvent(
                                                                                type = e.type,
                                                                                data = e.payload.toStringUtf8()
                                                                            )
                                                                        )
                                                                    }
                                                                    repository.save(tx).subscribe {
                                                                        log.info("Transaction Persisted")
                                                                        messageTemplate.convertAndSend("/topic/transaction", it)
                                                                    }
                                                                }

                                                                override fun onError(t: Throwable) {
                                                                    log.error(t.message, t)
                                                                }

                                                                override fun onCompleted() {

                                                                }

                                                            }
                                                        )
                                                    }

                                                    override fun onError(t: Throwable) {
                                                        log.error(t.message, t)
                                                    }

                                                    override fun onCompleted() {

                                                    }

                                                }
                                            )
                                        }
                                    }

                                    override fun onError(t: Throwable) {
                                        log.error(t.message, t)
                                    }

                                    override fun onCompleted() {

                                    }

                                }
                            )
                        }
                    }

                    override fun onError(t: Throwable) {
                        log.error(t.message, t)
                    }

                    override fun onCompleted() {

                    }

                }
            )
        }
    }
}
