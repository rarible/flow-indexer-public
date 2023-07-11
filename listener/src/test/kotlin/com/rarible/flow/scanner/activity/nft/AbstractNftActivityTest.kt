package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import com.rarible.flow.scanner.test.BaseJsonEventTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import java.time.Instant

abstract class AbstractNftActivityTest : BaseJsonEventTest() {
    protected val txManager = mockk<TxManager>()

    protected val logRepository = mockk<FlowLogRepository> {
        coEvery { findAfterEventIndex(any(), any(), any(), any()) } returns emptyFlow()
        coEvery { findBeforeEventIndex(any(), any(), any(), any()) } returns emptyFlow()
    }
    protected val properties = mockk<FlowListenerProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }

    protected val transactionHash = "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c"
    protected val blockHash = "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"

    protected val contractSpec = Contracts.HW_GARAGE_CARD
    protected val contract = contractSpec.fqn(properties.chainId)
    protected val contractAddress = contractSpec.deployments[properties.chainId]!!.formatted
    protected val collection = contract
    protected val mintEventType = NonFungibleTokenEventType.MINT.full(contractSpec.fqn(properties.chainId))
    protected val burnEventType = NonFungibleTokenEventType.BURN.full(contractSpec.fqn(properties.chainId))
    protected val depositEventType = NonFungibleTokenEventType.DEPOSIT.full(contractSpec.fqn(properties.chainId))
    protected val withdrawEventType = NonFungibleTokenEventType.WITHDRAW.full(contractSpec.fqn(properties.chainId))
    protected val timestamp = Instant.now()
    protected val blockHeight = 19683033L

    protected val tokenId = 12L
    protected val to = "0xd796ff17107bbff6"
    protected val from = "0x8390ff27117abff6"

    protected val mint = FlowLogEvent(
        FlowLog(
            transactionHash = transactionHash,
            eventIndex = 0,
            eventType = mintEventType,
            timestamp = timestamp,
            blockHeight = blockHeight,
            blockHash = blockHash
        ),
        event = EventMessage(
            EventId.of(mintEventType),
            mapOf(
                "id" to UInt64NumberField(tokenId.toString())
            )
        ),
        type = FlowLogType.MINT
    )
    protected val burn = FlowLogEvent(
        FlowLog(
            transactionHash = transactionHash,
            eventIndex = 1,
            eventType = burnEventType,
            timestamp = timestamp,
            blockHeight = blockHeight,
            blockHash = blockHash
        ),
        event = EventMessage(
            EventId.of(burnEventType),
            mapOf(
                "id" to UInt64NumberField(tokenId.toString())
            )
        ),
        type = FlowLogType.BURN
    )
    protected val deposit = FlowLogEvent(
        FlowLog(
            transactionHash = transactionHash,
            eventIndex = 2,
            eventType = depositEventType,
            timestamp = timestamp,
            blockHeight = blockHeight,
            blockHash = blockHash
        ),
        event = EventMessage(
            EventId.of(depositEventType),
            mapOf(
                "id" to UInt64NumberField(tokenId.toString()),
                "to" to OptionalField(AddressField(to))
            )
        ),
        type = FlowLogType.DEPOSIT
    )
    protected val withdraw = FlowLogEvent(
        FlowLog(
            transactionHash = transactionHash,
            eventIndex = 3,
            eventType = withdrawEventType,
            timestamp = timestamp,
            blockHeight = blockHeight,
            blockHash = blockHash
        ),
        event = EventMessage(
            EventId.of(withdrawEventType),
            mapOf(
                "id" to UInt64NumberField(tokenId.toString()),
                "from" to OptionalField(AddressField(from))
            )
        ),
        type = FlowLogType.WITHDRAW
    )
    protected val withdrawWithoutFrom = FlowLogEvent(
        FlowLog(
            transactionHash = transactionHash,
            eventIndex = 3,
            eventType = withdrawEventType,
            timestamp = timestamp,
            blockHeight = blockHeight,
            blockHash = blockHash
        ),
        event = EventMessage(
            EventId.of(withdrawEventType),
            mapOf(
                "id" to UInt64NumberField(tokenId.toString()),
                "from" to OptionalField(null)
            )
        ),
        type = FlowLogType.WITHDRAW
    )
    protected val depositWithoutTo = FlowLogEvent(
        FlowLog(
            transactionHash = transactionHash,
            eventIndex = 2,
            eventType = depositEventType,
            timestamp = timestamp,
            blockHeight = blockHeight,
            blockHash = blockHash
        ),
        event = EventMessage(
            EventId.of(depositEventType),
            mapOf(
                "id" to UInt64NumberField(tokenId.toString()),
                "to" to OptionalField(null)
            )
        ),
        type = FlowLogType.DEPOSIT
    )
}