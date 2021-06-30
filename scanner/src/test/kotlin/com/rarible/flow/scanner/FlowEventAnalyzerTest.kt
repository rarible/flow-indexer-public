package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.test.kafka.SuccessfulProducer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import java.time.Instant

internal class FlowEventAnalyzerTest: FunSpec({

    val kafkaSuccess = SuccessfulProducer<EventMessage>()
    val mapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    val analyzer = FlowEventAnalyzer(
        kafkaSuccess,
        mapper,
        listOf("1cd85950d20f05b2")
    )

    test("should track events") {

        val event = FlowEvent(
            "A.1cd85950d20f05b2.NFTProvider.Withdraw",
            "",
            Instant.now()
        )

        val event2 = FlowEvent(
            "A.2cd85950d20f05b2.NFTProvider.Withdraw",
            "",
            Instant.now()
        )

        analyzer.isEventTracked(event).shouldBeTrue()
        analyzer.isEventTracked(event2).shouldBeFalse()
    }

    test("should make one kafka message") {
        val event = FlowEvent(
            "A.1cd85950d20f05b2.NFTProvider.Withdraw",
            """
                {
                    "id": 1001,
                    "fields": {},
                    "timestamp": "${Instant.now()}"
                }
            """.trimIndent(),
            Instant.now()
        )

        val event2 = FlowEvent(
            "A.2cd85950d20f05b2.NFTProvider.Withdraw",
            """
                {
                    "id": 2002,
                    "fields": {},
                    "timestamp": "${Instant.now()}"
                }
            """.trimIndent(),
            Instant.now()
        )

        analyzer.makeKafkaMessages("123", listOf(event, event2)).shouldBeSingleton {
            it.id shouldBeEqualIgnoringCase "123.0"
            it.value.id shouldBeEqualIgnoringCase "1001"
        }
    }
})