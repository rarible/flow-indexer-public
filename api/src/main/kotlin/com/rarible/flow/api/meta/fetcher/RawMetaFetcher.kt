package com.rarible.flow.api.meta.fetcher

import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.flow.api.config.FeatureFlags
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.service.UrlService
import com.rarible.flow.api.service.itemMetaError
import com.rarible.flow.api.service.itemMetaInfo
import com.rarible.flow.core.domain.ItemId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL

@Component
class RawMetaFetcher(
    private val urlService: UrlService,
    private val externalHttpClient: ExternalHttpClient,
    private var featureFlags: FeatureFlags
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(itemId: ItemId, resource: UrlResource): String? {
        val useProxy = (resource is HttpUrl) && featureFlags.enableProxyForMetaDownload
        return fetch(resource, itemId, useProxy)
    }

    private suspend fun fetch(resource: UrlResource, itemId: ItemId, useProxy: Boolean = false): String? {
        val internalUrl = urlService.resolveInternalHttpUrl(resource)

        if (internalUrl == resource.original) {
            logger.itemMetaInfo(itemId, "Fetching property string by URL $internalUrl")
        } else {
            logger.itemMetaInfo(itemId, "Fetching property string by URL $internalUrl (original: ${resource.original})")
        }

        try {
            URL(internalUrl)
        } catch (e: Throwable) {
            throw MetaException("Corrupted URL: $internalUrl, ${e.message}", MetaException.Status.CORRUPTED_URL)
        }

        // There are several points:
        // 1. Without proxy some sites block our requests (403/429)
        // 2. With proxy some sites block us too, but respond fine without proxy
        // 3. It is better to avoid proxy requests since they are paid
        // So even if useProxy specified we want to try fetch data without it first
        val withoutProxy = safeFetch(internalUrl, itemId, false)

        // Second try with proxy, if needed
        return if (withoutProxy == null && useProxy) {
            safeFetch(internalUrl, itemId, true)
        } else {
            withoutProxy
        }
    }

    private suspend fun safeFetch(url: String, itemId: ItemId, useProxy: Boolean = false): String? {
        return try {
            externalHttpClient.getBody(
                url = url,
                id = itemId.toString(),
                useProxy = useProxy
            )
        } catch (e: Exception) {
            logger.itemMetaError(itemId, "Failed to receive property string via URL (proxy: $useProxy) $url $e")
            null
        }
    }
}
