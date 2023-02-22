package com.rarible.flow.api.service

import com.rarible.flow.core.domain.ItemId
import org.slf4j.Logger

fun Logger.itemMetaDebug(id: ItemId, message: String) = this.debug(message(id.toString(), message))
fun Logger.itemMetaInfo(id: ItemId, message: String) = this.info(message(id.toString(), message))
fun Logger.itemMetaWarn(id: ItemId, message: String) = this.warn(message(id.toString(), message))
fun Logger.itemMetaError(id: ItemId, message: String) = this.error(message(id.toString(), message))

fun Logger.itemMetaDebug(id: String, message: String) = this.debug(message(id, message))
fun Logger.itemMetaInfo(id: String, message: String) = this.info(message(id, message))
fun Logger.itemMetaWarn(id: String, message: String) = this.warn(message(id, message))
fun Logger.itemMetaError(id: String, message: String) = this.error(message(id, message))

private fun message(id: String, message: String) = "ItemMeta [$id]: $message"