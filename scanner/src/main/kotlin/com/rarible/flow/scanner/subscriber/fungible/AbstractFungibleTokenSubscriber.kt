package com.rarible.flow.scanner.subscriber.fungible

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.UFix64NumberField
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.domain.BalanceId
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.enum.safeOf
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.subscriber.balanceHistory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value


abstract class AbstractFungibleTokenSubscriber: FlowLogEventSubscriber {

    val dbCollection = "balance_events"

    @Value("\${blockchain.scanner.flow.chainId}")
    protected lateinit var chainId: FlowChainId

    @Autowired
    protected lateinit var balanceRepository: BalanceRepository

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getDescriptor(): FlowDescriptor = descriptors[chainId]!!

    override fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord<*>> = flow {
        val payload = com.nftco.flow.sdk.FlowEventPayload(log.event.payload.bytes)
        val event = log.event.copy(payload = payload)
        val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
        val eventType = safeOf<FungibleEvents>(
            EventId.of(fixedLog.event.type).eventName
        )
        emitAll(if (eventType == null) {
            logger.info("Unknown FlowToken event: {}", fixedLog.event)
            emptyFlow<FlowLogRecord<BalanceHistory>>()
        } else {
            val token = EventId.of(fixedLog.event.type).collection()
            val fields = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, fixedLog.event.event).fields

            when (eventType) {
                FungibleEvents.TokensWithdrawn -> {
                    val from: OptionalField by fields
                    if (from.value == null) {
                        emptyFlow<FlowLogRecord<BalanceHistory>>()
                    }
                    val amount: UFix64NumberField by fields
                    val balanceId = BalanceId(FlowAddress((from.value as AddressField).value!!), token)
                    if (balanceRepository.existsById(balanceId).awaitSingle()) {
                        flowOf(balanceHistory(
                            balanceId, amount.toBigDecimal()!!.negate(), block, fixedLog
                        ))
                    } else emptyFlow()
                }
                FungibleEvents.TokensDeposited -> {
                    val to: OptionalField by fields
                    if (to.value == null) {
                        emptyFlow<FlowLogRecord<BalanceHistory>>()
                    }
                    val amount: UFix64NumberField by fields
                    val balanceId = BalanceId(FlowAddress((to.value as AddressField).value!!), token)
                    if (balanceRepository.existsById(balanceId).awaitSingle()) {
                        flowOf(balanceHistory(
                            to, amount.toBigDecimal()!!, token, block, fixedLog
                        ))
                    } else emptyFlow()
                }
            }
        })
    }

    companion object {
        enum class FungibleEvents {
            TokensWithdrawn,
            TokensDeposited;
        }

        fun supportedEvents(): Set<String> {
            return FungibleEvents.values().map { it.name }.toSet()
        }

        val logger by Log()
    }
}
