package com.rarible.flow.api.meta

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.flow.api.util.itemMetaInfo
import com.rarible.flow.api.util.itemMetaWarn
import com.rarible.flow.core.domain.ItemId
import org.slf4j.LoggerFactory
import java.util.Base64

object JsonPropertiesParser {

    private const val BASE_64_JSON_PREFIX = "data:application/json;base64,"
    private const val JSON_PREFIX = "data:application/json;utf8,"

    private val logger = LoggerFactory.getLogger(javaClass)

    private val emptyChars = "\uFEFF".toCharArray()

    private val mapper = ObjectMapper().registerKotlinModule()
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())

    fun parse(itemId: ItemId, data: String): ObjectNode = parse(itemId.toString(), data)

    fun parse(id: String, data: String): ObjectNode {
        val trimmed = trim(data)
        return when {
            trimmed.startsWith(BASE_64_JSON_PREFIX) -> parseBase64(id, trimmed.removePrefix(BASE_64_JSON_PREFIX))
            trimmed.startsWith(JSON_PREFIX) -> parseJson(id, trimmed.removePrefix(JSON_PREFIX))
            isRawJson(trimmed) -> parseJson(id, trimmed)
            else -> throw MetaException(
                "failed to parse properties from json: $data",
                MetaException.Status.CORRUPTED_DATA
            )
        }
    }

    private fun isRawJson(data: String): Boolean {
        return (data.startsWith("{") && data.endsWith("}"))
    }

    private fun parseBase64(itemId: String, data: String): ObjectNode {
        logger.itemMetaInfo(itemId, "parsing properties as Base64")
        val decodedJson = try {
            String(Base64.getMimeDecoder().decode(data.toByteArray()))
        } catch (e: Exception) {
            val errorMessage = "failed to decode Base64: ${e.message}"
            logger.itemMetaWarn(itemId, errorMessage)
            throw MetaException(errorMessage, status = MetaException.Status.CORRUPTED_DATA)
        }
        return parseJson(itemId, decodedJson)
    }

    private fun parseJson(itemId: String, data: String): ObjectNode {
        return try {
            mapper.readTree(data) as ObjectNode
        } catch (e: Exception) {
            val errorMessage = "failed to parse properties from json: ${e.message}"
            logger.itemMetaWarn(itemId, errorMessage)

            throw MetaException(errorMessage, status = MetaException.Status.CORRUPTED_DATA)
        }
    }

    private fun trim(data: String): String {
        return data.trim { it.isWhitespace() || it in emptyChars }
    }

}
