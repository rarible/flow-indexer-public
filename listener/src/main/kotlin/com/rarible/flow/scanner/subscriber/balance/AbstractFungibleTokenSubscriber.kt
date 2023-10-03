package com.rarible.flow.scanner.subscriber.balance

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
import com.rarible.flow.core.domain.BalanceId
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.util.Log
import com.rarible.flow.core.util.safeOf
import com.rarible.flow.scanner.subscriber.balanceHistory
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

abstract class AbstractFungibleTokenSubscriber : FlowLogEventSubscriber {

    val dbCollection = "balance_events"

    @Value("\${blockchain.scanner.flow.chainId}")
    protected lateinit var chainId: FlowChainId

    @Autowired
    protected lateinit var balanceRepository: BalanceRepository

    @Autowired
    private lateinit var mongo: ReactiveMongoTemplate

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getDescriptor(): FlowDescriptor = descriptors[chainId]!!
    override suspend fun getEventRecords(
        block: FlowBlockchainBlock,
        log: FlowBlockchainLog
    ): List<FlowLogRecord> {
        val payload = com.nftco.flow.sdk.FlowEventPayload(log.event.payload.bytes)
        val event = log.event.copy(payload = payload)
        val fixedLog = FlowBlockchainLog(log.hash, log.blockHash, event)
        val eventType = safeOf<FungibleEvents>(
            EventId.of(fixedLog.event.type).eventName
        )
        val token = EventId.of(fixedLog.event.type).collection()
        val fields = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, fixedLog.event.event).fields
        return when (eventType) {
            FungibleEvents.TokensWithdrawn -> {
                val from: OptionalField by fields
                if (from.value == null) {
                    emptyList()
                } else {
                    val amount: UFix64NumberField by fields
                    val balanceId = BalanceId(FlowAddress((from.value as AddressField).value!!), token)
                    if (isNewEvent(fixedLog) && balanceRepository.existsById(balanceId).awaitSingle()) {
                        listOf(
                            balanceHistory(
                                balanceId, amount.toBigDecimal()!!.negate(), block, fixedLog
                            )
                        )
                    } else emptyList()
                }
            }
            FungibleEvents.TokensDeposited -> {
                val to: OptionalField by fields
                if (to.value == null) {
                    emptyList()
                } else {
                    val amount: UFix64NumberField by fields
                    val balanceId = BalanceId(FlowAddress((to.value as AddressField).value!!), token)
                    if (isNewEvent(fixedLog) && balanceRepository.existsById(balanceId).awaitSingle()) {
                        listOf(
                            balanceHistory(
                                to, amount.toBigDecimal()!!, token, block, fixedLog
                            )
                        )
                    } else emptyList()
                }
            }
            null -> {
                logger.info("Unknown FlowToken event: {}", fixedLog.event)
                emptyList()
            }
        }
    }

    private suspend fun isNewEvent(log: FlowBlockchainLog): Boolean {
        return mongo.exists(
            Query(Criteria.where("_id").isEqualTo("${log.event.transactionId}${log.event.eventIndex}")),
            dbCollection
        ).awaitSingle().not()
    }

    companion object {
        enum class FungibleEvents {
            TokensWithdrawn,
            TokensDeposited;
        }

        fun supportedEvents(): Set<String> {
            return FungibleEvents.values().map { it.name }.toSet()
        }

        private val logger by Log()
    }
}
