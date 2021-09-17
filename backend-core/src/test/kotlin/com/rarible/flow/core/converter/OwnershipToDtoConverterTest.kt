package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Payout
import com.rarible.protocol.dto.PayInfoDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

internal class OwnershipToDtoConverterTest: FunSpec({

    test("should convert ownership to dto") {
        OwnershipToDtoConverter.convert(
            Ownership(
                contract = "A.B.C",
                tokenId = 1337L,
                owner = FlowAddress("0x01"),
                date = Instant.now(),
                creators = listOf(
                    Payout(FlowAddress("0x1337"), 50.toBigDecimal()),
                    Payout(FlowAddress("0x1338"), 40.50.toBigDecimal()),
                    Payout(FlowAddress("0x1339"), 9.50.toBigDecimal()),
                )
            )
        ) should { o ->
            o.owner shouldBe "0x0000000000000001"
            o.tokenId shouldBe 1337L
            o.contract shouldBe "A.B.C"
            o.id shouldBe "A.B.C:1337:0x0000000000000001"
            o.creators shouldContainAll listOf(
                PayInfoDto("0x0000000000001337", 50.toBigDecimal()),
                PayInfoDto("0x0000000000001338", 40.50.toBigDecimal()),
                PayInfoDto("0x0000000000001339", 9.50.toBigDecimal()),
            )
        }
    }
})