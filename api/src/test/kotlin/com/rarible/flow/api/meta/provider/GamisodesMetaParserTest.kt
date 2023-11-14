package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.royalty.provider.Royalty
import com.rarible.flow.core.test.randomItemId
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GamisodesMetaParserTest {

    fun json(path: String) = this.javaClass.getResourceAsStream(path)!!
        .bufferedReader().use { it.readText() }

    @Test
    fun `parse meta - ok`() = runBlocking<Unit> {
        val json = json("/json/gamisodes_meta.json")
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
        assertThat(meta.setId).isEqualTo("15")
        assertThat(meta.templateId).isEqualTo("0")
    }

    @Test
    fun `parse attributes - ok`() = runBlocking<Unit> {
        val json = json("/json/gamisodes_meta_attr.json")

        val attributes = GamisodesMetaParser.parseAttributes(json, randomItemId())
        assertThat(attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute(key = "platform", value = "Gamisodes"),
            ItemMetaAttribute(key = "mintLevel", value = "1"),
            ItemMetaAttribute(key = "collection", value = "Gadgets"),
            ItemMetaAttribute(key = "rank", value = "1"),
            ItemMetaAttribute(key = "type", value = "Trading Card"),
            ItemMetaAttribute(key = "property", value = "Inspector Gadget"),
            ItemMetaAttribute(key = "editionSize", value = "72"),
            ItemMetaAttribute(key = "artist", value = "Bayu Sadewo"),
            ItemMetaAttribute(key = "series", value = "1"),
            ItemMetaAttribute(key = "mediaUrl", value = "ipfs://bafybeie33roqkg2otsf2f2n5vrsymn6ckvnqnwu677jvj6yoxhn27unw2e/GadgetsToothpaste_1_1.png"),
            ItemMetaAttribute(key = "posterUrl", value = "ipfs://bafybeig2d4vr45iycb6s5i24t4amk2k3qx63dqhqtsvks7u3jfslhxsjni/GadgetsToothpaste_1_1_Thumbnail.png")
        )
    }
}
