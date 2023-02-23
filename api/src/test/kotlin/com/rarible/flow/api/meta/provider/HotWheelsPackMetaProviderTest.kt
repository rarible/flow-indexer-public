package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.core.test.randomItem
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HotWheelsPackMetaProviderTest {

    private val json = this.javaClass.getResourceAsStream("/json/hot_wheels_pack_meta.json")!!
        .bufferedReader().use { it.readText() }

    private val provider = HotWheelsPackMetaProvider()

    @Test
    fun `get meta - ok`() = runBlocking<Unit> {
        val meta = provider.getMeta(randomItem(meta = json))!!
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

}