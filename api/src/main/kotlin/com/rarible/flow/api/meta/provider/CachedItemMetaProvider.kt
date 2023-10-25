package com.rarible.flow.api.meta.provider

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.core.config.FeatureFlagsProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.RawOnChainMeta
import com.rarible.flow.core.repository.RawOnChainMetaCacheRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.io.ClassPathResource

abstract class CachedItemMetaProvider(
    private val rawOnChainMetaCacheRepository: RawOnChainMetaCacheRepository,
    private val ff: FeatureFlagsProperties,
    protected val chainId: FlowChainId
) : ItemMetaProvider {

    protected val builder = JsonCadenceBuilder()

    abstract suspend fun parse(raw: String, item: Item): ItemMeta?

    abstract suspend fun fetchRawMeta(item: Item): String?

    protected fun getScript(fileName: String) = ClassPathResource("script/$fileName")
        .inputStream.bufferedReader()
        .use { it.readText() }

    protected suspend fun getRawMeta(item: Item): String? {
        val itemId = item.id
        if (ff.enableRawOnChainMetaCacheRead) {
            rawOnChainMetaCacheRepository.findById(itemId.toString())
                .awaitFirstOrNull()?.let { return it.data }
        }

        val raw = fetchRawMeta(item)

        if (ff.enableRawOnChainMetaCacheWrite) {
            raw?.let {
                val rawMeta = RawOnChainMeta(itemId.toString(), raw)
                rawOnChainMetaCacheRepository.save(rawMeta).awaitFirst()
            }
        }

        return raw
    }

    override suspend fun getMeta(item: Item): ItemMeta? {
        val raw = getRawMeta(item) ?: return null
        return parse(raw, item)
    }
}
