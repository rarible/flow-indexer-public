package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.DepositActivity
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.WithdrawnActivity
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import java.time.Instant

internal class ItemHistoryToDtoConverterTest: FunSpec({

    val convert = ItemHistoryToDtoConverter::convert
    val date = Instant.now()

    test("should convert Mint activity") {
        convert(
            createItemHistory(date, mintActivity(date))
        ) shouldBe FlowMintDto(
            owner = FlowAddress("0x01").formatted,
            contract = "Rarible",
            tokenId = 1337.toBigInteger(),
            value = BigInteger.ONE,
            "tx_hash",
            "block_hash",
            12345,
            10,
            "tx_hash.10",
            date
        )
    }

    test("should convert Burn activity") {
        convert(
            createItemHistory(date, burnActivity(date))
        ) shouldBe FlowBurnDto(
            owner = FlowAddress("0x01").formatted,
            contract = "Rarible",
            tokenId = 1337.toBigInteger(),
            value = BigInteger.ONE,
            "tx_hash",
            "block_hash",
            12345,
            10,
            "tx_hash.10",
            date
        )
    }

    test("should skip Deposit") {
        convert(
            createItemHistory(date, depositActivity(date))
        ) shouldBe null
    }

    test("should skip Withdraw") {
        convert(
            createItemHistory(date, withdrawActivity(date))
        ) shouldBe null
    }

})

private fun createItemHistory(date: Instant, activity: BaseActivity): ItemHistory {
    return ItemHistory(
        date,
        log = FlowLog(
            "tx_hash", Log.Status.CONFIRMED, 10,
            "A.EventType", date, 12345, "block_hash"
        ),
        activity = activity
    )
}

private fun mintActivity(date: Instant) = MintActivity(
    owner = FlowAddress("0x01").formatted,
    contract = "Rarible",
    tokenId = 1337,
    timestamp = date,
    royalties = listOf(
        Part(FlowAddress("0x02"), 0.1)
    ),
    metadata = mapOf()
)

private fun burnActivity(date: Instant) = BurnActivity(
    owner = FlowAddress("0x01").formatted,
    contract = "Rarible",
    tokenId = 1337,
    timestamp = date
)

private fun depositActivity(date: Instant) = DepositActivity(
    contract = "Rarible",
    tokenId = 1337,
    timestamp = date,
    to = FlowAddress("0x42").formatted
)

private fun withdrawActivity(date: Instant) = WithdrawnActivity(
    contract = "Rarible",
    tokenId = 1337,
    timestamp = date,
    from = FlowAddress("0x42").formatted
)