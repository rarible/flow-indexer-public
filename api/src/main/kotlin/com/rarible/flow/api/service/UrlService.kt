package com.rarible.flow.api.service

import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.flow.api.util.itemMetaWarn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UrlService(
    private val urlParser: UrlParser,
    private val urlResolver: UrlResolver
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInternalHttpUrl(url: String): String? =
        urlParser.parse(url)
            ?.let { resolveInternalHttpUrl(it) }

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String): String? =
        urlParser.parse(url)
            ?.let { resolvePublicHttpUrl(it) }

    fun parseUrl(url: String, id: String): UrlResource? {
        val resource = urlParser.parse(url)
        if (resource == null) {
            logger.itemMetaWarn(id, "UrlService: Cannot parse and resolve url: $url")
        }
        return resource
    }

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInternalHttpUrl(resource: UrlResource): String = urlResolver.resolveInternalUrl(resource)

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(resource: UrlResource): String = urlResolver.resolvePublicUrl(resource)
}
