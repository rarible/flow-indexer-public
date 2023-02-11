package com.rarible.flow.api.imageprovider

import com.rarible.flow.Contract
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import org.springframework.util.MimeType

sealed interface ItemImageProvider {
    fun isSupported(itemId: ItemId): Boolean

    suspend fun getImage(itemMeta: ItemMeta): Pair<MimeType, ByteArray>
}

abstract class BaseItemImageProvider: ItemImageProvider {

    abstract val contract: Contract

    override fun isSupported(itemId: ItemId): Boolean = contract.contractName == itemId.contract.substringAfterLast(".")
}

class Base64Detector(
    private val url: String
) {

    private val prefixIndex = url.indexOf(base64prefix)
    private val markerIndex = if (prefixIndex >= 0) url.indexOf(base64marker, prefixIndex) else -1

    val isBase64Image = prefixIndex >= 0 && markerIndex >= 0

    // Don't want to use regex here, not sure how fast it will work on large strings
    companion object {
        private const val base64prefix = "data:image/"
        private const val mimeTypePrefix = "data:"
        private const val base64marker = ";base64,"
    }

    fun getBase64Data(): String {
        val prefixIndex = url.indexOf(base64marker)
        return url.substring(prefixIndex + base64marker.length).trim()
    }

    fun getBase64MimeType(): String {
        return url.substring(url.indexOf(mimeTypePrefix) + mimeTypePrefix.length, markerIndex).trim()
    }

}
