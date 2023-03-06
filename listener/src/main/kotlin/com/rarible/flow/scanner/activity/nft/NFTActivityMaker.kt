package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.util.findAfterEventIndex
import com.rarible.flow.core.util.findBeforeEventIndex
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.ActivityMaker
import com.rarible.flow.scanner.model.BurnEvent
import com.rarible.flow.scanner.model.DepositEvent
import com.rarible.flow.scanner.model.GeneralBurnEvent
import com.rarible.flow.scanner.model.GeneralDepositEvent
import com.rarible.flow.scanner.model.GeneralMintEvent
import com.rarible.flow.scanner.model.GeneralWithdrawEvent
import com.rarible.flow.scanner.model.MintEvent
import com.rarible.flow.scanner.model.NFTEvent
import com.rarible.flow.scanner.model.WithdrawEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

abstract class NFTActivityMaker : ActivityMaker {

    abstract val contractName: String

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    @Value("\${blockchain.scanner.flow.chainId}")
    lateinit var chainId: FlowChainId

    @Autowired
    protected lateinit var txManager: TxManager

    @Autowired
    protected lateinit var flowLogRepository: FlowLogRepository

    fun <T> parse(fn: JsonCadenceParser.() -> T): T {
        return fn(cadenceParser)
    }

    override fun isSupportedCollection(collection: String): Boolean =
        collection.substringAfterLast(".").lowercase() == contractName.lowercase()

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
        events.forEach {
            val activity = when (it.type) {
                FlowLogType.MINT -> getMintActivity(it)
                FlowLogType.BURN -> getBurnActivity(it)
                FlowLogType.DEPOSIT -> getTransferActivity(it)
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

    private suspend fun getTransferActivity(event: FlowLogEvent): TransferActivity? {
        val deposit = deposit(event)
        val withdraw = findWithdrawBefore(deposit)
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

    private suspend fun findDepositAfter(event: NFTEvent): DepositEvent? {
        val result = flowLogRepository.findAfterEventIndex(
            transactionHash = event.transactionHash,
            afterEventIndex = event.eventIndex,
        )
        return result
            .filter { it.type == FlowLogType.DEPOSIT }
            .map { deposit(it) }
            .firstOrNull { it.sameNftEvent(event) }
    }

    private suspend fun findWithdrawBefore(event: NFTEvent): WithdrawEvent? {
        val result = flowLogRepository.findBeforeEventIndex(
            transactionHash = event.transactionHash,
            beforeEventIndex = event.eventIndex,
        )
        return result
            .filter { it.type == FlowLogType.WITHDRAW }
            .map { withdraw(it) }
            .firstOrNull { it.sameNftEvent(event) }
    }

    abstract fun tokenId(logEvent: FlowLogEvent): Long

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

    protected open suspend fun itemCollection(mintEvent: FlowLogEvent): String {
        return mintEvent.event.eventId.collection()
    }
}