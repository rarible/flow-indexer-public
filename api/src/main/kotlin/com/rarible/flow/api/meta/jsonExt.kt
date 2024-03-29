package com.rarible.flow.api.meta

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode

fun JsonNode.getText(vararg paths: List<String>): String? {
    for (path in paths) {
        val current = path.fold(this) { node, subPath -> node.path(subPath) }
        if (current.isTextual || current.isNumber) {
            return current.asText()
        }
    }
    return null
}

fun JsonNode.getText(vararg paths: String): String? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isTextual || current.isNumber) {
            return current.asText()
        }
    }
    return null
}

fun JsonNode.getArray(vararg paths: String): List<JsonNode> {
    for (path in paths) {
        val current = this.path(path)
        if (current.isArray) {
            return (current as ArrayNode).map { it }
        }
    }
    return emptyList()
}

fun JsonNode.getArray(vararg paths: List<String>): List<JsonNode> {
    for (path in paths) {
        val current = path.fold(this) { node, subPath -> node.path(subPath) }
        if (current.isArray) {
            return (current as ArrayNode).map { it }
        }
    }
    return emptyList()
}

fun JsonNode.getNested(vararg paths: List<String>): JsonNode {
    for (path in paths) {
        val current = path.fold(this) { node, subPath -> node.path(subPath) }
        return current
    }
    return NullNode.getInstance()
}

fun <K, V> Map<K, V>.getFirst(vararg paths: K, filter: (value: V) -> Boolean = { true }): V? {
    paths.forEach { path ->
        this[path]?.let { if (filter(it)) return it }
    }
    return null
}
