package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.royalty.provider.Royalty
import com.rarible.flow.core.test.randomItemId
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GamisodesMetaParserTest {

    private val json = this.javaClass.getResourceAsStream("/json/gamisodes_meta.json")!!
        .bufferedReader().use { it.readText() }

    @Test
    fun `parse meta - ok`() = runBlocking<Unit> {
        val meta = GamisodesMetaParser.parse(json, randomItemId())!!

        assertThat(meta.name).isEqualTo("Arms")
        assertThat(meta.description).isEqualTo("Go Go Gadget Arms")
        assertThat(meta.externalUri).isEqualTo("https://gamisodes.com")
        assertThat(meta.imageOriginal).isEqualTo("https://cf-ipfs.com/ipfs/QmYdtHThthkMLZTyfNoyhPZZtzMagRbGoaE6jDyTYoKaxQ")
        assertThat(meta.imagePreview).isEqualTo("https://storage.googleapis.com/gamisodes_inspector_gadget_trading_cards/Arms_1_1_Thumbnail.png")
        assertThat(meta.attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute(key = "mimetype", value = "image/png"),
            ItemMetaAttribute(key = "nftType", value = "Type A"),
            ItemMetaAttribute(key = "platform", value = "Gamisodes"),
            ItemMetaAttribute(key = "mintLevel", value = "1"),
            ItemMetaAttribute(key = "rank", value = "1"),
            ItemMetaAttribute(key = "level", value = "1"),
            ItemMetaAttribute(key = "property", value = "Inspector Gadget"),
            ItemMetaAttribute(key = "series", value = "Gadgets"),
            ItemMetaAttribute(key = "rarity", value = "Common"),
            ItemMetaAttribute(key = "number", value = "14250"),
            ItemMetaAttribute(key = "max", value = "27906"),
            ItemMetaAttribute(
                key = "decentralizedMediaFiles",
                value = "https://cf-ipfs.com/ipfs/QmYdtHThthkMLZTyfNoyhPZZtzMagRbGoaE6jDyTYoKaxQ"
            ),
            ItemMetaAttribute(
                key = "mediaURL",
                value = "https://storage.googleapis.com/gamisodes_inspector_gadget_trading_cards/Arms_1_1.png"
            ),
        )
        assertThat(meta.royalties).containsExactlyInAnyOrder(
            Royalty(address = "0xb780208cb751b1ae", fee = "0.02500000".toBigDecimal()),
            Royalty(address = "0xa83a288bd044926e", fee = "0.02500000".toBigDecimal())
        )
    }
}
