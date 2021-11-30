package com.rarible.flow.scanner.subscriber.fungible

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.UFix64NumberField
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.domain.BalanceId
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.enum.safeOf
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.springframework.beans.factory.annotation.Value
import java.math.BigDecimal
import java.time.Instant


abstract class AbstractFungibleTokenSubscriber(
    open val balanceRepository: BalanceRepository
) : FlowLogEventSubscriber {

    @Value("\${blockchain.scanner.flow.chainId}")
    lateinit var chainId: FlowChainId

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getDescriptor(): FlowDescriptor = descriptors[chainId]!!

    override fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord<*>> = flow {
        val descriptor = getDescriptor()
        val payload = com.nftco.flow.sdk.FlowEventPayload(log.event.payload.bytes)
        val event = log.event.copy(payload = payload)
        val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
        val eventType = safeOf<FungibleEvents>(fixedLog.event.id)
        if (eventType == null) {
            logger.info("Unknown FlowToken event: {}", fixedLog.event)
        } else {
            val token = EventId.of(fixedLog.event.type).collection()
            val fields = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, fixedLog.event.event).fields

            when (eventType) {
                FungibleEvents.TokensWithdrawn -> {
                    val from: OptionalField by fields
                    val amount: UFix64NumberField by fields

                    updateBalance(from, token) { b ->
                        b.withdraw(amount.toBigDecimal() ?: BigDecimal.ZERO)
                    }
                }
                FungibleEvents.TokensDeposited -> {
                    val to: OptionalField by fields
                    val amount: UFix64NumberField by fields

                    updateBalance(to, token) { b ->
                        b.deposit(amount.toBigDecimal() ?: BigDecimal.ZERO)
                    }
                }
            }
        }
        emptyFlow<FlowLogRecord<*>>()
    }

    private suspend fun updateBalance(address: OptionalField, token: String, update: (Balance) -> Balance) {
        if (address.value != null) {
            val flowAddress = FlowAddress((address.value as AddressField).value!!)
            val updated = update(
                balanceRepository
                    .findById(BalanceId(flowAddress, token))
                    .awaitFirstOrDefault(Balance(flowAddress, token))
            )

            if (updated.balance.compareTo(BigDecimal.ZERO) == 1) {
                logger.warn("Incorrect balance after update: {}", updated)
            }

            balanceRepository.coSave(updated)
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