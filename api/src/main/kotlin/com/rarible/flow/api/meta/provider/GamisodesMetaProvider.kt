package com.rarible.flow.api.meta.provider

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowException
import com.nftco.flow.sdk.FlowScriptResponse
import com.rarible.flow.Contract
import com.rarible.flow.Contracts
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.config.FeatureFlagsProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.RawOnChainMetaCacheRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GamisodesMetaProvider(
    private val scriptExecutor: ScriptExecutor,
    appProperties: AppProperties,
    rawOnChainMetaCacheRepository: RawOnChainMetaCacheRepository,
    ff: FeatureFlagsProperties
) : CachedItemMetaProvider(
    rawOnChainMetaCacheRepository = rawOnChainMetaCacheRepository,
    chainId = appProperties.chainId,
    ff = ff
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val contract: Contract = Contracts.GAMISODES
    private val script: String = getScript("get_gamisodes_meta.cdc")

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".${contract.contractName}")

    suspend fun getGamisodesMeta(item: Item): GamisodesMeta? {
        val raw = fetchRawMeta(item) ?: return null
        return GamisodesMetaParser.parse(raw, item.id)
    }

    override suspend fun parse(raw: String, item: Item): ItemMeta? {
        val gamisodesMeta: GamisodesMeta = try {
            GamisodesMetaParser.parse(raw, item.id)
        } catch (e: Exception) {
            throw MetaException(
                message = "Corrupted Json of metadata for Item ${item.id}: ${e.message}",
                status = MetaException.Status.CORRUPTED_DATA
            )
        }
        return ItemMeta(
            itemId = item.id,
            name = gamisodesMeta.name,
            description = gamisodesMeta.description ?: "",
            attributes = gamisodesMeta.attributes,
            externalUri = gamisodesMeta.externalUri,
            content = listOfNotNull(
                gamisodesMeta.imageOriginal?.let {
                    ItemMetaContent(
                        url = it,
                        representation = ItemMetaContent.Representation.ORIGINAL,
                        type = ItemMetaContent.Type.IMAGE
                    )
                },
                gamisodesMeta.imagePreview?.let {
                    ItemMetaContent(
                        url = it,
                        representation = ItemMetaContent.Representation.PREVIEW,
                        type = ItemMetaContent.Type.IMAGE
                    )
                },
            )
        )
    }

    override suspend fun fetchRawMeta(item: Item): String? {
        val params = when (chainId) {
            FlowChainId.MAINNET -> Params.MAINNET
            FlowChainId.TESTNET -> Params.TESTNET
            else -> throw RuntimeException("Doesn't supported for $chainId")
        }

        val preparedScript = prepare(script, params.registryAddress) ?: return null
        val owner = item.owner ?: return null
        val tokenId = item.tokenId

        val result = params.storages.firstNotNullOfOrNull { path ->
            val meta = safeExecute(
                script = preparedScript,
                owner = owner,
                tokenId = tokenId,
                storagePath = path,
                registryAddress = params.registry,
                brand = params.brand
            )
            if (meta == null) {
                logger.warn("Meta for ${item.tokenId} from $path is empty")
            }
            meta
        } ?: return null

        return String(result.bytes)
    }

    private suspend fun safeExecute(
        script: String,
        owner: FlowAddress,
        tokenId: TokenId,
        storagePath: String,
        registryAddress: String,
        brand: String
    ): FlowScriptResponse? {
        return try {
            scriptExecutor.execute(
                code = script,
                args = mutableListOf(
                    builder.address(owner.formatted),
                    builder.uint64(tokenId),
                    builder.string(storagePath),
                    builder.address(registryAddress),
                    builder.string(brand),
                ),
            )
        } catch (e: FlowException) {
            logger.error("Can't execute script to get meta", e)
            null
        }
    }

    private fun prepare(script: String, registry: String): String? {
        val metaViewAddress = Contracts.METADATA_VIEWS.deployments[chainId] ?: return null
        return script
            .replace(Contracts.METADATA_VIEWS.import, metaViewAddress.formatted)
            .replace("REGISTRY_ADDRESS", registry)
    }

    enum class Params(val storages: List<String>, val registry: String, val registryAddress: String, val brand: String) {
        TESTNET(
            storages = listOf("cl9bqlj3600000ilb44ugzei6_Gamisodes_nft_collection"),
            registry = "0x6085ae87e78e1433",
            registryAddress = "04f74f0252479aed",
            brand = "cl9bqlj3600000ilb44ugzei6_Gamisodes"
        ),
        MAINNET(
            storages = listOf("cl9bquwn300010hkzt0td7pec_Gamisodes_nft_collection", "GamisodesCollection"),
            registry = "0x32d62d5c43ad1038",
            registryAddress = "7ec1f607f0872a9e",
            brand = "cl9bquwn300010hkzt0td7pec_Gamisodes"
        );
    }
}
