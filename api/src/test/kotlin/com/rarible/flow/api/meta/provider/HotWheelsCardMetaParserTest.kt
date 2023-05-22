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
        val meta = HotWheelsCardMetaParser.parse(json, randomItemId(), ItemMetaContent.Type.IMAGE)!!
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
            ItemMetaAttribute("miniCollection", "HW Road Trippin'")
        )
    }

}