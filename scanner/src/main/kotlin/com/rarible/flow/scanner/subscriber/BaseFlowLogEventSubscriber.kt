package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowEventPayload
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.repository.FlowLogEventRepository
import com.rarible.flow.events.EventMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

abstract class BaseFlowLogEventSubscriber: FlowLogEventSubscriber {

    @Value("\${blockchain.scanner.flow.chainId}")
    protected lateinit var chainId: FlowChainId

    protected val collection = "flow_log_event"

    protected val logger by com.rarible.flow.log.Log()

    @Autowired
    private lateinit var flogEventRepository: FlowLogEventRepository


    private val reg1 = Regex.fromLiteral("\"type\":\"Type\"")
    private val reg2 = Regex("""\{"staticType":"([^"]+)"}""")

    // replace "Type" field to "String" field
    private fun ByteArray.fixed(): ByteArray {
        val s1 = reg1.replace(String(this), "\"type\":\"String\"")
        val s2 = reg2.replace(s1) { "\"${it.groupValues[1]}\"" }
        return s2.toByteArray()
    }

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getDescriptor(): FlowDescriptor = when(chainId) {
        FlowChainId.EMULATOR -> FlowDescriptor("", emptySet(), "")
        else -> descriptors[chainId]!!
    }

    override fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord<*>> = flow {
        val descriptor = getDescriptor()
        val payload = FlowEventPayload(log.event.payload.bytes.fixed())
        val event = log.event.copy(payload = payload)
        val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
        emitAll(
            if (descriptor.events.contains(fixedLog.event.id) && isNewEvent(block, event)) {
                flowOf(
                    FlowLogEvent(
                        log = FlowLog(
                            transactionHash = fixedLog.event.transactionId.base16Value,
                            status = Log.Status.CONFIRMED,
                            eventIndex = fixedLog.event.eventIndex,
                            eventType = fixedLog.event.type,
                            timestamp = Instant.ofEpochMilli(block.timestamp),
                            blockHeight = block.number,
                            blockHash = block.hash
                        ),
                        event = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, event.event),
                        type = eventType(fixedLog),
                    )
                )
            } else emptyFlow()
        )
    }


    protected open suspend fun isNewEvent(block: FlowBlockchainBlock, event: FlowEvent): Boolean {
        return !flogEventRepository.existsById("${event.transactionId.base16Value}.${event.eventIndex}").awaitSingle()
    }

    abstract suspend fun eventType(log: FlowBlockchainLog): FlowLogType
}
