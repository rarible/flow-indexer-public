package com.rarible.flow.form

import java.net.URI

/**
 * Represents request form for creating NFT's meta information
 */
data class MetaForm(
    val title: String,
    val description: String,
    val uri: URI
)