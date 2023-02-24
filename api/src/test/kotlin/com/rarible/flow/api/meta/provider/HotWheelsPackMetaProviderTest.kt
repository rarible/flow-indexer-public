package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.fetcher.HotWheelsMetaFetcher
import com.rarible.flow.core.test.randomItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HotWheelsPackMetaProviderTest {

    private val jsonV1 = this.javaClass.getResourceAsStream("/json/hot_wheels_pack_meta_1.json")!!
        .bufferedReader().use { it.readText() }

    private val jsonV2 = this.javaClass.getResourceAsStream("/json/hot_wheels_pack_meta_2.json")!!
        .bufferedReader().use { it.readText() }

    private val fetcher = mockk<HotWheelsMetaFetcher>()

    private val provider = HotWheelsPackMetaProvider(fetcher)

    @Test
    fun `get meta - ok, pack v1`() = runBlocking<Unit> {
        val item = randomItem()
        coEvery { fetcher.getContent(item.id) } returns jsonV1
        val meta = provider.getMeta(item)!!
        val content = meta.content[0]

        assertThat(meta.name).isEqualTo("Series 4")
        assertThat(meta.description).isEqualTo("")
        assertThat(meta.content).hasSize(1)

        assertThat(content.url).isEqualTo("QmPcs6V5RfLrCzUm8zfU62UGQWNArKE44xYYof8iA5bRm8")
        assertThat(content.representation).isEqualTo(ItemMetaContent.Representation.ORIGINAL)
        assertThat(content.type).isEqualTo(ItemMetaContent.Type.IMAGE)

        assertThat(meta.attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute("totalItemCount", "7")
        )
    }

    @Test
    fun `get meta - ok, pack v2`() = runBlocking<Unit> {
        val item = randomItem()
        coEvery { fetcher.getContent(item.id) } returns jsonV2
        val meta = provider.getMeta(item)!!
        val content = meta.content[0]

        assertThat(meta.name).isEqualTo("82 Cadillac Seville Redemption")
        assertThat(meta.description).isEqualTo("")
        assertThat(meta.content).hasSize(1)

        assertThat(content.url).isEqualTo("Qmdw3hCeuPDQfgCKwFJN631L31b2SeYRN2ANz7NTe6PM33")
        assertThat(content.representation).isEqualTo(ItemMetaContent.Representation.ORIGINAL)
        assertThat(content.type).isEqualTo(ItemMetaContent.Type.IMAGE)

        assertThat(meta.attributes).containsExactlyInAnyOrder(
            ItemMetaAttribute("tokenExpireDate", "3/28/23"),
            ItemMetaAttribute("tokenReleaseDate", "3/14/23")
        )
    }

}