package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.scanner.listener.disabled.SoftCollectionEventsListener
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono

internal class SoftCollectionEventsListenerTest : FunSpec({
    val environmentInfo = ApplicationEnvironmentInfo("test", "test")

    test("should parse royalties") {
        SoftCollectionEventsListener(mockk(), FlowChainId.TESTNET, environmentInfo).parseRoyalties(
            Flow.decodeJsonCadence(CHANGED_ROYALTIES)
        ) shouldContainExactly listOf(Part(FlowAddress("0x4895ce5fb8a40f47"), 0.55))
    }

    test("should parse optional royalties") {
        SoftCollectionEventsListener(mockk(), FlowChainId.TESTNET, environmentInfo).parseRoyalties(
            Flow.decodeJsonCadence("""{"type": "Optional", "value": $CHANGED_ROYALTIES}""")
        ) shouldContainExactly listOf(Part(FlowAddress("0x4895ce5fb8a40f47"), 0.55))
    }

    test("should update collection") {
        val collectionId = ItemId(Contracts.SOFT_COLLECTION.fqn(FlowChainId.TESTNET), 15, '.')
        val repo = mockk<ItemCollectionRepository>() {
            every {
                findById("$collectionId")
            } returns Mono.just(
                ItemCollection(
                    name = "initial",
                    owner = FlowAddress("0x01"),
                    id = collectionId.toString(),
                    symbol = "INIT",
                    royalties = emptyList(),
                )
            )

            every {
                save(any())
            } answers { Mono.just(arg(0)) }
        }
        SoftCollectionEventsListener(
            repo,
            FlowChainId.TESTNET,
            environmentInfo
        ).updateSoftCollection(CHANGED_EVENT)

        verify {
            repo.findById(collectionId.toString())
            repo.save(
                withArg { updated ->
                    updated.royalties shouldContainExactly listOf(Part(FlowAddress("0x4895ce5fb8a40f47"), 0.55))
                    updated.name shouldBe "kfkkf_22"
                }
            )
        }
    }
}) {

    companion object {

        const val CHANGED_ROYALTIES = """
            {
                "type": "Array",
                "value": [{
                    "type": "Struct",
                    "value": {
                        "id": "A.ebf4ae01d1284af8.SoftCollection.Royalty",
                        "fields": [{
                            "name": "address",
                            "value": {
                                "type": "Address",
                                "value": "0x4895ce5fb8a40f47"
                            }
                        }, {
                            "name": "fee",
                            "value": {
                                "type": "UFix64",
                                "value": "0.55000000"
                            }
                        }]
                    }
                }]
            }
        """

        const val CHANGED_META = """{
            "type": "Struct",
            "value": {
                "id": "A.ebf4ae01d1284af8.SoftCollection.Meta",
                "fields": [{
                    "name": "name",
                    "value": {
                        "type": "String",
                        "value": "kfkkf_22"
                    }
                }, {
                    "name": "symbol",
                    "value": {
                        "type": "String",
                        "value": "kfkkf_22"
                    }
                }, {
                    "name": "icon",
                    "value": {
                        "type": "Optional",
                        "value": null
                    }
                }, {
                    "name": "description",
                    "value": {
                        "type": "Optional",
                        "value": null
                    }
                }, {
                    "name": "url",
                    "value": {
                        "type": "Optional",
                        "value": null
                    }
                }]
            }
        }"""

        val CHANGED_EVENT = FlowLogEvent(
            type = FlowLogType.COLLECTION_CHANGE,
            log = mockk {
                every { transactionHash } returns "tx"
                every { eventIndex } returns 0
            },
            event = EventMessage(
                EventId.of("A.ebf4ae01d1284af8.SoftCollection.Changed"),
                mapOf(
                    "id" to UInt64NumberField("15"),
                    "meta" to Flow.decodeJsonCadence(CHANGED_META),
                    "royalties" to Flow.decodeJsonCadence(CHANGED_ROYALTIES)
                )
            )
        )
    }
}
