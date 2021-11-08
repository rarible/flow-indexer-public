package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowEventPayload
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.events.EventMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

abstract class BaseItemHistoryFlowLogSubscriber : FlowLogEventSubscriber {

    internal val collection = "item_history"

    internal fun flowDescriptor(
        address: String,
        contract: String,
        events: Iterable<String>,
        startFrom: Long? = null,
    ) = FlowDescriptor(
        id = "${contract}Descriptor",
        events = events.map { "A.$address.$contract.$it" }.toSet(),
        collection = collection,
        startFrom = startFrom
    )

    private val reg1 = Regex.fromLiteral("\"type\":\"Type\"")
    private val reg2 = Regex("""\{"staticType":"([^"]+)"}""")

    // replace "Type" field to "String" field
    private fun ByteArray.fixed(): ByteArray {
        val s1 = reg1.replace(String(this), "\"type\":\"String\"")
        val s2 = reg2.replace(s1) { "\"${it.groupValues[1]}\"" }
        return s2.toByteArray()
    }

    @Value("\${blockchain.scanner.flow.chainId}")
    lateinit var chainId: FlowChainId

    @Autowired
    private lateinit var itemHistoryRepository: ItemHistoryRepository

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override suspend fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord<*>> {
        val descriptor = getDescriptor()
        val payload = FlowEventPayload(log.event.payload.bytes.fixed())
        val event = log.event.copy(payload = payload)
        val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
        return if (descriptor.events.contains(fixedLog.event.id)) {
            val blockTimestamp = Instant.ofEpochMilli(block.timestamp)
            val activity = activity(
                block, fixedLog,
                com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, fixedLog.event.event))
            if (activity == null) {
                emptyFlow()
            } else if (isNewLog(log)) {
                flowOf(
                    ItemHistory(
                        log = FlowLog(
                            transactionHash = fixedLog.event.transactionId.base16Value,
                            status = Log.Status.CONFIRMED,
                            eventIndex = fixedLog.event.eventIndex,
                            eventType = fixedLog.event.type,
                            timestamp = blockTimestamp,
                            blockHeight = block.number,
                            blockHash = block.hash
                        ),
                        date = blockTimestamp,
                        activity = activity
                    )
                )
            } else emptyFlow()
        } else emptyFlow()
    }

    override fun getDescriptor(): FlowDescriptor = descriptors[chainId]!!

    abstract suspend fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): BaseActivity?

    private suspend fun isNewLog(log: FlowBlockchainLog): Boolean {
        val txHash = log.event.transactionId.base16Value
        val eventIndex = log.event.eventIndex
        return !itemHistoryRepository.existsByLog_TransactionHashAndLog_EventIndex(txHash, eventIndex).awaitSingle()
    }
}
