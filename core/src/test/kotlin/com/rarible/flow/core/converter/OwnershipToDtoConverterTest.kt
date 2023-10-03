package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Ownership
import com.rarible.protocol.dto.PayInfoDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class OwnershipToDtoConverterTest : FunSpec({

    test("should convert ownership to dto") {
        OwnershipToDtoConverter.convert(
            Ownership(
                contract = "A.B.C",
                tokenId = 1337L,
                owner = FlowAddress("0x01"),
                creator = FlowAddress("0x1337"),
                date = Instant.now()
            )
        ) should { o ->
            o.owner shouldBe "0x0000000000000001"
            o.tokenId shouldBe BigInteger.valueOf(1337L)
            o.contract shouldBe "A.B.C"
            o.id shouldBe "A.B.C:1337:0x0000000000000001"
            o.creators shouldContain PayInfoDto("0x0000000000001337", BigDecimal.ONE)
        }
    }
})
