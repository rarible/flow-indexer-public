package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowEvent
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.repository.FlowLogEventRepository
import com.rarible.flow.core.util.Log
import com.rarible.flow.scanner.service.RecordKeyProvider
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

abstract class BaseFlowLogEventSubscriber(val chainId: FlowChainId) : FlowLogEventSubscriber {

    protected val collection = "flow_log_event"

    protected val logger by Log()

    @Autowired
    private lateinit var flogEventRepository: FlowLogEventRepository

    @Autowired
    private lateinit var recordKeyProvider: RecordKeyProvider

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getDescriptor(): FlowDescriptor = when (this.chainId) {
        FlowChainId.EMULATOR -> descriptors[this.chainId] ?: EMPTY_DESCRIPTOR
        else -> {
            descriptors[this.chainId]
                ?: throw IllegalArgumentException(
                    "Cant find descriptor for ${this.javaClass.simpleName} (chain ${this.chainId}), available descriptiors are: $descriptors"
                )
        }
    }

    override suspend fun getEventRecords(
        block: FlowBlockchainBlock,
        log: FlowBlockchainLog
    ): List<FlowLogRecord> {
        val descriptor = getDescriptor()
        return if (descriptor.events.contains(log.event.id) && isNewEvent(block, log.event)) {
            listOf(
                FlowLogEvent(
                    log = FlowLog(
                        transactionHash = log.event.transactionId.base16Value,
                        eventIndex = log.event.eventIndex,
                        eventType = log.event.type,
                        timestamp = Instant.ofEpochMilli(block.timestamp),
                        blockHeight = block.number,
                        blockHash = block.hash
                    ),
                    event = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, log.event.event),
                    type = eventType(log),
                ).let {
                    val recordKey = recordKeyProvider.getRecordKey(it)
                    it.copy(recordKey = recordKey)
                }
            )
        } else emptyList()
    }

    protected open suspend fun isNewEvent(block: FlowBlockchainBlock, event: FlowEvent): Boolean {
        return !flogEventRepository.existsById("${event.transactionId.base16Value}.${event.eventIndex}").awaitSingle()
    }

    abstract suspend fun eventType(log: FlowBlockchainLog): FlowLogType

    private companion object {
        val EMPTY_DESCRIPTOR = FlowDescriptor(
            address = "",
            collection = "",
            groupId = "",
            entityType = Any::class.java,
            id = "",
            events = emptySet(),
        )
    }
}
