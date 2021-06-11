package com.rarible.flow.scanner.batch

import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import org.bouncycastle.util.encoders.Hex
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.protobuf.entities.BlockOuterClass
import org.springframework.batch.item.ItemProcessor
import java.time.Instant

/**
 * Created by TimochkinEA at 09.06.2021
 */
class FlowProcessor(private val client: AccessAPIGrpc.AccessAPIBlockingStub) : ItemProcessor<BlockOuterClass.Block, Pair<FlowBlock, List<FlowTransaction>>> {
    override fun process(item: BlockOuterClass.Block): Pair<FlowBlock, List<FlowTransaction>>? {
        item.collectionGuaranteesList

        val fb = FlowBlock(
            id = Hex.toHexString(item.id.toByteArray()),
            parentId = Hex.toHexString(item.parentId.toByteArray()),
            height = item.height,
            timestamp = Instant.ofEpochSecond(item.timestamp.seconds),
            collectionsCount = item.collectionGuaranteesCount
        )

        val transactions = mutableListOf<FlowTransaction>()
        if (fb.collectionsCount > 0) {
            item.collectionGuaranteesList.forEach { cgl ->
                client.getCollectionByID(
                    Access.GetCollectionByIDRequest.newBuilder().setId(cgl.collectionId).build()
                ).collection.transactionIdsList.forEach { txId ->
                    val request = Access.GetTransactionRequest.newBuilder().setId(txId).build()
                    val tx = client.getTransaction(request).transaction
                    val result = client.getTransactionResult(request)

                    transactions.add(FlowTransaction(
                        id = Hex.toHexString(tx.referenceBlockId.toByteArray()),
                        blockHeight = fb.height,
                        proposer = Hex.toHexString(tx.proposalKey.address.toByteArray()),
                        payer = Hex.toHexString(tx.payer.toByteArray()),
                        authorizers = tx.authorizersList.map { Hex.toHexString(it.toByteArray()) },
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
