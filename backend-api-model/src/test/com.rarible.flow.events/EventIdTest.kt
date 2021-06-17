package com.rarible.flow.events


import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import org.junit.jupiter.api.Test
import org.onflow.sdk.Address

class EventIdTest: FunSpec({

    test("EventId de-/serialization") {
        val str = "A.0b2a3299cc857e29.TopShot.MomentMinted"
        val id = EventId("A", Address("0b2a3299cc857e29"), "TopShot", "MomentMinted")

        EventId.of(str) shouldBeEqualToComparingFields id
        id.toString() shouldBeEqualComparingTo str
    }
})