package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

internal class ContinuationsTest: FunSpec({

    test("should parse CNN continuations") {
        val primary = Instant.ofEpochMilli(1628787662840)
        val secondary = ItemId("A.329feb3ab062d289.CNN_NFT", 3231)
        Cont.asc<Instant, ItemId>("1628787662840_A.329feb3ab062d289.CNN_NFT:3231") shouldBe Cont.AscCont(
            primary,
            secondary
        )

        Cont.desc<Instant, ItemId>("1628787662840_A.329feb3ab062d289.CNN_NFT:3231") shouldBe Cont.DescCont(
            primary,
            secondary
        )
    }

    test("should parse arbitrary continuations") {
        val primary = Instant.ofEpochMilli(1628787662840)
        val secondary = ItemId("A.329feb3ab062d289.Contract", 3231)
        Cont.asc<Instant, ItemId>("1628787662840_A.329feb3ab062d289.Contract:3231") shouldBe Cont.AscCont(
            primary,
            secondary
        )

        Cont.asc<Instant, ItemId>("1628787662840_A.329feb3ab062d289.Contract:3231") shouldBe Cont.AscCont(
            Instant.ofEpochMilli(1628787662840),
            secondary
        )
    }

})