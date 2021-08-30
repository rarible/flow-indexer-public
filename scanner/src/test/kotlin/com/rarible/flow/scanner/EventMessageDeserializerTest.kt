package com.rarible.flow.scanner

import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.json.commonMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class EventMessageDeserializerTest {

    private val mapper = commonMapper()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.e3750a9bc4137f3f.DisruptNow.Withdraw\",\"fields\":[{\"name\":\"id\",\"value\":{\"type\":\"UInt64\",\"value\":\"34\"}},{\"name\":\"from\",\"value\":{\"type\":\"Optional\",\"value\":{\"type\":\"Address\",\"value\":\"0x9b86289236e7fe76\"}}}]}}",
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.e3750a9bc4137f3f.Marketplace.ForSale\",\"fields\":[{\"name\":\"id\",\"value\":{\"type\":\"UInt64\",\"value\":\"34\"}},{\"name\":\"price\",\"value\":{\"type\":\"UFix64\",\"value\":\"1.00000000\"}}]}}",
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.7e60df042a9c0868.FlowToken.TokensWithdrawn\",\"fields\":[{\"name\":\"amount\",\"value\":{\"type\":\"UFix64\",\"value\":\"0.00010000\"}},{\"name\":\"from\",\"value\":{\"type\":\"Optional\",\"value\":{\"type\":\"Address\",\"value\":\"0xf086a545ce3c552d\"}}}]}}",
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.e4e5f90bf7e2a25f.NFTProvider.Mint\",\"fields\":[{\"name\":\"id\",\"value\":{\"type\":\"UInt64\",\"value\":\"211\"}},{\"name\":\"collection\",\"value\":{\"type\":\"String\",\"value\":\"A.e4e5f90bf7e2a25f.NFTProvider.NFT\"}},{\"name\":\"creator\",\"value\":{\"type\":\"Address\",\"value\":\"0xf23ff23d90720ab4\"}},{\"name\":\"royalties\",\"value\":{\"type\":\"Array\",\"value\":[{\"type\":\"Struct\",\"value\":{\"id\":\"A.e4e5f90bf7e2a25f.NFTProvider.Royalties\",\"fields\":[{\"name\":\"address\",\"value\":{\"type\":\"Address\",\"value\":\"0xf23ff23d90720ab4\"}},{\"name\":\"fee\",\"value\":{\"type\":\"UInt8\",\"value\":\"10\"}}]}}]}},{\"name\":\"metadata\",\"value\":{\"type\":\"Struct\",\"value\":{\"id\":\"A.e4e5f90bf7e2a25f.NFTProvider.Metadata\",\"fields\":[{\"name\":\"uri\",\"value\":{\"type\":\"String\",\"value\":\"https://i.pinimg.com/originals/65/17/22/651722a77be5cd72e194660e264896b8.png\"}},{\"name\":\"title\",\"value\":{\"type\":\"String\",\"value\":\"A.e4e5f90bf7e2a25f.NFTProvider.NFT\"}},{\"name\":\"description\",\"value\":{\"type\":\"Optional\",\"value\":{\"type\":\"String\",\"value\":\"A.e4e5f90bf7e2a25f.NFTProvider.NFT\"}}},{\"name\":\"properties\",\"value\":{\"type\":\"Dictionary\",\"value\":[]}}]}}}]}}\n",
            """
                {"type":"Event","value":{"id":"A.665b9acf64dfdfdb.NFTStorefront.SaleOfferAvailable","fields":[{"name":"storefrontAddress","value":{"type":"Address","value":"0xaf405c429709b1ae"}},{"name":"saleOfferResourceID","value":{"type":"UInt64","value":"11947772"}},{"name":"nftType","value":{"type":"Type","value":{"staticType":"A.665b9acf64dfdfdb.CommonNFT.NFT"}}},{"name":"nftID","value":{"type":"UInt64","value":"6"}},{"name":"ftVaultType","value":{"type":"Type","value":{"staticType":"A.7e60df042a9c0868.FlowToken.Vault"}}},{"name":"price","value":{"type":"UFix64","value":"2.05000000"}}]}}
            """

        ]
    )
    fun deserializeEventWithFieldsTest(source: String) {
        val raw = mapper.readTree(source)
        val expectedId = EventId.of(raw["value"]["id"].asText())
        val message = mapper.readValue<EventMessage>(source)
        message shouldNotBe null
        message.eventId shouldBe expectedId
        message.fields.size shouldNotBe 0
    }

    @Test
    fun shouldParseRoyalties() {
        val str = """
            {"type":"Event","value":{"id":"A.e91e497115b9731b.CommonNFT.Mint","fields":[{"name":"id","value":{"type":"UInt64","value":"14"}},{"name":"collection","value":{"type":"String","value":"A.e91e497115b9731b.CommonNFT.NFT"}},{"name":"creator","value":{"type":"Address","value":"0x7ec498ace78086cb"}},{"name":"metadata","value":{"type":"String","value":"url://abc"}},{"name":"royalties","value":{"type":"Array","value":[{"type":"Struct","value":{"id":"A.e91e497115b9731b.CommonNFT.Royalties","fields":[{"name":"address","value":{"type":"Address","value":"0x9aa32171f67a8614"}},{"name":"fee","value":{"type":"UFix64","value":"2.00000000"}}]}},{"type":"Struct","value":{"id":"A.e91e497115b9731b.CommonNFT.Royalties","fields":[{"name":"address","value":{"type":"Address","value":"0xe91e497115b9731b"}},{"name":"fee","value":{"type":"UFix64","value":"5.00000000"}}]}}]}}]}}
        """.trimIndent()
        val message = mapper.readValue<EventMessage>(str)
        message shouldNotBe null
        val royalties = message.fields["royalties"] as List<Map<String, String>>
        royalties[0]["address"] shouldBe "0x9aa32171f67a8614"
        royalties[0]["fee"]?.toDouble() shouldBe 2.0
        royalties[1]["address"] shouldBe "0xe91e497115b9731b"
        royalties[1]["fee"]?.toDouble() shouldBe 5.0
    }
}
