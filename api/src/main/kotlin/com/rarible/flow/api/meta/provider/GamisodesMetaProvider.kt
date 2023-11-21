package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    private val scriptAttr: String = getScript("get_gamisodes_meta_attr.cdc")

    private val objectMapper = jacksonObjectMapper()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".${contract.contractName}")

    suspend fun getGamisodesMeta(item: Item): GamisodesMeta? {
        val raw = fetchRawMeta(item) ?: return null
        return objectMapper.readValue(raw)
    }

    override suspend fun parse(raw: String, item: Item): ItemMeta? {
        val gamisodesMeta: GamisodesMeta = try {
            objectMapper.readValue(raw)
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
        val preparedScript = prepare(script) ?: return null
        val preparedScriptAttr = prepareAttr(scriptAttr) ?: return null
        val owner = item.owner ?: return null
        val tokenId = item.tokenId

        val params = when (chainId) {
            FlowChainId.MAINNET -> Params.MAINNET
            FlowChainId.TESTNET -> Params.TESTNET
            else -> throw RuntimeException("Doesn't supported for $chainId")
        }

        val result = params.storages.firstNotNullOfOrNull { path ->
            val meta = safeExecute(
                script = preparedScript,
                owner = owner,
                tokenId = tokenId,
                storagePath = path
            )
            if (meta == null) {
                logger.warn("Meta for ${item.tokenId} from $path is empty")
            }
            meta
        } ?: return null
        val raw = String(result.bytes)
        val meta = GamisodesMetaParser.parse(raw, item.id)
        if (meta.setId == null || meta.templateId == null) {
            logger.warn("Meta attributes setId or templateId for ${item.tokenId} from is empty")
            return null
        }

        val attributes = safeExecuteAttr(
            script = preparedScriptAttr,
            registryAddress = params.registry,
            brand = params.brand,
            setId = meta.setId,
            templateId = meta.templateId
        )
        if (attributes == null) {
            logger.warn("Meta attributes for ${item.tokenId} from is empty")
            return null
        }
        val enriched = enrichWithAttributes(item.id, meta, String(attributes.bytes))
        return objectMapper.writeValueAsString(enriched)
    }

    private fun enrichWithAttributes(itemId: ItemId, meta: GamisodesMeta, traits: String): GamisodesMeta {
        val attrs = GamisodesMetaParser.parseAttributes(traits, itemId)
        return meta.copy(attributes = (meta.attributes + attrs).distinct())
    }

    private suspend fun safeExecute(
        script: String,
        owner: FlowAddress,
        tokenId: TokenId,
        storagePath: String
    ): FlowScriptResponse? {
        return try {
            scriptExecutor.execute(
                code = script,
                args = mutableListOf(
                    builder.address(owner.formatted),
                    builder.uint64(tokenId),
                    builder.string(storagePath)
                ),
            )
        } catch (e: FlowException) {
            logger.error("Can't execute script to get meta", e)
            null
        }
    }

    private suspend fun safeExecuteAttr(
        script: String,
        registryAddress: String,
        brand: String,
        setId: String,
        templateId: String
    ): FlowScriptResponse? {
        return try {
            scriptExecutor.execute(
                code = script,
                args = mutableListOf(
                    builder.address(registryAddress),
                    builder.string(brand),
                    builder.int(setId),
                    builder.int(templateId),
                ),
            )
        } catch (e: FlowException) {
            logger.error("Can't execute script to get meta attributes", e)
            null
        }
    }

    private fun prepare(script: String): String? {
        val metaViewAddress = Contracts.METADATA_VIEWS.deployments[chainId] ?: return null
        return script
            .replace(Contracts.METADATA_VIEWS.import, metaViewAddress.formatted)
    }

    private fun prepareAttr(script: String): String? {
        val registry = when (chainId) {
            FlowChainId.MAINNET -> "7ec1f607f0872a9e"
            FlowChainId.TESTNET -> "04f74f0252479aed"
            else -> throw RuntimeException("Doesn't supported for $chainId")
        }
        return script.replace("REGISTRY_ADDRESS", registry)
    }

    enum class Params(val storages: List<String>, val registry: String, val brand: String) {
        TESTNET(
            storages = listOf("cl9bqlj3600000ilb44ugzei6_Gamisodes_nft_collection"),
            registry = "0x6085ae87e78e1433",
            brand = "cl9bqlj3600000ilb44ugzei6_Gamisodes"
        ),
        MAINNET(
            storages = listOf("cl9bquwn300010hkzt0td7pec_Gamisodes_nft_collection", "GamisodesCollection"),
            registry = "0x32d62d5c43ad1038",
            brand = "cl9bquwn300010hkzt0td7pec_Gamisodes"
        );
    }
}
