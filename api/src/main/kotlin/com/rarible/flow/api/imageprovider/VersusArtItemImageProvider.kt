package com.rarible.flow.api.imageprovider

import com.rarible.flow.Contract
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.ItemMeta
import org.springframework.stereotype.Component
import org.springframework.util.MimeType
import java.util.*

@Component
class VersusArtItemImageProvider: BaseItemImageProvider() {
    override val contract: Contract
        get() = Contracts.VERSUS_ART

    override suspend fun getImage(itemMeta: ItemMeta): Pair<MimeType, ByteArray> {
        val base64Str = itemMeta.base64 ?: throw IllegalStateException("Base64 content not found!")
        val detector = Base64Detector(base64Str)
        if (!detector.isBase64Image) throw IllegalStateException("Unexpected not base64 image!")
        return Pair(
            MimeType.valueOf(detector.getBase64MimeType()),
            Base64.getDecoder().decode(detector.getBase64Data())
        )
    }
}
