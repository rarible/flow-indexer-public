package com.rarible.flow.scanner.subscriber.fungible

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.UFix64NumberField
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.enum.safeOf
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.subscriber.balanceHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.springframework.beans.factory.annotation.Value


abstract class AbstractFungibleTokenSubscriber(
    open val balanceRepository: BalanceRepository
) : FlowLogEventSubscriber {

    @Value("\${blockchain.scanner.flow.chainId}")
    lateinit var chainId: FlowChainId

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getDescriptor(): FlowDescriptor = descriptors[chainId]!!

    override fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord<*>> {
        val payload = com.nftco.flow.sdk.FlowEventPayload(log.event.payload.bytes)
        val event = log.event.copy(payload = payload)
        val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
        val eventType = safeOf<FungibleEvents>(fixedLog.event.id)
        return if (eventType == null) {
            logger.info("Unknown FlowToken event: {}", fixedLog.event)
            emptyFlow<FlowLogRecord<BalanceHistory>>()
        } else {
            val event = log.event.copy(payload = payload)
            val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
            val token = EventId.of(fixedLog.event.type).collection()
            val fields = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, fixedLog.event.event).fields

            val balanceHistory = when (eventType) {
                FungibleEvents.TokensWithdrawn -> {
                    val from: OptionalField by fields
                    val amount: UFix64NumberField by fields
                    balanceHistory(
                        from, amount.toBigDecimal()!!.negate(), token, block, fixedLog
                    )
                }
                FungibleEvents.TokensDeposited -> {
                    val to: OptionalField by fields
                    val amount: UFix64NumberField by fields
                    balanceHistory(
                        to, amount.toBigDecimal()!!, token, block, fixedLog
                    )
                }
            }

            flowOf(balanceHistory)
        }
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