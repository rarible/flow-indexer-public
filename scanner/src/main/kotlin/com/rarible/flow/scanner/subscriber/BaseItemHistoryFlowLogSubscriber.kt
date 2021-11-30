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
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.math.BigDecimal
import java.time.Instant

abstract class BaseItemHistoryFlowLogSubscriber : FlowLogEventSubscriber {

    internal val collection = "item_history"

    internal val logger by com.rarible.flow.log.Log()

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

    @Autowired
    private lateinit var currencyApi: CurrencyControllerApi

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord<*>> = flow {
        val descriptor = getDescriptor()
        val payload = FlowEventPayload(log.event.payload.bytes.fixed())
        val event = log.event.copy(payload = payload)
        val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
        emitAll(if (descriptor.events.contains(fixedLog.event.id)) {
            val blockTimestamp = Instant.ofEpochMilli(block.timestamp)
            val activity = activity(
                block, fixedLog,
                com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, fixedLog.event.event)
            )
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
        } else emptyFlow())
    }

    override fun getDescriptor(): FlowDescriptor = descriptors[chainId]!!

    abstract suspend fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): BaseActivity?

    internal suspend fun usdRate(contract: String, timestamp: Long) = try {
        currencyApi.getCurrencyRate(BlockchainDto.FLOW, contract, timestamp).awaitSingle().rate
    } catch (e: Exception) {
        logger.warn("Unable to fetch USD price rate from currency api: ${e.message}", e)
        BigDecimal.ZERO
    }

    private suspend fun isNewLog(log: FlowBlockchainLog): Boolean {
        val txHash = log.event.transactionId.base16Value
        val eventIndex = log.event.eventIndex
        return !itemHistoryRepository.existsByLog_TransactionHashAndLog_EventIndex(txHash, eventIndex).awaitSingle()
    }
}
