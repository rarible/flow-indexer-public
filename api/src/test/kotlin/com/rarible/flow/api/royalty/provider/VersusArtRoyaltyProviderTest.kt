package com.rarible.flow.api.royalty.provider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.ZonedDateTime

class VersusArtRoyaltyProviderTest : FunSpec({
    test("should read royalties") {
        val provider = VersusArtRoyaltyProvider(
            mocks.scriptExecutor("getMetadataScript" to META),
            mocks.resource("getMetadataScript"),
        )
        val item = Item(
            "A.d796ff17107bbff6.Art",
            740,
            FlowAddress("0xbb74ee6b54a3db08"),
            listOf(),
            FlowAddress("0xd796ff17107bbff6"),
            ZonedDateTime.parse("2021-11-06T20:27:33.283Z").toInstant(),
            "{\"name\":\"The Illuminator\",\"artist\":\"Machado Leão\",\"artistAddress\":\"0xbb74ee6b54a3db08\",\"description\":\"Initially called 'Soul', 'The Illuminator' came to Instagram in animation with the soundtrack 'Everlasting' by Loyal Lobos and became a very popular work among lovers of Machado Leão's art in India. The artist believes in the innocent depth of this art and attributes its success to it.\",\"type\":\"ipfs/video\",\"edition\":\"14\",\"maxEdition\":\"15\"}",
            "A.d796ff17107bbff6.Art",
            ZonedDateTime.parse("2021-11-06T20:27:33.283Z").toInstant(),
        )
        provider.getRoyalties(item) should { royalties ->
            royalties shouldBe listOf(
                Royalty(address = "0xd796ff17107bbff6", fee = BigDecimal("0.02500000")),
                Royalty(address = "0xbb74ee6b54a3db08", fee = BigDecimal("0.05000000")),
            )
        }
    }
}) {
    companion object {
        const val META =
            """{"type":"Optional","value":{"type":"Resource","value":{"id":"A.d796ff17107bbff6.Art.NFT","fields":[{"name":"uuid","value":{"type":"UInt64","value":"76288599"}},{"name":"id","value":{"type":"UInt64","value":"740"}},{"name":"name","value":{"type":"String","value":"The Illuminator"}},{"name":"description","value":{"type":"String","value":"Initially called 'Soul', 'The Illuminator' came to Instagram in animation with the soundtrack 'Everlasting' by Loyal Lobos and became a very popular work among lovers of Machado Leão's art in India. The artist believes in the innocent depth of this art and attributes its success to it."}},{"name":"schema","value":{"type":"Optional","value":null}},{"name":"contentCapability","value":{"type":"Optional","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"private","identifier":"versusContentCollection"}},"address":"0xd796ff17107bbff6","borrowType":"\u0026A.d796ff17107bbff6.Content.Collection"}}}},{"name":"contentId","value":{"type":"Optional","value":{"type":"UInt64","value":"41"}}},{"name":"url","value":{"type":"Optional","value":null}},{"name":"metadata","value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Metadata","fields":[{"name":"name","value":{"type":"String","value":"The Illuminator"}},{"name":"artist","value":{"type":"String","value":"Machado Leão"}},{"name":"artistAddress","value":{"type":"Address","value":"0xbb74ee6b54a3db08"}},{"name":"description","value":{"type":"String","value":"Initially called 'Soul', 'The Illuminator' came to Instagram in animation with the soundtrack 'Everlasting' by Loyal Lobos and became a very popular work among lovers of Machado Leão's art in India. The artist believes in the innocent depth of this art and attributes its success to it."}},{"name":"type","value":{"type":"String","value":"ipfs/video"}},{"name":"edition","value":{"type":"UInt64","value":"14"}},{"name":"maxEdition","value":{"type":"UInt64","value":"15"}}]}}},{"name":"royalty","value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"minter"},"value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Royalty","fields":[{"name":"wallet","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"public","identifier":"flowTokenReceiver"}},"address":"0xd796ff17107bbff6","borrowType":"\u0026AnyResource{A.f233dcee88fe0abe.FungibleToken.Receiver}"}}},{"name":"cut","value":{"type":"UFix64","value":"0.02500000"}}]}}},{"key":{"type":"String","value":"artist"},"value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Royalty","fields":[{"name":"wallet","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"public","identifier":"flowTokenReceiver"}},"address":"0xbb74ee6b54a3db08","borrowType":"\u0026AnyResource{A.f233dcee88fe0abe.FungibleToken.Receiver}"}}},{"name":"cut","value":{"type":"UFix64","value":"0.05000000"}}]}}}]}}]}}}"""
    }
}
