package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

class VersusArtMetaProviderTest : FunSpec({

    test("should read ipfs metadata") {
        val itemId = ItemId("A.d796ff17107bbff6.Art", 42)
        val provider = VersusArtMetaProvider(
            mockk("itemRepository") {
                every { findById(any<ItemId>()) } returns Mono.just(
                    mockk("item") {
                        every { id } returns itemId
                        every { owner } returns FlowAddress("0xd796ff17107bbff6")
                        every { tokenId } returns 740
                    }
                )
            },
            mocks.scriptExecutor("getMetadataScript" to META1, "getContentScript" to CONTENT1),
            mocks.resource("getMetadataScript"),
            mocks.resource("getContentScript")
        )

        provider.getMeta(itemId) should { meta ->
            meta.itemId shouldBe itemId
            meta.name shouldBe "The Illuminator"
            meta.contentUrls.first() shouldBe "https://rarible.mypinata.cloud/ipfs/QmVWf7e3bkEvmy5jGHLCgeZmwPyqm5hKeQ72NAdDwyJm2Q"
        }
    }

    test("should read dataurl metadata") {
        val itemId = ItemId("A.d796ff17107bbff6.Art", 821)
        val provider = VersusArtMetaProvider(
            mockk("itemRepository") {
                every { findById(any<ItemId>()) } returns Mono.just(
                    mockk("item") {
                        every { id } returns itemId
                        every { owner } returns FlowAddress("0x65f12353ccc255ee")
                        every { tokenId } returns 821
                    }
                )
            },
            mocks.scriptExecutor("getMetadataScript" to META2, "getContentScript" to CONTENT2),
            mocks.resource("getMetadataScript"),
            mocks.resource("getContentScript")
        )

        provider.getMeta(itemId) should { meta ->
            meta.itemId shouldBe itemId
            meta.name shouldBe "JOYWORLD Portal, Sandstone Headland"
            meta.contentUrls.first() shouldBe "data:image/jpeg;base64, /9j/4QGWRXhpZ...y/rsc8vedj//2Q=="
        }
    }
}) {
    companion object {
        const val META1 =
            """{"type":"Optional","value":{"type":"Resource","value":{"id":"A.d796ff17107bbff6.Art.NFT","fields":[{"name":"uuid","value":{"type":"UInt64","value":"76288599"}},{"name":"id","value":{"type":"UInt64","value":"740"}},{"name":"name","value":{"type":"String","value":"The Illuminator"}},{"name":"description","value":{"type":"String","value":"Initially called 'Soul', 'The Illuminator' came to Instagram in animation with the soundtrack 'Everlasting' by Loyal Lobos and became a very popular work among lovers of Machado Leão's art in India. The artist believes in the innocent depth of this art and attributes its success to it."}},{"name":"schema","value":{"type":"Optional","value":null}},{"name":"contentCapability","value":{"type":"Optional","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"private","identifier":"versusContentCollection"}},"address":"0xd796ff17107bbff6","borrowType":"\u0026A.d796ff17107bbff6.Content.Collection"}}}},{"name":"contentId","value":{"type":"Optional","value":{"type":"UInt64","value":"41"}}},{"name":"url","value":{"type":"Optional","value":null}},{"name":"metadata","value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Metadata","fields":[{"name":"name","value":{"type":"String","value":"The Illuminator"}},{"name":"artist","value":{"type":"String","value":"Machado Leão"}},{"name":"artistAddress","value":{"type":"Address","value":"0xbb74ee6b54a3db08"}},{"name":"description","value":{"type":"String","value":"Initially called 'Soul', 'The Illuminator' came to Instagram in animation with the soundtrack 'Everlasting' by Loyal Lobos and became a very popular work among lovers of Machado Leão's art in India. The artist believes in the innocent depth of this art and attributes its success to it."}},{"name":"type","value":{"type":"String","value":"ipfs/video"}},{"name":"edition","value":{"type":"UInt64","value":"14"}},{"name":"maxEdition","value":{"type":"UInt64","value":"15"}}]}}},{"name":"royalty","value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"minter"},"value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Royalty","fields":[{"name":"wallet","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"public","identifier":"flowTokenReceiver"}},"address":"0xd796ff17107bbff6","borrowType":"\u0026AnyResource{A.f233dcee88fe0abe.FungibleToken.Receiver}"}}},{"name":"cut","value":{"type":"UFix64","value":"0.02500000"}}]}}},{"key":{"type":"String","value":"artist"},"value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Royalty","fields":[{"name":"wallet","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"public","identifier":"flowTokenReceiver"}},"address":"0xbb74ee6b54a3db08","borrowType":"\u0026AnyResource{A.f233dcee88fe0abe.FungibleToken.Receiver}"}}},{"name":"cut","value":{"type":"UFix64","value":"0.05000000"}}]}}}]}}]}}}"""
        const val CONTENT1 =
            """{"type":"Optional","value":{"type":"String","value":"QmVWf7e3bkEvmy5jGHLCgeZmwPyqm5hKeQ72NAdDwyJm2Q"}}"""
        const val META2 =
            """{"type":"Optional","value":{"type":"Resource","value":{"id":"A.d796ff17107bbff6.Art.NFT","fields":[{"name":"uuid","value":{"type":"UInt64","value":"85226085"}},{"name":"id","value":{"type":"UInt64","value":"821"}},{"name":"name","value":{"type":"String","value":"JOYWORLD Portal, Sandstone Headland"}},{"name":"description","value":{"type":"String","value":"GM from JOY lingering near the precipice of the JOYWORLD Portal at Sandstone Headland. Popping into existence only for an instance this JOYWORLD Portal spit out a few JOYs and swallowed some JOY Collectors whole. Taking them down the trip through JOYWORLD, never to return from JOYfull bliss."}},{"name":"schema","value":{"type":"Optional","value":null}},{"name":"contentCapability","value":{"type":"Optional","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"private","identifier":"versusContentCollection"}},"address":"0xd796ff17107bbff6","borrowType":"\u0026A.d796ff17107bbff6.Content.Collection"}}}},{"name":"contentId","value":{"type":"Optional","value":{"type":"UInt64","value":"47"}}},{"name":"url","value":{"type":"Optional","value":null}},{"name":"metadata","value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Metadata","fields":[{"name":"name","value":{"type":"String","value":"JOYWORLD Portal, Sandstone Headland"}},{"name":"artist","value":{"type":"String","value":"JOY"}},{"name":"artistAddress","value":{"type":"Address","value":"0x68f692175fff34e5"}},{"name":"description","value":{"type":"String","value":"GM from JOY lingering near the precipice of the JOYWORLD Portal at Sandstone Headland. Popping into existence only for an instance this JOYWORLD Portal spit out a few JOYs and swallowed some JOY Collectors whole. Taking them down the trip through JOYWORLD, never to return from JOYfull bliss."}},{"name":"type","value":{"type":"String","value":"image/dataurl"}},{"name":"edition","value":{"type":"UInt64","value":"3"}},{"name":"maxEdition","value":{"type":"UInt64","value":"11"}}]}}},{"name":"royalty","value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"artist"},"value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Royalty","fields":[{"name":"wallet","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"public","identifier":"flowTokenReceiver"}},"address":"0x68f692175fff34e5","borrowType":"\u0026AnyResource{A.f233dcee88fe0abe.FungibleToken.Receiver}"}}},{"name":"cut","value":{"type":"UFix64","value":"0.05000000"}}]}}},{"key":{"type":"String","value":"minter"},"value":{"type":"Struct","value":{"id":"A.d796ff17107bbff6.Art.Royalty","fields":[{"name":"wallet","value":{"type":"Capability","value":{"path":{"type":"Path","value":{"domain":"public","identifier":"flowTokenReceiver"}},"address":"0xd796ff17107bbff6","borrowType":"\u0026AnyResource{A.f233dcee88fe0abe.FungibleToken.Receiver}"}}},{"name":"cut","value":{"type":"UFix64","value":"0.02500000"}}]}}}]}}]}}}"""
        const val CONTENT2 =
            """{"type":"Optional","value":{"type":"String","value":"data:image/jpeg;base64, /9j/4QGWRXhpZ...y/rsc8vedj//2Q=="}}"""
    }
}
