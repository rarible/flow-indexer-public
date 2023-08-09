package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.core.test.randomItemId
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HotWheelsCardMetaParserTest {

    private val json = this.javaClass.getResourceAsStream("/json/hot_wheels_card_meta.json")!!
        .bufferedReader().use { it.readText() }

    @Test
    fun `get meta - ok`() = runBlocking<Unit> {
        val meta = HotWheelsCardMetaParser.parse(json, randomItemId())
        val content = meta.content[0]

        assertThat(meta.name).isEqualTo("Chevy Silverado Off Road #58")
        assertThat(meta.description).isEqualTo("")
        assertThat(meta.rights).isEqualTo("©Copyright General Motors 2022")
        assertThat(meta.content).hasSize(1)

        assertThat(content.url).isEqualTo("https://sfipfs.infura-ipfs.io/ipfs/123")
        assertThat(content.representation).isEqualTo(ItemMetaContent.Representation.ORIGINAL)
        assertThat(content.type).isEqualTo(ItemMetaContent.Type.IMAGE)

        assertThat(meta.attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute("seriesName", "Series 4"),
            ItemMetaAttribute("releaseYear", "2015"),
            ItemMetaAttribute("rarity", "Super Rare"),
            ItemMetaAttribute("redeemable", "Yes"),
            ItemMetaAttribute("type", "Premium"),
            ItemMetaAttribute("mint", "2780"),
            ItemMetaAttribute("totalSupply", "3000"),
            ItemMetaAttribute("cardId", "58"),
            ItemMetaAttribute("seriesMint", "90780"),
            ItemMetaAttribute("seriesTotalSupply", "295750"),
            ItemMetaAttribute("templateId", "Series_4_58"),
            ItemMetaAttribute("releaseDate", "2022-12-15"),
            ItemMetaAttribute("miniCollection", "HW Road Trippin'"),
            ItemMetaAttribute("licensorLegal", "©Copyright General Motors 2022"),
            ItemMetaAttribute("serialNumber", "QANFT0002022362"),
            ItemMetaAttribute("imageCID", "QmNfc1gZfC46MfSBjypqTiXyfRhNMikS9yoiqzzupGgtrw"),
            ItemMetaAttribute("imageUrl", "https://sfipfs.infura-ipfs.io/ipfs/123"),
            ItemMetaAttribute("packHash", "0022e32218f30e14d5f67465b49151a88268b8f6f12fb4bb48de1c2d6f158063"),
            ItemMetaAttribute("carName", "Chevy Silverado Off Road"),
        )
    }

    @Test
    fun `get meta - ok, broken imageUrl`() = runBlocking<Unit> {
        val jsonWithBrokenImageUrl = json.replace("https://sfipfs.infura-ipfs.io/ipfs/123", "N/A")
        val meta = HotWheelsCardMetaParser.parse(jsonWithBrokenImageUrl, randomItemId())
        val content = meta.content[0]

        assertThat(content.url).isEqualTo("QmNfc1gZfC46MfSBjypqTiXyfRhNMikS9yoiqzzupGgtrw")
    }
}