package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.util.findAfterEventIndex
import com.rarible.flow.core.util.findBeforeEventIndex
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.ActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.model.BurnEvent
import com.rarible.flow.scanner.model.DepositEvent
import com.rarible.flow.scanner.model.GeneralBurnEvent
import com.rarible.flow.scanner.model.GeneralDepositEvent
import com.rarible.flow.scanner.model.GeneralMintEvent
import com.rarible.flow.scanner.model.GeneralWithdrawEvent
import com.rarible.flow.scanner.model.MintEvent
import com.rarible.flow.scanner.model.NFTEvent
import com.rarible.flow.scanner.model.WithdrawEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

abstract class NFTActivityMaker(
    private val flowLogRepository: FlowLogRepository,
    private val txManager: TxManager,
    properties: FlowListenerProperties,
) : ActivityMaker {

    abstract val contractName: String

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    protected val chainId = properties.chainId

    fun <T> parse(fn: JsonCadenceParser.() -> T): T {
        return fn(cadenceParser)
    }

    override fun isSupportedCollection(collection: String): Boolean =
        collection.substringAfterLast(".").lowercase() == contractName.lowercase()

    open fun tokenId(logEvent: FlowLogEvent): Long {
        return mint(logEvent).tokenId
    }

    override fun getItemId(event: FlowLogEvent): ItemId {
        return ItemId(itemCollection(event), tokenId(event))
    }

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
        events.forEach {
            val activity = when (it.type) {
                FlowLogType.MINT -> getMintActivity(it)
                FlowLogType.BURN -> getBurnActivity(it)
                FlowLogType.DEPOSIT -> getDepositTransferActivity(it)
                FlowLogType.WITHDRAW -> getWithdrawTransferActivity(it)
                else -> null
            }
            if (activity != null) result[it.log] = activity
        }
        return result
    }

    private suspend fun getMintActivity(event: FlowLogEvent): MintActivity? {
        val mint = mint(event)
        val deposit = findDepositAfter(mint)
        return MintActivity(
            creator = creator(event),
            contract = mint.collection,
            tokenId = mint.tokenId,
            owner = deposit?.to ?: mint.contractAddress,
            metadata = meta(event),
            royalties = royalties(event),
            collection = itemCollection(event),
            timestamp = event.log.timestamp,
        )
    }

    private suspend fun getBurnActivity(event: FlowLogEvent): BurnActivity? {
        val burn = burn(event)
        val withdraw = findWithdrawBefore(burn)
        return BurnActivity(
            contract = burn.collection,
            tokenId = burn.tokenId,
            owner = withdraw?.from ?: burn.contractAddress,
            timestamp = event.log.timestamp
        )
    }

    private suspend fun getDepositTransferActivity(event: FlowLogEvent): TransferActivity? {
        val deposit = deposit(event)
        val events = findBefore(deposit, listOf(FlowLogType.WITHDRAW, FlowLogType.MINT))
        require(events.size <= 1) {
            "Found more then 1 event coupled with deposit: tx=${event.log.transactionHash}, index=${event.log.eventIndex}"
        }
        val withdraw = when (val nftEvent = events.singleOrNull()) {
            is WithdrawEvent -> nftEvent
            null -> null
            else -> return null
        }
        return if (deposit.optionalTo != null) {
            TransferActivity(
                contract = deposit.collection,
                tokenId = deposit.tokenId,
                to = deposit.to,
                from = withdraw?.from ?: deposit.contractAddress,
                timestamp = event.log.timestamp
            )
        } else null
    }

    private suspend fun getWithdrawTransferActivity(event: FlowLogEvent): TransferActivity? {
        val withdraw = withdraw(event)
        val events = findAfter(withdraw, listOf(FlowLogType.DEPOSIT, FlowLogType.BURN))
        require(events.size <= 1) {
            "Found more then 1 event coupled with withdraw: tx=${event.log.transactionHash}, index=${event.log.eventIndex}"
        }
        when (events.singleOrNull()) {
            null -> {}
            else -> return null
        }
        return if (withdraw.optionalFrom != null) {
            TransferActivity(
                contract = withdraw.collection,
                tokenId = withdraw.tokenId,
                from = withdraw.from,
                to = withdraw.contractAddress,
                timestamp = event.log.timestamp
            )
        } else null
    }

    private suspend fun findDepositAfter(event: NFTEvent): DepositEvent? {
        return findAfter(event, listOf(FlowLogType.DEPOSIT)).firstOrNull() as DepositEvent?
    }

    private suspend fun findWithdrawBefore(event: NFTEvent): WithdrawEvent? {
        return findBefore(event, listOf(FlowLogType.WITHDRAW)).firstOrNull() as WithdrawEvent?
    }

    private suspend fun findAfter(event: NFTEvent, types: List<FlowLogType>): List<NFTEvent> {
        return findNftEvent(event, types) { tx, index ->
            flowLogRepository.findAfterEventIndex(tx, index)
        }
    }

    private suspend fun findBefore(event: NFTEvent, types: List<FlowLogType>): List<NFTEvent> {
        return findNftEvent(event, types) { tx, index ->
            flowLogRepository.findBeforeEventIndex(tx, index)
        }
    }

    private suspend fun findNftEvent(
        event: NFTEvent,
        types: List<FlowLogType>,
        search: suspend (String, Int) -> Flow<FlowLogEvent>
    ): List<NFTEvent> {
        val result = search(event.transactionHash, event.eventIndex).toList()
        return result
            .filter { it.type in types }
            .mapNotNull {
                when (it.type) {
                    FlowLogType.MINT -> mint(it)
                    FlowLogType.BURN -> burn(it)
                    FlowLogType.DEPOSIT -> deposit(it)
                    FlowLogType.WITHDRAW -> withdraw(it)
                    else -> throw IllegalArgumentException("Unsupported event type in $types")
                }.takeIf { nftEvent -> nftEvent.sameNft(event) }
            }
    }

    abstract fun meta(logEvent: FlowLogEvent): Map<String, String>

    protected open fun mint(logEvent: FlowLogEvent): MintEvent {
        return GeneralMintEvent(logEvent)
    }

    protected open fun burn(logEvent: FlowLogEvent): BurnEvent {
        return GeneralBurnEvent(logEvent)
    }

    protected open fun deposit(logEvent: FlowLogEvent): DepositEvent {
        return GeneralDepositEvent(logEvent)
    }

    protected open fun withdraw(logEvent: FlowLogEvent): WithdrawEvent {
        return GeneralWithdrawEvent(logEvent)
    }

    protected open fun royalties(logEvent: FlowLogEvent): List<Part> {
        return emptyList()
    }

    protected open fun creator(logEvent: FlowLogEvent): String {
        return logEvent.event.eventId.contractAddress.formatted
    }

    protected open fun itemCollection(mintEvent: FlowLogEvent): String {
        return mintEvent.event.eventId.collection()
    }
}
