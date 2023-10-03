package com.rarible.flow.api.royalty.provider

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowException
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.rarible.flow.Contract
import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import java.lang.IllegalArgumentException

abstract class AbstractMattelRoyaltyProvider(
    private val contract: Contract,
    private val scriptExecutor: ScriptExecutor,
    properties: ApiProperties,
    scriptFile: String = SCRIPT_FILE,
) : ItemRoyaltyProvider {

    private val builder = JsonCadenceBuilder()
    private val script: String = ClassPathResource("script/$scriptFile").inputStream.bufferedReader().use { it.readText() }
    private val chainId = properties.chainId
    private val nftMetadataViews = Contracts.METADATA_VIEWS
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".${contract.contractName}")

    override suspend fun getRoyalties(item: Item): List<Royalty> {
        val preparedScript = prepare(script) ?: return emptyList()
        val owner = item.owner ?: return emptyList()
        val tokenId = item.tokenId

        val result = safeExecute(preparedScript, owner, tokenId) ?: return emptyList()
        val node = jacksonObjectMapper().readTree(result.bytes)

        val nodes = node
            .findValue("fields")
            .findValues("fields")

        return nodes.map {
            val address = it.findValue("address").asText()
            val part = it
                .find { node -> node["name"]?.asText() == "cut" }
                ?.findValue("value")
                ?.findValue("value")
                ?.asDouble()
                ?: throw IllegalArgumentException("Can't get royalty part in json ${String(result.bytes)}")

            Royalty(address, part.toBigDecimal())
        }
    }

    private suspend fun safeExecute(
        script: String,
        owner: FlowAddress,
        tokenId: TokenId
    ): FlowScriptResponse? {
        return try {
            /**
             * If we get an error, rollback is not set
             */
            scriptExecutor.execute(
                script,
                mutableListOf(
                    builder.address(owner.formatted),
                    builder.uint64(tokenId)
                ),
            )
        } catch (e: FlowException) {
            logger.error("Can't execute script to get royalty", e)
            null
        }
    }

    private fun prepare(script: String): String? {
        val contractAddress = contract.deployments[chainId] ?: return null
        val metaViewAddress = nftMetadataViews.deployments[chainId] ?: return null
        return script
            .replace("#CONTRACNAME", contract.contractName)
            .replace("0xCONTRACTADDRESS", contractAddress.formatted)
            .replace(nftMetadataViews.import, metaViewAddress.formatted)
    }

    companion object {
        const val SCRIPT_FILE = "get_nft_metadata_mattel.cdc"
    }
}
