package com.rarible.flow.scanner

import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import org.bouncycastle.util.encoders.Hex
import org.onflow.protobuf.access.Access
import java.time.Instant
import java.util.concurrent.Callable

/**
 * Created by TimochkinEA at 01.06.2021
 */
class ReadTask(private val blockHeight: Long) : Callable<Pair<FlowBlock, List<FlowTransaction>>> {

    override fun call(): Pair<FlowBlock, List<FlowTransaction>> {
        val client = GrpcClients.forBlock(blockHeight).sync

        val block = client.getBlockByHeight(
            Access.GetBlockByHeightRequest.newBuilder().setHeight(blockHeight).build()
        ).block
        val fb = FlowBlock(
            id = Hex.toHexString(block.id.toByteArray()),
            parentId = Hex.toHexString(block.parentId.toByteArray()),
            height = block.height,
            timestamp = Instant.ofEpochSecond(block.timestamp.seconds),
            collectionsCount = block.collectionGuaranteesCount
        )

        val transactions = mutableListOf<FlowTransaction>()
        if (fb.collectionsCount > 0) {
            block.collectionGuaranteesList.forEach { cgl ->
                client.getCollectionByID(
                    Access.GetCollectionByIDRequest.newBuilder().setId(cgl.collectionId).build()
                ).collection.transactionIdsList.forEach { txId ->
                    val request = Access.GetTransactionRequest.newBuilder().setId(txId).build()
                    val tx = client.getTransaction(request).transaction
                    val result = client.getTransactionResult(request)

                    transactions.add(FlowTransaction(
                        id = Hex.toHexString(tx.referenceBlockId.toByteArray()),
                        blockHeight = blockHeight,
                        proposer = tx.proposalKey.address.toStringUtf8(),
                        payer = tx.payer.toStringUtf8(),
                        authorizers = tx.authorizersList.map { it.toStringUtf8() },
                        script = tx.script.toStringUtf8().trimIndent(),
                        events = result.eventsList.map {
                            FlowEvent(
                                type = it.type,
                                data = it.payload.toStringUtf8()
                            )
                        }
                    ))
                }
            }
        }
        fb.transactionsCount = transactions.size
        return Pair(fb, transactions.toList())
    }


}
