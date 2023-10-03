package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.core.test.randomItemId
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HotWheelsPackMetaParserTest {

    private val jsonV1 = this.javaClass.getResourceAsStream("/json/hot_wheels_pack_meta_1.json")!!
        .bufferedReader().use { it.readText() }

    private val jsonV2 = this.javaClass.getResourceAsStream("/json/hot_wheels_pack_meta_2.json")!!
        .bufferedReader().use { it.readText() }

    @Test
    fun `get meta - ok, pack v1`() = runBlocking<Unit> {
        val meta = HotWheelsPackMetaParser.parse(jsonV1, randomItemId())
        val content = meta.content[0]

        assertThat(meta.name).isEqualTo("Series 4")
        assertThat(meta.description).isEqualTo("")
        assertThat(meta.content).hasSize(1)

        assertThat(content.url).isEqualTo("QmPcs6V5RfLrCzUm8zfU62UGQWNArKE44xYYof8iA5bRm8")
        assertThat(content.representation).isEqualTo(ItemMetaContent.Representation.ORIGINAL)
        assertThat(content.type).isEqualTo(ItemMetaContent.Type.IMAGE)

        assertThat(meta.attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute("totalItemCount", "7"),
            ItemMetaAttribute("packHash", "0024b47534c6f2afaf70e70d4e6fef0fe8745d656298eefe17dcfea2e84efe91"),
            ItemMetaAttribute("thumbnailPath", ""),
            ItemMetaAttribute("seriesNumber", "4"),
            ItemMetaAttribute("seriesName", "Series 4"),
            ItemMetaAttribute("thumbnailCID", "QmPcs6V5RfLrCzUm8zfU62UGQWNArKE44xYYof8iA5bRm8"),
        )
    }

    @Test
    fun `get meta - ok, pack v2`() = runBlocking<Unit> {
        val meta = HotWheelsPackMetaParser.parse(jsonV2, randomItemId())
        val content = meta.content[0]

        assertThat(meta.name).isEqualTo("82 Cadillac Seville Redemption")
        assertThat(meta.description).isEqualTo("")
        assertThat(meta.content).hasSize(1)

        assertThat(content.url).isEqualTo("Qmdw3hCeuPDQfgCKwFJN631L31b2SeYRN2ANz7NTe6PM33")
        assertThat(content.representation).isEqualTo(ItemMetaContent.Representation.ORIGINAL)
        assertThat(content.type).isEqualTo(ItemMetaContent.Type.IMAGE)

        assertThat(meta.attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute("thumbnailCID", "Qmdw3hCeuPDQfgCKwFJN631L31b2SeYRN2ANz7NTe6PM33"),
            ItemMetaAttribute("tokenExpireDate", "3/28/23"),
            ItemMetaAttribute("tokenReleaseDate", "3/14/23"),
            ItemMetaAttribute("originalCardSerial", "QANFT0001931668"),
            ItemMetaAttribute("cardID", "1"),
            ItemMetaAttribute("templateID", "Series_4_Token_1"),
            ItemMetaAttribute("name", "Series_4_Token"),
            ItemMetaAttribute("carName", "82 Cadillac Seville Redemption"),
            ItemMetaAttribute("tokenSerial", "QANFT0002229625"),
        )
    }
}
