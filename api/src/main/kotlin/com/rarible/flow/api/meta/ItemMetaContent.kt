package com.rarible.flow.api.meta

data class ItemMetaContent(
    val url: String,
    val type: Type,
    val representation: Representation = Representation.ORIGINAL,
    val fileName: String? = null,
    val mimeType: String? = null,
    val size: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
) {

    enum class Representation {
        ORIGINAL,
        BIG,
        PREVIEW
    }

    enum class Type {
        IMAGE,
        VIDEO,
        AUDIO,
        MODEL_3D,
        HTML,
        UNKNOWN
    }
}