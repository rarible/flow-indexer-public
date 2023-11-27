package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.royalty.provider.Royalty
import com.rarible.flow.core.test.randomItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GamisodesMetaParserTest {

    fun json(path: String) = this.javaClass.getResourceAsStream(path)!!
        .bufferedReader().use { it.readText() }

    @Test
    fun `parse meta - ok`() {
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
            ItemMetaAttribute(
                key = "serialNumber",
                value = "14250"
            ),
        )
        assertThat(meta.royalties).containsExactlyInAnyOrder(
            Royalty(address = "0xb780208cb751b1ae", fee = "0.02500000".toBigDecimal()),
            Royalty(address = "0xa83a288bd044926e", fee = "0.02500000".toBigDecimal())
        )
    }

    @Test
    fun `parse meta with nested traits - ok`() {
        val json = json("/json/gamisodes_meta_nested.json")
        val meta = GamisodesMetaParser.parse(json, randomItemId())!!

        assertThat(meta.attributes).hasSize(11)
        assertThat(meta.attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute(key = "Costume Type", value = "Generative", type = null, format = null),
            ItemMetaAttribute(key = "Head Piece", value = "Blue Beret", type = null, format = null),
            ItemMetaAttribute(key = "Eyes", value = "Angry", type = null, format = null),
            ItemMetaAttribute(key = "Mouth", value = "Neutral Left", type = null, format = null),
            ItemMetaAttribute(key = "Coms", value = "In", type = null, format = null),
            ItemMetaAttribute(key = "Outfit", value = "Flight Attendant", type = null, format = null),
            ItemMetaAttribute(key = "Background Color", value = "Orange", type = null, format = null),
            ItemMetaAttribute(key = "Background Pattern", value = "Cross Hatch", type = null, format = null),
            ItemMetaAttribute(
                key = "mediaUrl",
                value = "ipfs://bafybeieobsljwbuv3cqlqaolwvoiblosblgcnfkjffyjrr2nwoj5fbwbe4/Brain%20Train%20Ticket%20-%20Series%201",
                type = null,
                format = null
            ),
            ItemMetaAttribute(
                key = "posterUrl",
                value = "ipfs://bafybeieobsljwbuv3cqlqaolwvoiblosblgcnfkjffyjrr2nwoj5fbwbe4/Brain%20Train%20Ticket%20-%20Series%201",
                type = null,
                format = null
            ),
            ItemMetaAttribute(key = "serialNumber", value = "1", type = null, format = null)
        )
    }
}
