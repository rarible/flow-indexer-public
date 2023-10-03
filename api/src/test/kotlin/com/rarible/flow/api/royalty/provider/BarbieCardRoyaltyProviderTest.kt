package com.rarible.flow.api.royalty.provider

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowScriptResponse
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BarbieCardRoyaltyProviderTest {
    private val apiProperties = mockk<ApiProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }
    private val executor = mockk<ScriptExecutor>()

    private val provider = BarbieCardRoyaltyProvider(apiProperties, executor)

    @Test
    fun `get royalty - ok`() = runBlocking<Unit> {
        coEvery { executor.execute(any(), any()) } returns FlowScriptResponse(royaltyJson.toByteArray())
        val item = mockk<Item> {
            every { owner } returns FlowAddress("0xfe43a2bd8f799f84")
            every { tokenId } returns 33694
        }
        val royalties = provider.getRoyalties(item)
        Assertions.assertThat(royalties).isNotEmpty
        Assertions.assertThat(royalties[0].address).isEqualTo("0xf86e2f015cd692be")
        Assertions.assertThat(royalties[0].fee).isEqualTo(BigDecimal("0.05"))
    }

    private val royaltyJson = """
        {
            "value": {
                "id": "s.eeff6173bde2f47643fcf904f4208d50190ff915ab6f594f2e2e8f7b68be27ec.NFTView",
                "fields": [
                    {
                        "value": {
                            "value": "33694",
                            "type": "UInt64"
                        },
                        "name": "id"
                    },
                    {
                        "value": {
                            "value": "1002376670",
                            "type": "UInt64"
                        },
                        "name": "uuid"
                    },
                    {
                        "value": {
                            "value": "Hot Wheels Garage Pack Series 5 #33694",
                            "type": "String"
                        },
                        "name": "name"
                    },
                    {
                        "value": {
                            "value": [
                                {
                                    "value": {
                                        "id": "A.1d7e57aa55817448.MetadataViews.Royalty",
                                        "fields": [
                                            {
                                                "value": {
                                                    "value": {
                                                        "path": {
                                                            "value": {
                                                                "domain": "public",
                                                                "identifier": "flowTokenReceiver"
                                                            },
                                                            "type": "Path"
                                                        },
                                                        "borrowType": {
                                                            "type": {
                                                                "kind": "Restriction",
                                                                "typeID": "AnyResource{A.f233dcee88fe0abe.FungibleToken.Receiver}",
                                                                "type": {
                                                                    "kind": "AnyResource"
                                                                },
                                                                "restrictions": [
                                                                    {
                                                                        "type": "",
                                                                        "kind": "ResourceInterface",
                                                                        "typeID": "A.f233dcee88fe0abe.FungibleToken.Receiver",
                                                                        "fields": [
                                                                            {
                                                                                "type": {
                                                                                    "kind": "UInt64"
                                                                                },
                                                                                "id": "uuid"
                                                                            }
                                                                        ],
                                                                        "initializers": []
                                                                    }
                                                                ]
                                                            },
                                                            "kind": "Reference",
                                                            "authorized": false
                                                        },
                                                        "address": "0xf86e2f015cd692be"
                                                    },
                                                    "type": "Capability"
                                                },
                                                "name": "receiver"
                                            },
                                            {
                                                "value": {
                                                    "value": "0.05000000",
                                                    "type": "UFix64"
                                                },
                                                "name": "cut"
                                            },
                                            {
                                                "value": {
                                                    "value": "Mattel 5% Royalty",
                                                    "type": "String"
                                                },
                                                "name": "description"
                                            }
                                        ]
                                    },
                                    "type": "Struct"
                                }
                            ],
                            "type": "Array"
                        },
                        "name": "royalties"
                    }
                ]
            },
            "type": "Struct"
        }
    """.trimIndent()
}
