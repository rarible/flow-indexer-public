package com.rarible.flow.listener

import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.EventHandler

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.time.LocalDateTime


//todo add more message types
class EventMessageConverterTest: FunSpec({

    test("should convert mint message") {
        val message = EventMessage(
            "A.1cd85950d20f05b2.NFTProvider.Mint",
            mapOf("id" to "71", "to"  to "0x482c68c2015dc1d8"),
            LocalDateTime.parse("2021-07-02T08:03:43.860754688")
        )
        val nftMessage = message.convert()
        nftMessage.shouldNotBe(null)
        nftMessage!!.should {
            it.eventId.eventName shouldBe "Mint"
        }
    }

})